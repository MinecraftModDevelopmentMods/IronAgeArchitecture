// Read-only comparison of placed architecture and saved roof fragments after a clean reload.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),nbt=require('./fixture-nbt.cjs');
function capture(folder){
 const level=nbt.read(path.join(folder,'level.dat'));
 const ids=new Map(level.v.FML.v.Registries.v['minecraft:blocks'].v.ids.v.items.map(x=>[x.V.v,x.K.v]));
 const blocks=[],profiles=[];let windows=0,openWindows=0,legacy=0;
 const nibble=(a,i)=>(a[i>>1]>>((i&1)*4))&15;
 for(const file of fs.readdirSync(path.join(folder,'region')).filter(x=>x.endsWith('.mca')))
  for(const chunk of nbt.region(path.join(folder,'region',file))){
   const l=chunk.v.Level.v;
   for(const s of l.Sections.v.items)for(let i=0;i<4096;i++){
    const name=ids.get(s.Blocks.v[i]+(s.Add?nibble(s.Add.v,i)*256:0));
    if(name?.startsWith('ironagearchitecture:'))blocks.push([l.xPos.v*16+(i&15),s.Y.v*16+(i>>8),l.zPos.v*16+((i>>4)&15),name,nibble(s.Data.v,i)].join('|'));
   }
   for(const cell of chunk.v.ironagearchitecture_roof_cells_v1?.v.items||[]){
    profiles.push([cell.pos.v.toString(),nbt.encode({name:'',t:10,v:cell}).toString('base64')].join('|'));
    if(cell.version?.v!==2)legacy++;
    for(const w of cell.windows?.v.items||[]){windows++;if(w.w?.v[10])openWindows++;}
   }
  }
 const hash=rows=>crypto.createHash('sha256').update(rows.sort().join('\n')).digest('hex');
 return {architectureBlocks:blocks.length,blocksHash:hash(blocks),roofProfiles:profiles.length,profilesHash:hash(profiles),legacyProfiles:legacy,windowFragments:windows,openWindowFragments:openWindows};
}
const [before,after]=process.argv.slice(2);if(!before||!after)throw Error('Usage: node audit-roof-reload.cjs source-world reloaded-world');
const source=capture(before),reloaded=capture(after),passed=JSON.stringify(source)===JSON.stringify(reloaded);
console.log(JSON.stringify({passed,source,reloaded},null,2));if(!passed)process.exitCode=1;
