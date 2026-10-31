import fs from 'node:fs/promises';
import { Presentation, PresentationFile } from '@oai/artifact-tool';

const out = '/Users/jaingler/Desktop/fello/outputs';
await fs.mkdir(out, {recursive:true});
const p = Presentation.create({slideSize:{width:1280,height:720}});
const C={bg:'#F7FAF8',ink:'#14231F',muted:'#6D7B76',line:'#DCE6E1',dark:'#153B35',accent:'#2E8A72',red:'#C8463F',yellow:'#B68120',green:'#2D8A61',white:'#FFFFFF'};
function box(s,x,y,w,h,fill=C.white,line=C.line,r=10){return s.shapes.add({geometry:r? 'roundRect':'rect',position:{left:x,top:y,width:w,height:h},fill,line:{style:'solid',fill:line,width:1},borderRadius:r?'rounded-xl':undefined});}
function txt(s,t,x,y,w,h,size=18,color=C.ink,bold=false){const z=s.shapes.add({geometry:'textbox',position:{left:x,top:y,width:w,height:h},fill:'none',line:{style:'solid',fill:'none',width:0}});z.text=t;z.text.style={fontSize:size,color,bold};return z;}
function base(s,n,title,kicker){s.background.fill=C.bg;txt(s,`FELLO / MONDAY MORNING    ${String(n).padStart(2,'0')}`,72,36,650,22,12,C.accent,true);txt(s,title,72,74,1130,68,36,C.ink,true);txt(s,kicker,72,650,1130,22,12,C.muted,false);}

let s=p.slides.add();s.background.fill=C.dark;txt(s,'FELLO / CUSTOMER SUCCESS',72,62,600,24,14,'#9ED1C0',true);txt(s,'Monday Morning',72,160,1000,90,62,C.white,true);txt(s,'An explainable account-risk queue for CSMs.',76,270,900,42,25,'#C2D9D0',false);txt(s,'Generated data · fitted model · unattended scoring · human-approved outreach',76,592,1050,28,15,'#9ED1C0',false);txt(s,'Staff Engineer take-home',930,62,240,24,14,'#C2D9D0',false);

s=p.slides.add();base(s,2,'The job is earlier, better intervention','The product is a Monday queue, not another dashboard.');
txt(s,'CSMs are drowning in signals across five systems.',72,172,480,42,25,C.ink,true);txt(s,'The business outcome is not “more red accounts.” It is earlier, higher-quality conversations before renewal risk becomes obvious.',72,232,450,100,20,C.muted,false);
box(s,612,160,510,330,C.white,C.line,12);txt(s,'One morning workflow',648,188,340,24,15,C.accent,true);
const steps=[['01','Normalize','messy events'],['02','Score','30-day risk'],['03','Explain','why now'],['04','Act','review task']];steps.forEach((a,i)=>{let y=236+i*58;txt(s,a[0],650,y,35,24,14,C.accent,true);txt(s,a[1],700,y,110,24,17,C.ink,true);txt(s,a[2],820,y,230,24,16,C.muted,false);});
txt(s,'I disagree with auto-email on red.',72,410,500,32,22,C.red,true);txt(s,'A probabilistic score can be wrong, stale, or sensitive. The first automated action is a reviewable recommendation; sending comes after consent and suppression rules.',72,450,520,86,18,C.ink,false);

s=p.slides.add();base(s,3,'A narrow vertical slice proves the decision loop','Generated events become a ranked, evidence-backed queue.');
const cols=[['Five sources','Salesforce\nGmail\nZendesk\nProduct\nStripe'],['Feature contract','usage trend\nsupport severity\npayment failures\nrenewal proximity'],['Fitted model','logistic regression\ntrained on generated data\nrisk probability'],['CSM action','ranked queue\nsource evidence\nreviewable outreach']];cols.forEach((c,i)=>{let x=72+i*285;box(s,x,190,245,270,i===3?'#EAF6F0':C.white,C.line,12);txt(s,c[0],x+22,218,200,28,18,i===3?C.accent:C.ink,true);txt(s,c[1],x+22,270,200,130,17,C.muted,false);if(i<3){txt(s,'→',x+250,294,35,30,25,C.accent,true)}});txt(s,'The score is a policy layer over a model probability: green <50 · yellow 50–79 · red ≥80.',72,535,1000,28,16,C.muted,false);

s=p.slides.add();base(s,4,'Trust is a product feature','Every visible result can be traced to a run, a feature snapshot, and source events.');
box(s,72,172,510,360,C.white,C.line,12);txt(s,'Why is Atlas 100 red?',100,200,420,30,22,C.ink,true);txt(s,'Risk score',100,258,120,22,13,C.muted,true);txt(s,'82 / 100',100,282,190,42,32,C.red,true);[['Usage trend','0.42×'],['Severe tickets','3'],['Payment failures','2'],['Renewal proximity','47 days']].forEach((a,i)=>{let y=350+i*39;txt(s,a[0],100,y,180,22,15,C.muted,false);txt(s,a[1],330,y,150,22,15,C.ink,true)});
box(s,634,172,488,360,'#EAF6F0','#CBE5D8',12);txt(s,'Computed, not guessed',662,200,380,30,22,C.accent,true);txt(s,'risk-logreg-v1',662,260,280,26,17,C.ink,true);txt(s,'scheduled agent run\nfeature values + contributions\nZendesk / Stripe / Product event IDs\ndata coverage confidence\nrecommendation approval state',662,314,360,160,17,C.ink,false);txt(s,'tenant_id is carried through every account, event, feature, and API query.',72,578,1030,25,16,C.muted,false);

s=p.slides.add();base(s,5,'Ship the smallest system we can own','The demo proves the core loop; production hardening is explicit.');
txt(s,'Built now',72,170,240,28,20,C.accent,true);txt(s,'5,000 accounts\n200,000 shuffled events\n3 customer organizations\nJava HTTP service\nbackground scoring every 5 minutes\nmodel + evidence API + CSM UI',72,220,360,250,19,C.ink,false);
txt(s,'Business decisions still needed',500,170,430,28,20,C.accent,true);txt(s,'What is a risk event?\nWhich segments have different renewal motions?\nWho may approve outreach?\nWhat are consent and suppression rules?\nWhich outcome defines success?',500,220,440,250,19,C.ink,false);
txt(s,'At 10×',972,170,220,28,20,C.accent,true);txt(s,'Move events to durable storage, materialize features incrementally, separate training from scoring, and add a durable queue + leader election.',972,220,240,180,17,C.muted,false);txt(s,'Recommendation: evaluate lead time, precision/recall, CSM time saved, and retained ARR before enabling sends.',72,590,1060,28,17,C.ink,true);

for(const [i,slide] of p.slides.items.entries()){const png=await p.export({slide,format:'png',scale:1});await fs.writeFile(`${out}/solution-brief-${i+1}.png`,new Uint8Array(await png.arrayBuffer()));}
const pptx=await PresentationFile.exportPptx(p);await pptx.save(`${out}/fello-solution-brief.pptx`);
