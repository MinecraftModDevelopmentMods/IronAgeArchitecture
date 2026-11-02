// Run explicitly after editing templates. --check compares without writing.
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '..');
const ns = 'ironagearchitecture';
const assets = `src/main/resources/assets/${ns}`;
const check = process.argv.includes('--check');
const expected = new Map();
const json = (file, value) => expected.set(`${assets}/${file}.json`, JSON.stringify(value, null, 2) + '\n');
const model = (name, value) => json(`models/block/${name}`, value);
const item = (name, parent) => json(`models/item/${name}`, {parent:`${ns}:block/${parent}`});
const state = (name, variants) => json(`blockstates/${name}`, {variants});
const finishes = ['cob','limewashed_cob'];
const woods = ['oak','spruce','birch','jungle','acacia','dark_oak'];
const names = ['Oak','Spruce','Birch','Jungle','Acacia','Dark Oak'];
const facings = {north:0,east:90,south:180,west:270};
const shapes = ['straight','inner_left','inner_right','outer_left','outer_right'];
const lang = {'itemGroup.ironagearchitecture':'Iron Age Architecture'};
for (const finish of finishes) {
  const name = finish === 'cob' ? 'Cob' : 'Limewashed Cob';
  model(finish,{parent:'minecraft:block/cube_all',textures:{all:`${ns}:blocks/${finish}`}});
  state(finish,{normal:{model:`${ns}:${finish}`}}); item(finish,finish);
  lang[`tile.${ns}.${finish}.name`] = name;
  for (const [part,parent] of [['slab','half_slab'],['upper_slab','upper_slab'],['stairs','stairs'],['inner_stairs','inner_stairs'],['outer_stairs','outer_stairs']])
    model(`${finish}_${part}`,{parent:`minecraft:block/${parent}`,textures:{bottom:`${ns}:blocks/${finish}`,top:`${ns}:blocks/${finish}`,side:`${ns}:blocks/${finish}`}});
  for (const doubled of [false,true]) {
    const variants = {};
    for (const half of ['bottom','top']) for (const variant of ['false','true'])
      variants[`half=${half},variant=${variant}`] = {model:`${ns}:${doubled ? finish : finish + (half === 'top' ? '_upper_slab' : '_slab')}`};
    state(`${finish}_${doubled ? 'double_slab' : 'slab'}`,variants);
  }
  item(`${finish}_slab`,`${finish}_slab`); lang[`tile.${ns}.${finish}_slab.name`] = name+' Slab';
  const stairs = {};
  for (const [facing,y] of Object.entries({east:0,south:90,west:180,north:270})) for (const half of ['bottom','top']) for (const shape of shapes) {
    let rotation = y + (shape.endsWith('left') ? 270 : 0);
    if (half === 'top' && shape !== 'straight') rotation += 90;
    stairs[`facing=${facing},half=${half},shape=${shape}`] = {model:`${ns}:${finish}_${shape.startsWith('inner') ? 'inner_stairs' : shape.startsWith('outer') ? 'outer_stairs' : 'stairs'}`,x:half==='top'?180:0,y:rotation%360,uvlock:true};
  }
  state(`${finish}_stairs`,stairs); item(`${finish}_stairs`,`${finish}_stairs`);
  lang[`tile.${ns}.${finish}_stairs.name`] = name+' Stairs';
}
model('timber_wall_mesh',{parent:'minecraft:block/block',textures:{particle:`${ns}:blocks/limewashed_cob`},elements:[]});
const families=[['timber_frame','Post','Cross Brace'],['timber_rail','Sill Rail','Head Rail'],
  ['timber_stud','Stud','Close Studs'],['timber_brace','Rising Brace','Falling Brace'],['timber_terminal','Corner Post','End Post']];
for (let wood=0;wood<6;wood++) for(const [prefix,first,second] of families) {
  const name=`${prefix}_${woods[wood]}`,variants={};
  for(let panel=0;panel<4;panel++) {
    // Geometry is baked from the shared wall parts and render-only connection mask.
    item(`${name}_${panel}`,'timber_wall_mesh');
    lang[`tile.${ns}.${name}.${panel}.name`]=`${names[wood]} ${panel%2?second:first} with ${panel<2?'Daub':'Limewashed Daub'}`;
    for(const facing of Object.keys(facings)) variants[`facing=${facing},panel=${panel}`]={model:`${ns}:timber_wall_mesh`};
  }
  state(name,variants);
}
const infill={};
for(let panel=0;panel<4;panel++) for(const facing of Object.keys(facings)) infill[`facing=${facing},panel=${panel}`]={model:`${ns}:timber_wall_mesh`};
state('wattle_and_daub',infill);
for(let panel=0;panel<2;panel++) {
  item(`wattle_and_daub_${panel}`,'timber_wall_mesh');
  lang[`tile.${ns}.wattle_and_daub.${panel}.name`]=panel?'Limewashed Wattle and Daub':'Wattle and Daub';
}
const profiles=['thin','short_left','short_right','corner_l','corner_half_left','corner_half_right','corner_quarter'];
for(const profile of profiles) for(const wood of [null,...woods]) for(const [prefix,first,second] of (wood?families:[['wattle_and_daub','Infill','Infill']])) {
  const corner=profile.startsWith('corner_');
  if(corner&&(!wood||prefix!=='timber_terminal')) continue;
  const name=`partial_${profile}_${prefix}${wood?'_'+wood:''}`,variants={};
  for(let panel=0;panel<4;panel++) for(const facing of Object.keys(facings)) variants[`facing=${facing},panel=${panel}`]={model:`${ns}:timber_wall_mesh`};
  state(name,variants);
  if(profile==='short_right'||profile==='corner_half_right') continue;
  const count=!wood||prefix==='timber_terminal'&&!corner?2:4;
  for(let panel=0;panel<count;panel++) {
    item(`${name}_${panel}`,'timber_wall_mesh');
    const finish=count===2?panel:panel>>1;
    const pattern=corner?`${panel%2?'Inside':'Outside'} ${profile==='corner_l'?'L':profile==='corner_quarter'?'Quarter':'Half'} Corner Post`:
      prefix==='timber_terminal'?'End Post':panel%2?second:first;
    lang[`tile.${ns}.${name}.${panel}.name`]=`${wood?names[woods.indexOf(wood)]+' ':''}${profile==='thin'?'Thin':corner?'':'Short'} ${pattern} with ${finish?'Limewashed Daub':'Daub'}`.replace(/\s+/g,' ').trim();
  }
}
for(const wood of woods) {
  for(const shape of ['full','half','quarter','l']) for(const bracket of [null,...Object.keys(facings)]) {
    const name=`jetty_floor_${shape}_${wood}${bracket?'_bracket_'+bracket:''}`,variants={};
    for(let panel=0;panel<4;panel++) for(const facing of Object.keys(facings)) variants[`facing=${facing},panel=${panel}`]={model:`${ns}:timber_wall_mesh`};
    state(name,variants);
    if(!bracket) {
      item(name+'_0','timber_wall_mesh');
      lang[`tile.${ns}.${name}.0.name`]=`${names[woods.indexOf(wood)]} ${shape==='l'?'L-shaped':shape[0].toUpperCase()+shape.slice(1)} Jetty Floor`;
    }
  }
  for(const reach of ['half','full']) {
    const name=`jetty_bracket_${reach}_${wood}`;
    const faces=Object.fromEntries(['down','up','north','south','west','east'].map(face=>[face,{texture:'#wood'}]));
    model(name,{parent:'minecraft:block/block',textures:{wood:`${ns}:blocks/beam_${wood}`,particle:`${ns}:blocks/beam_${wood}`},elements:[
      {from:[2,12,6],to:[14,15,9],faces}, {from:[11,2,6],to:[14,12,9],faces},
      {from:[3,7,6],to:[13,9,9],rotation:{origin:[8,8,8],axis:'z',angle:45},faces}]});
    item(name,name);lang[`item.${ns}.${name}.name`]=`${names[woods.indexOf(wood)]} ${reach==='half'?'Half':'Full'}-reach Jetty Bracket`;
  }
}
for (const material of ['thatch','slate']) {
  model(`${material}_mesh`,{parent:'minecraft:block/block',textures:{particle:`${ns}:blocks/${material}`},elements:[]});
  for (const ridge of [false,true]) {
    const name = `${material}_${ridge?'ridge':'roof'}`, variants = {};
    for (const facing of Object.keys(facings)) for (const shape of ridge?['single','start','middle','end']:shapes)
      variants[ridge?`ends=${shape},facing=${facing}`:`facing=${facing},shape=${shape}`] = {model:`${ns}:${material}_mesh`};
    state(name,variants); item(name,`${material}_mesh`);
    lang[`tile.${ns}.${name}.name`] = `${material==='thatch'?'Thatch':'Slate'} ${ridge?'Ridge Cap':'Roof'}`;
  }
}
for (const locale of ['en_US','en_GB']) expected.set(`${assets}/lang/${locale}.lang`,Object.entries(lang).map(([k,v])=>`${k}=${v}`).join('\n')+'\n');
expected.set('src/main/resources/mcmod.info',JSON.stringify([{modid:ns,name:'Iron Age Architecture',description:'Cob walls, timber frames, thatch and slate roofs for the Iron Age series.',version:'${version}',mcversion:'${mcversion}',url:'https://github.com/MinecraftModDevelopmentMods/IronAgeArchitecture',authorList:['SkyBlade1978','FoxNekitsune'],credits:'Original 2019 scaffold and model drafts retained in repository history.',dependencies:[]}],null,2)+'\n');
expected.set('src/main/resources/pack.mcmeta',JSON.stringify({pack:{pack_format:2,description:'Iron Age Architecture resources'}},null,2)+'\n');
let failures=[];
// Remove only the exact generated frame meshes superseded by the shared wall model.
const obsolete=[...Array.from({length:4},(_,n)=>`template_frame_${n}`),
  ...woods.flatMap(wood=>Array.from({length:4},(_,n)=>`timber_frame_${wood}_${n}`))];
for(const name of obsolete) {
  const file=path.join(root,assets,'models/block',name+'.json');
  if(fs.existsSync(file)) {
    if(check) failures.push('obsolete generated frame mesh '+name);
    else fs.unlinkSync(file);
  }
}
for(const wood of woods) {
  const file=path.join(root,assets,'textures/blocks',`beam_${wood}.png`);
  if(!fs.existsSync(file)) failures.push('missing beam texture '+wood);
  else {const png=fs.readFileSync(file);if(png.readUInt32BE(16)!==32||png.readUInt32BE(20)!==32) failures.push('beam texture must be 32x32: '+wood);}
}
for (const [file,content] of expected) {
  const dest=path.join(root,file);
  if (check) { if (!fs.existsSync(dest)||fs.readFileSync(dest,'utf8')!==content) failures.push(file); }
  else {fs.mkdirSync(path.dirname(dest),{recursive:true});fs.writeFileSync(dest,content);}
}
// Validate all local parents and texture references, including inherited slots.
for(const [file,content] of expected) if(file.includes('/models/')&&file.endsWith('.json')) {
  const value=JSON.parse(content);
  if(value.parent?.startsWith(ns+':')&&!expected.has(`${assets}/models/${value.parent.split(':')[1]}.json`)) failures.push('missing parent '+file);
  for(const texture of Object.values(value.textures||{})) if(texture.startsWith(ns+':')&&!fs.existsSync(path.join(root,`${assets}/textures/${texture.split(':')[1]}.png`))) failures.push('missing texture '+texture);
}
if(failures.length) {console.error(failures.join('\n'));process.exitCode=1;}
else console.log(`${check?'Verified':'Generated'} ${expected.size} resources; ${Object.keys(lang).length-1} visible block variants.`);
