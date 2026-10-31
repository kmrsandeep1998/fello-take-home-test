import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class Main {
  static final int ACCOUNT_COUNT = 5000, EVENTS_PER_ACCOUNT = 40;
  static final String[] TENANTS = {"northstar", "acme", "lumen"};
  static final String[] SOURCES = {"Salesforce", "Gmail", "Zendesk", "Product", "Stripe"};
  static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;
  static final Random RNG = new Random(42);
  static final Map<String, Account> accounts = new LinkedHashMap<>();
  static final Map<String, List<Event>> eventsByAccount = new HashMap<>();
  static final List<Event> events = new ArrayList<>();
  static Model model;
  static Instant lastAgentRun;

  static class Account {
    String id, tenant, name, segment; double arr, usageTrend, activeDays, severeTickets, unresolvedTickets, paymentFailures, sentiment, renewalDays, missingRate, risk, confidence; String health, action;
    List<Event> evidence = new ArrayList<>();
  }
  static class Event {
    String id, accountId, tenant, source, type, at; double value, severity, sentiment; boolean missing;
    Event(String id,String accountId,String tenant,String source,String type,String at,double value,double severity,double sentiment,boolean missing){this.id=id;this.accountId=accountId;this.tenant=tenant;this.source=source;this.type=type;this.at=at;this.value=value;this.severity=severity;this.sentiment=sentiment;this.missing=missing;}
  }
  static class Model { double[] w; double bias; String version="risk-logreg-v1"; }

  public static void main(String[] args) throws Exception {
    generate(); train(); runAgent();
    if (args.length > 0 && args[0].equals("--smoke")) {
      long red=accounts.values().stream().filter(a->a.health.equals("RED")).count();
      long yellow=accounts.values().stream().filter(a->a.health.equals("YELLOW")).count(), green=accounts.values().stream().filter(a->a.health.equals("GREEN")).count();
      double min=accounts.values().stream().mapToDouble(a->a.risk).min().orElse(0), max=accounts.values().stream().mapToDouble(a->a.risk).max().orElse(0), avg=accounts.values().stream().mapToDouble(a->a.risk).average().orElse(0);
      System.out.println("SMOKE_OK accounts="+accounts.size()+" events="+events.size()+" tenants="+TENANTS.length+" red="+red+" yellow="+yellow+" green="+green+" risk="+round(min)+".."+round(max)+" avg="+round(avg)+" model="+model.version);
      return;
    }
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    String host = System.getenv().getOrDefault("HOST", "127.0.0.1");
    HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
    server.createContext("/api/summary", Main::summary);
    server.createContext("/api/accounts", Main::accountsApi);
    server.createContext("/api/agent", Main::agentApi);
    server.createContext("/api/health", Main::healthApi);
    server.createContext("/", Main::staticFile);
    server.setExecutor(Executors.newCachedThreadPool()); server.start();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(() -> { synchronized(accounts){ runAgent(); } }, 5, 300, TimeUnit.SECONDS);
    System.out.println("Fello Monday Morning running at http://"+host+":"+port);
  }

  static void generate() {
    accounts.clear(); events.clear(); eventsByAccount.clear(); RNG.setSeed(42);
    Instant now = Instant.parse("2026-10-28T00:00:00Z");
    for (int i=0;i<ACCOUNT_COUNT;i++) {
      Account a=new Account(); a.id="acct-%04d".formatted(i+1); a.tenant=TENANTS[i%TENANTS.length]; a.name=new String[]{"Atlas","Beacon","Cedar","Delta","Evergreen","Fjord","Garnet","Harbor"}[i%8]+" "+(100+i); a.segment=i%5==0?"Enterprise":i%3==0?"Mid-market":"SMB"; a.arr=25000+(i%37)*4200+(i%7)*10000;
      double latent=RNG.nextDouble();
      for(int j=0;j<EVENTS_PER_ACCOUNT;j++) {
        String source=SOURCES[(j+i)%SOURCES.length]; String type; double value=0, sev=0, sent=0; boolean missing=RNG.nextDouble()<0.07;
        int daysAgo=(j*9+i*3)%1095; Instant at=now.minus(Duration.ofDays(daysAgo)).minus(Duration.ofHours((j*7)%24));
        if(source.equals("Product")){type="usage"; value=Math.max(0, (1-latent)*18 + RNG.nextGaussian()*5 + (j<8? -latent*8:0));}
        else if(source.equals("Zendesk")){type=RNG.nextDouble()<0.28?"ticket_high":"ticket"; sev=type.endsWith("high")?3:1+RNG.nextInt(2); value=sev;}
        else if(source.equals("Stripe")){type=RNG.nextDouble()<(latent*.35)?"payment_failed":"invoice_paid"; value=type.equals("payment_failed")?1:0;}
        else if(source.equals("Gmail")){type=RNG.nextDouble()<.18?"negative_reply":"customer_touch"; sent=type.startsWith("negative")?-(1+RNG.nextDouble()*2):.5+RNG.nextDouble(); value=sent;}
        else {type=RNG.nextDouble()<.15?"renewal_at_risk":"account_update"; value=type.startsWith("renewal")?1:0;}
        events.add(new Event("evt-%06d".formatted(events.size()+1),a.id,a.tenant,source,type,ISO.format(at),value,sev,sent,missing));
      }
      accounts.put(a.id,a); eventsByAccount.put(a.id, new ArrayList<>());
    }
    for (Event e : events) eventsByAccount.get(e.accountId).add(e);
    Collections.shuffle(events, RNG); // deliberately out of order at ingestion boundary
  }

  static double[] features(Account a) {
    List<Event> es=eventsByAccount.getOrDefault(a.id, List.of());
    double usage=0, usageRecent=0, usageOld=0, active=0, high=0, unresolved=0, fails=0, sentiment=0, sentCount=0; int missing=0; double renewal=30+(Integer.parseInt(a.id.substring(5))%330);
    for(Event e:es){ if(e.missing) {missing++; continue;} if(e.source.equals("Product")){usage+=e.value; if(e.at.compareTo("2026-06")>0)usageRecent+=e.value; else usageOld+=e.value; if(e.value>3)active++;} if(e.source.equals("Zendesk")){if(e.severity>=3)high++; unresolved+=e.severity>=2?1:0;} if(e.source.equals("Stripe")&&e.type.equals("payment_failed"))fails++; if(e.source.equals("Gmail")){sentiment+=e.sentiment;sentCount++;} }
    double trend=(usageRecent+1)/(usageOld+1); if(sentCount>0) sentiment/=sentCount; else sentiment=0;
    a.usageTrend=trend; a.activeDays=active; a.severeTickets=high; a.unresolvedTickets=unresolved; a.paymentFailures=fails; a.sentiment=sentiment; a.renewalDays=renewal; a.missingRate=missing/(double)es.size();
    return new double[]{Math.log(trend), active/12.0, high/4.0, unresolved/8.0, fails/3.0, -sentiment/2.0, (180-renewal)/180.0, missing/(double)es.size()};
  }

  static void train() { model=new Model(); model.w=new double[8]; List<Account> as=new ArrayList<>(accounts.values()); double[][] X=new double[as.size()][]; double[] y=new double[as.size()];
    for(int i=0;i<as.size();i++){Account a=as.get(i); X[i]=features(a); double latent=(a.severeTickets*.35+a.paymentFailures*.6+(1-a.usageTrend)*1.8+(a.renewalDays<90?.4:0)+(-a.sentiment)*.3+a.missingRate*.2); y[i]=latent>2.35?1:0;}
    for(int epoch=0;epoch<500;epoch++){double[] g=new double[9]; for(int i=0;i<X.length;i++){double z=model.bias;for(int k=0;k<8;k++)z+=model.w[k]*X[i][k];double p=sig(z),d=p-y[i];g[0]+=d;for(int k=0;k<8;k++)g[k+1]+=d*X[i][k];}for(int k=0;k<8;k++)model.w[k]-=.06*g[k+1]/X.length;model.bias-=.06*g[0]/X.length;}
  }
  static double sig(double z){return 1/(1+Math.exp(-Math.max(-30,Math.min(30,z))));}
  static void runAgent(){ synchronized(accounts){for(Account a:accounts.values()){double[] fv=features(a);double z=model.bias;for(int i=0;i<fv.length;i++)z+=model.w[i]*fv[i];a.risk=sig(z)*100;a.health=a.risk>=80?"RED":a.risk>=50?"YELLOW":"GREEN";a.confidence=Math.max(0,1-a.missingRate*1.7);a.action=a.health.equals("RED")?"Review renewal risk and contact champion":"Monitor on next weekly review";a.evidence.clear();for(Event e:eventsByAccount.getOrDefault(a.id,List.of()))if(!e.missing&&(e.source.equals("Zendesk")||e.source.equals("Stripe")||e.source.equals("Product")))a.evidence.add(e);a.evidence.sort((e1,e2)->e2.at.compareTo(e1.at));if(a.evidence.size()>8)a.evidence=new ArrayList<>(a.evidence.subList(0,8));}lastAgentRun=Instant.now();}}

  static void summary(HttpExchange ex)throws IOException{String t=query(ex,"tenant","northstar");int r=0,y=0,g=0,tenantEvents=0;double avg=0;for(Account a:accounts.values())if(a.tenant.equals(t)){avg+=a.risk;if(a.health.equals("RED"))r++;else if(a.health.equals("YELLOW"))y++;else g++;tenantEvents+=eventsByAccount.getOrDefault(a.id,List.of()).size();}int n=r+y+g;send(ex,"{\"tenant\":"+q(t)+",\"accounts\":"+n+",\"red\":"+r+",\"yellow\":"+y+",\"green\":"+g+",\"avgRisk\":"+round(n==0?0:avg/n)+",\"lastAgentRun\":"+q(lastAgentRun.toString())+",\"events\":"+tenantEvents+",\"model\":"+q(model.version)+"}");}
  static void accountsApi(HttpExchange ex)throws IOException{String[] p=ex.getRequestURI().getPath().split("/");String t=query(ex,"tenant","northstar");if(p.length>=4&&p[3].length()>0){Account a=accounts.get(p[3]);if(a==null||!a.tenant.equals(t)){send(ex,"{\"error\":\"not found\"}",404);return;}send(ex,accountJson(a));return;}String health=query(ex,"health","");List<Account> list=new ArrayList<>();for(Account a:accounts.values())if(a.tenant.equals(t)&&(health.isEmpty()||a.health.equals(health)))list.add(a);list.sort((x,y)->Double.compare(y.risk,x.risk));StringBuilder b=new StringBuilder("[");for(int i=0;i<Math.min(80,list.size());i++){if(i>0)b.append(',');b.append(accountJson(list.get(i),false));}send(ex,b.append(']').toString());}
  static void agentApi(HttpExchange ex)throws IOException{if(!ex.getRequestMethod().equals("POST")){ex.getResponseHeaders().set("Allow","POST");send(ex,"{\"error\":\"agent runs require POST\"}",405);return;}runAgent();send(ex,"{\"ok\":true,\"lastAgentRun\":"+q(lastAgentRun.toString())+"}");}
  static void healthApi(HttpExchange ex)throws IOException{send(ex,"{\"ok\":true,\"accounts\":"+accounts.size()+",\"events\":"+events.size()+",\"modelFitted\":true}");}
  static String accountJson(Account a){return accountJson(a,true);}
  static String accountJson(Account a,boolean full){StringBuilder b=new StringBuilder("{\"id\":").append(q(a.id)).append(",\"name\":").append(q(a.name)).append(",\"tenant\":").append(q(a.tenant)).append(",\"segment\":").append(q(a.segment)).append(",\"arr\":").append(a.arr).append(",\"risk\":").append(round(a.risk)).append(",\"health\":").append(q(a.health)).append(",\"confidence\":").append(round(a.confidence*100)).append(",\"action\":").append(q(a.action));if(full){double[] fv=features(a);b.append(",\"modelVersion\":").append(q(model.version)).append(",\"computedAt\":").append(q(lastAgentRun.toString())).append(",\"features\":{\"usageTrend\":").append(round(a.usageTrend)).append(",\"activeDays\":").append(round(a.activeDays)).append(",\"severeTickets\":").append(round(a.severeTickets)).append(",\"unresolvedTickets\":").append(round(a.unresolvedTickets)).append(",\"paymentFailures\":").append(round(a.paymentFailures)).append(",\"sentiment\":").append(round(a.sentiment)).append(",\"renewalDays\":").append(round(a.renewalDays)).append(",\"missingRate\":").append(round(a.missingRate*100)).append("},\"contributions\":{\"usageTrend\":").append(round(fv[0]*model.w[0])).append(",\"activeDays\":").append(round(fv[1]*model.w[1])).append(",\"support\":").append(round((fv[2]*model.w[2])+(fv[3]*model.w[3]))).append(",\"billing\":").append(round(fv[4]*model.w[4])).append(",\"sentiment\":").append(round(fv[5]*model.w[5])).append(",\"renewal\":").append(round(fv[6]*model.w[6])).append("},\"evidence\":[");for(int i=0;i<a.evidence.size();i++){if(i>0)b.append(',');Event e=a.evidence.get(i);b.append("{\"id\":").append(q(e.id)).append(",\"source\":").append(q(e.source)).append(",\"type\":").append(q(e.type)).append(",\"at\":").append(q(e.at)).append(",\"value\":").append(round(e.value)).append("}");}b.append("]");}return b.append('}').toString();}
  static String q(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";} static String round(double d){return String.format(Locale.US,"%.2f",d);} 
  static String query(HttpExchange e,String k,String d){String q=e.getRequestURI().getRawQuery();if(q==null)return d;for(String x:q.split("&")){String[] p=x.split("=",2);if(p.length==2&&p[0].equals(k))try{return URLDecoder.decode(p[1],StandardCharsets.UTF_8);}catch(Exception ignored){}}return d;}
  static void send(HttpExchange e,String s)throws IOException{send(e,s,200);}static void send(HttpExchange e,String s,int code)throws IOException{byte[] b=s.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(code,b.length);try(OutputStream o=e.getResponseBody()){o.write(b);}}
  static void staticFile(HttpExchange e)throws IOException{String path=e.getRequestURI().getPath();if(path.equals("/"))path="/index.html";Path p=Paths.get("public"+path).normalize();if(!p.startsWith(Paths.get("public"))||!Files.exists(p)){send(e,"not found",404);return;}String ct=path.endsWith(".css")?"text/css":path.endsWith(".js")?"text/javascript":"text/html";byte[] b=Files.readAllBytes(p);e.getResponseHeaders().set("Content-Type",ct);e.sendResponseHeaders(200,b.length);try(OutputStream o=e.getResponseBody()){o.write(b);}}
}
