import fs from 'node:fs/promises';
import { Presentation, PresentationFile } from '@oai/artifact-tool';

const out='/Users/jaingler/Desktop/fello/outputs';
await fs.mkdir(out,{recursive:true});
const p=Presentation.create({slideSize:{width:1280,height:720}});
const C={bg:'#F7FAF8',ink:'#14231F',muted:'#60716B',line:'#D5E2DC',dark:'#153B35',accent:'#2E8A72',red:'#C8463F',warm:'#B68120',white:'#FFFFFF'};
function shape(s,x,y,w,h,fill=C.white,line=C.line,r=10){return s.shapes.add({geometry:r?'roundRect':'rect',position:{left:x,top:y,width:w,height:h},fill,line:{style:'solid',fill:line,width:1},borderRadius:r?'rounded-xl':undefined});}
function text(s,t,x,y,w,h,size=16,color=C.ink,bold=false){const z=s.shapes.add({geometry:'textbox',position:{left:x,top:y,width:w,height:h},fill:'none',line:{style:'solid',fill:'none',width:0}});z.text=t;z.text.style={fontSize:size,color,bold};return z;}
const s=p.slides.add();s.background.fill=C.bg;
text(s,'FELLO / SOLUTION BRIEF',72,32,400,24,13,C.accent,true);
text(s,'Make Monday a ranked conversation list',72,67,1120,62,50,C.ink,true);
text(s,'A narrow first release for explainable, multi-tenant account risk.',74,135,900,30,22,C.muted,false);

shape(s,72,190,350,310,C.dark,C.dark,12);text(s,'THE ACTUAL PROBLEM',98,218,280,24,15,'#9ED1C0',true);text(s,'CSMs need a trustworthy\nway to decide who to call.',98,266,290,78,25,C.white,true);text(s,'Success means earlier, higher-quality intervention before renewal risk becomes obvious.',98,388,280,68,16,'#C2D9D0',false);

shape(s,448,190,360,310,C.white,C.line,12);text(s,'BUILD FIRST',474,218,260,24,15,C.accent,true);text(s,'An explainable risk queue',474,258,300,58,25,C.ink,true);text(s,'• normalize five source systems\n• generate account-level features\n• fit logistic regression on the data\n• rank by risk and renewal proximity\n• show evidence, confidence, and action\n• schedule scoring without a human click',474,326,300,140,16,C.ink,false);

shape(s,834,190,374,310,'#EAF6F0','#CBE5D8',12);text(s,'IMPORTANT BOUNDARY',860,218,290,24,15,C.red,true);text(s,'Do not auto-email on red',860,258,300,30,25,C.ink,true);text(s,'A score is probabilistic and data may be stale or incomplete. The first automated output is a reviewable outreach task.',860,308,300,72,17,C.ink,false);text(s,'Business input still needed',860,402,280,22,16,C.accent,true);text(s,'risk-event label · consent rules · owners · success metric',860,436,300,36,16,C.muted,false);

shape(s,72,530,1136,108,C.white,C.line,10);text(s,'NON-NEGOTIABLES PROVEN IN THE SLICE',98,555,370,20,14,C.accent,true);text(s,'5,000 accounts · 200,000 shuffled/incomplete events · 3 tenants · fitted model · scheduled agent · source-event traceability',98,588,1050,26,17,C.ink,true);text(s,'Production next: durable event storage, incremental features, model registry, queue/leader election, and real integrations.',98,616,1050,22,14,C.muted,false);

const png=await p.export({slide:s,format:'png',scale:1});await fs.writeFile(`${out}/one-page-solution-brief.png`,new Uint8Array(await png.arrayBuffer()));
const pptx=await PresentationFile.exportPptx(p);await pptx.save(`${out}/one-page-solution-brief.pptx`);
