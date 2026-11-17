// Read-only post-run check: preserve all original architecture blocks/profiles and count new material.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),nbt=require('./fixture-nbt.cjs');
const root=process.argv[2],cfg=JSON.parse(fs.readFileSync(path.join(root,'case.json'),'utf8').replace(/^\uFEFF/,'')),source=cfg.SourceWorld,world=process.argv[3]||path.join(root,'server/world');
function chunks(folder){const map=new Map();for(let file of fs.readdirSync(path.join(folder,'region')).filter(x=>x.endsWith('.mca')))for(let r of nbt.region(path.join(folder,'region',file))){let l=r.v.Level.v;map.set(l.xPos.v+','+l.zPos.v,l);}return map;}
const base=chunks(source),after=chunks(world),level=nbt.read(path.join(source,'level.dat')),registry=new Map(level.v.FML.v.Registries.v['minecraft:blocks'].v.ids.v.items.map(x=>[x.V.v,x.K.v]));
const nibble=(v,i)=>(v[i>>1]>>((i&1)*4))&15;
function state(l,s,i){return [s.Blocks.v[i]+(s.Add?nibble(s.Add.v,i)*256:0),nibble(s.Data.v,i)];}
function at(x,y,z){let l=after.get((x>>4)+','+(z>>4));if(!l)return [0,0];let s=l.Sections.v.items.find(s=>s.Y.v===(y>>4));return s?state(l,s,((y&15)<<8)|((z&15)<<4)|(x&15)):[0,0];}
let checked=0,blockChanges=0,legacyHelpers=0,removedLegacyHelpers=0,reusedUndersides=0;for(let l of base.values())for(let s of l.Sections.v.items)for(let i=0;i<4096;i++){let [id,meta]=state(l,s,i);if(!(registry.get(id)||'').startsWith('ironagearchitecture:'))continue;let x=l.xPos.v*16+(i&15),z=l.zPos.v*16+((i>>4)&15),y=s.Y.v*16+(i>>8),actual=at(x,y,z);if(registry.get(id)==='ironagearchitecture:smart_thatch_part'){legacyHelpers++;if(actual[0]===0)removedLegacyHelpers++;else if(actual[0]===id&&actual[1]===8)reusedUndersides++;else if(actual[0]!==id)blockChanges++;continue;}checked++;if(actual[0]!==id||actual[1]!==meta)blockChanges++;}
const expected=new Map(),districts=[[11,11],[2,2],[20,2],[2,19],[20,19],[11,2],[11,19],[2,11],[20,11],[6,6]];
for(let player=0;player<cfg.Players;player++){
 let px=cfg.Layout==='spread'?32+districts[player][0]*16:176+(player%5)*8,pz=cfg.Layout==='spread'?32+districts[player][1]*16:208+Math.floor(player/5)*8;
 for(let y=1;y<=4;y++)for(let z=1;z<=5;z++)for(let x=1;x<=5;x++){
  if(y<4&&x!==1&&x!==5&&z!==1&&z!==5)continue;if(y<3&&x===3&&z===1)continue;
  let name=y===4?'smart_thatch':player%3===2?'smart_daub_full_oak':'smart_cob',meta=y<4&&player%3===1?1:0;
  expected.set([px+x,100+y,pz+z].join(','),['ironagearchitecture:'+name,meta]);
 }
}
// Full timber walls can inherit any of the four horizontal facings on placement.
// Their finish and full-cell footprint must remain correct; orientation is not material.
function matches(wanted,name,meta){return wanted&&wanted[0]===name&&(name==='ironagearchitecture:smart_daub_full_oak'?(meta&12)===(wanted[1]<<2):name==='ironagearchitecture:smart_thatch'?meta<4:meta===wanted[1]);}
const added={},unexpected=[];for(let l of after.values())for(let s of l.Sections.v.items)if(s.Y.v>=6)for(let i=0;i<4096;i++){
 let [id,meta]=state(l,s,i),name=registry.get(id)||'';if(!name.startsWith('ironagearchitecture:'))continue;added[name]=(added[name]||0)+1;
 if(name==='ironagearchitecture:smart_thatch_part')continue;
 let key=[l.xPos.v*16+(i&15),s.Y.v*16+(i>>8),l.zPos.v*16+((i>>4)&15)].join(','),wanted=expected.get(key);
 if(!matches(wanted,name,meta))unexpected.push({position:key,name,meta});
}
const missing=[];for(let [key,wanted]of expected){let [id,meta]=at(...key.split(',').map(Number));if(!matches(wanted,registry.get(id),meta))missing.push(key);}
const shapes=require('./shape-save.cjs');
function roof(folder){const r=shapes.roof(folder),templates=r.templates.map(t=>crypto.createHash('sha256').update(t.z).digest('hex')),cells=new Map(),owners=new Set();for(const g of r.groups){for(const o of g.owners)owners.add(o.p.toString());for(const c of g.cells){const k=c.p.toString(),h=templates[c.t];if(cells.has(k)&&cells.get(k)!==h)throw Error('Unexpected overlapping saved groups in this fixture');cells.set(k,h);}}return {cells,owners};}
function cob(folder){return new Map([...shapes.cob(folder)].map(([k,v])=>[k,JSON.stringify(v)]));}
let oldRoof=roof(source),newRoof=roof(world),oldCob=cob(source),newCob=cob(world),changedRoof=0,changedCob=0;for(let [p,v] of oldRoof.cells)if(newRoof.cells.get(p)!==v)changedRoof++;for(let [p,v]of oldCob)if(newCob.get(p)!==v)changedCob++;
const n=cfg.Players,timber=Math.floor(n/3),cobPlayers=n-timber,newOwners=added['ironagearchitecture:smart_thatch']||0;
const countsOk=(added['ironagearchitecture:smart_cob']||0)===46*cobPlayers&&(added['ironagearchitecture:smart_daub_full_oak']||0)===46*timber&&(added['ironagearchitecture:smart_thatch']||0)===25*n&&newOwners===25*n;
const result={passed:blockChanges===0&&changedRoof===0&&changedCob===0&&countsOk&&unexpected.length===0&&missing.length===0,players:n,layout:cfg.Layout,existingArchitectureBlocksChecked:checked,legacyHelperBlocks:legacyHelpers,removedLegacyHelpers,reusedUndersides,changedExistingBlocks:blockChanges,existingRoofCellsChecked:oldRoof.cells.size,changedExistingRoofProfiles:changedRoof,existingCobProfilesChecked:oldCob.size,changedExistingCobProfiles:changedCob,additionalMaterials:added,newRoofOwners:newOwners,expectedOperations:91*n,unexpectedPlacedBlocks:unexpected,missingExpectedBlocks:missing};console.log(JSON.stringify(result,null,2));if(!result.passed)process.exitCode=1;
