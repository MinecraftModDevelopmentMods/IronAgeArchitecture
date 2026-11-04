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
const lang = {'itemGroup.ironagearchitecture':'Iron Age Architecture',
  'itemGroup.ironagearchitecture.iron_age':'Iron Age',
  'itemGroup.ironagearchitecture.late_medieval_tudor':'Late Medieval & Tudor'};
for (const [key, text] of Object.entries({
  'wall.post':'Shape three matching post panels into a wall pattern.',
  'wall.patterns':'Row: rails; column: studs; diagonal: braces; L: corners.',
  'wall.swap':'Craft alone to swap the paired pattern.',
  'wall.reset':'Three matching pieces in a row return post panels.',
  'wall.corner':'Two shaped corners side by side recover short end posts.',
  'wall.infill':'Full infill + four matching planks makes four post panels.',
  'cut':'Cut a vertical pair; rejoin a horizontal pair.',
  'wash':'Limewash singly, or eight pieces around bone meal.',
  'floor.brackets':'Full floor + one stick makes two matching brackets.',
  'floor.full':'Two full floors vertically make four half floors.',
  'floor.quarter':'Two quarter floors side by side make one half floor.',
  'floor.l':'Craft alone to recover three quarter floors.',
  'bracket.reach':'One full bracket makes two halves; two halves rejoin.',
  'roof.ridge':'Cut two slopes vertically for four ridge caps.',
  'roof.slope':'Two ridge caps side by side make one roof slope.',
  'clay.fascia':'Place facing uphill for a timber edge along the low side.',
  'clay.verge':'Face uphill and click the left or right half that projects past the gable.',
  'clay.ridge_end':'Look along the ridge from outside to finish its exposed end.',
  'clay.swap':'Craft alone to swap a clay slope and a full hanging tile panel.',
  'clay.place':'Match the neighbouring wall half, or sneak to choose it yourself.',
  'cob':'Craft alone to make plain wattle and daub.',
  'slab':'Two slabs side by side recover one cob.',
  'wall.short':'Two short walls side by side make one thin wall.',
  'stair':'Four stairs in a square recover three cob.'
})) lang[`hint.${ns}.${key}`] = text;
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
for (const [material,label] of [['thatch','Thatch'],['slate','Slate'],['clay_tile','Clay Tile']]) {
  model(`${material}_mesh`,{parent:'minecraft:block/block',textures:{particle:`${ns}:blocks/${material}`},elements:[]});
  for (const ridge of [false,true]) {
    const name = `${material}_${ridge?'ridge':'roof'}`, variants = {};
    const mesh = material === 'clay_tile' && ridge ? 'clay_tile_cap_mesh' : `${material}_mesh`;
    if (mesh === 'clay_tile_cap_mesh') model(mesh,{parent:'minecraft:block/block',textures:{particle:`${ns}:blocks/clay_tile_cap`},elements:[]});
    for (const facing of Object.keys(facings)) for (const shape of ridge?['single','start','middle','end']:shapes)
      variants[ridge?`ends=${shape},facing=${facing}`:`facing=${facing},shape=${shape}`] = {model:`${ns}:${mesh}`};
    state(name,variants); item(name,mesh);
    lang[`tile.${ns}.${name}.name`] = `${label} ${ridge?'Ridge Cap':'Roof'}`;
  }
}
for (const [part,label] of [['fascia','Fascia'],['verge','Verge'],['ridge_end','Ridge End']]) {
  const name = `clay_tile_${part}`, variants = {};
  const mesh = part === 'ridge_end' ? 'clay_tile_cap_mesh' : 'clay_tile_mesh';
  for (const facing of Object.keys(facings)) {
    if (part === 'fascia') {
      for (let ends=0;ends<4;ends++) for (let fitting=0;fitting<3;fitting++)
        variants[`ends=${ends},facing=${facing},fitting=${fitting}`] = {model:`${ns}:${mesh}`};
    } else for (const right of [false,true])
      variants[`facing=${facing},right=${right}`] = {model:`${ns}:${mesh}`};
  }
  state(name,variants); item(name,mesh);
  lang[`tile.${ns}.${name}.name`] = `Clay Tile ${label}`;
}
model('drain_mesh',{parent:'minecraft:block/block',textures:{particle:'minecraft:blocks/iron_block'}});
for (const [name,label] of [['iron_gutter','Iron Gutter'],['iron_gutter_outlet','Iron Gutter Outlet']]) {
  item(name,'drain_mesh');
  lang[`item.${ns}.${name}.name`] = label;
}
for (const [name,labels] of [
  ['iron_downpipe',['Iron Downpipe']],
  ['iron_downpipe_shoe',['Iron Downpipe Shoe']],
  ['iron_downpipe_offset',['Eave Offset','Half-block Offset','Full-block Outer Elbow','Full-block Inner Elbow']]
]) {
  const variants = {};
  for (const facing of Object.keys(facings)) for (let profile=0;profile<4;profile++)
    variants[`facing=${facing},profile=${profile}`] = {model:`${ns}:drain_mesh`};
  state(name,variants);
  for (const [i,label] of labels.entries()) {
    const suffix = labels.length > 1 ? '_' + i : '';
    item(name + suffix,'drain_mesh');
    lang[`tile.${ns}.${name}${suffix ? '.' + i : ''}.name`] = label;
  }
}
model('clay_tile_hanging',{parent:'minecraft:block/cube_all',textures:{all:`${ns}:blocks/clay_tile`}});
const tileFaces = Object.fromEntries(['down','up','north','south','west','east'].map(face =>
  [face, face === 'south' ? {texture:'#tile'} : {texture:'#tile',cullface:face}]));
model('clay_tile_hanging_thin',{parent:'minecraft:block/block',
  textures:{tile:`${ns}:blocks/clay_tile`,particle:`${ns}:blocks/clay_tile`},
  elements:[{from:[0,0,0],to:[16,16,8],faces:tileFaces}]});
for (const [name,label] of [['clay_tile_hanging','Hanging Clay Tiles'],['clay_tile_hanging_thin','Hanging Clay Tiles (Thin)']]) {
  state(name,Object.fromEntries(Object.entries(facings).map(([facing,y]) =>
    [`facing=${facing}`,{model:`${ns}:${name}`,...(name.endsWith('_thin')&&y?{y}:{})}])));
  item(name,name);
  lang[`tile.${ns}.${name}.name`] = label;
}
model('window_mesh',{parent:'minecraft:block/block',textures:{particle:'minecraft:blocks/glass_silver'}});
for(const [w,wood] of woods.entries()) for(const finish of [0,1]) for(const thin of [false,true]) for(const diamond of [true,false]) for(const right of [false,true]) for(const back of [false,true]) {
  if(back&&!thin) continue;
  const name=`leaded_window_${thin?'thin':'full'}_${finishes[finish]}_${diamond?'diamond':'rectangle'}_${wood}${right?'_right':''}${back?'_back':''}`;
  const variants={};for(const facing of Object.keys(facings)) for(const open of [false,true]) for(const powered of [false,true]) variants[`facing=${facing},open=${open},powered=${powered}`]={model:`${ns}:window_mesh`};
  state(name,variants);
  if(!right&&!back) {item(name,'window_mesh');lang[`tile.${ns}.${name}.name`]=`${finish?'Limewashed ':''}${names[w]} ${diamond?'Diamond':'Rectangular'} Leaded Window${thin?' (Thin)':''}`;}
}
state('open_window_leaf',Object.fromEntries(Object.keys(facings).map(facing=>[`facing=${facing}`,{model:`${ns}:window_mesh`}])));
for(let meta=0;meta<2;meta++) {item(`leaded_pane_${meta}`,'window_mesh');lang[`item.${ns}.leaded_pane.${meta}.name`]=`${meta?'Rectangular':'Diamond'} Leaded Pane`;}
for (const [key,label] of Object.entries({
  'gutter.fit':'Fit to the outside face of a bare clay fascia. Sneak and use an empty hand to recover it.',
  'gutter.outlet':'Place a downpipe beneath the outlet to carry rain down.',
  'gutter.craft':'Three iron ingots in a row make six gutters. Add an ingot to make an outlet.',
  'pipe.place':'Place against a pipe end to line up. Sneak, face the wall and click to pick the depth.',
  'pipe.craft':'Three iron ingots in a column make six pipes. Add a stick to make an offset.',
  'pipe.offset.0':'Fits under an outlet and brings the pipe back to the wall.',
  'pipe.offset.1':'Fits under an outside pipe and brings it half a block in.',
  'pipe.offset.2':'Place beneath the upper pipe with its side opening toward the wall.',
  'pipe.offset.3':'Place beside the outer elbow, one cell closer to the wall.',
  'pipe.cycle':'Craft alone to switch to the next offset shape.',
  'pipe.shoe':'Craft a downpipe alone. Fit beneath the last pipe to let rain spill out above the ground.'
})) lang[`hint.${ns}.${key}`] = label;
lang[`hint.${ns}.window.use`]='Right-click to open the casement; redstone operates the whole column.';
lang[`hint.${ns}.window.place`]='Stack to share a hinge; sneak to choose a different alignment.';
lang[`hint.${ns}.window.glaze`]='Craft alone to swap leading. Shape the wall before glazing.';
lang[`hint.${ns}.pane.use`]='Add to a matching post panel; craft alone to swap leading.';
lang[`message.${ns}.window.blocked`]='There is not enough clear space to open this window.';
lang[`message.${ns}.window.join`]='Close the adjoining windows before joining these sections.';
for (const locale of ['en_US','en_GB']) expected.set(`${assets}/lang/${locale}.lang`,Object.entries(lang).map(([k,v])=>`${k}=${v}`).join('\n')+'\n');
expected.set('src/main/resources/mcmod.info',JSON.stringify([{modid:ns,name:'Iron Age Architecture',description:'Cob walls, timber frames, leaded windows and traditional roofs for the Iron Age series.',version:'${version}',mcversion:'${mcversion}',url:'https://github.com/MinecraftModDevelopmentMods/IronAgeArchitecture',authorList:['SkyBlade1978','FoxNekitsune'],credits:'Original 2019 scaffold and model drafts retained in repository history.',dependencies:[]}],null,2)+'\n');
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
for(const name of [...woods.map(wood=>`beam_${wood}`),'clay_tile','clay_tile_cap']) {
  const file=path.join(root,assets,'textures/blocks',`${name}.png`);
  if(!fs.existsSync(file)) failures.push('missing material texture '+name);
  else {const png=fs.readFileSync(file);if(png.readUInt32BE(16)!==32||png.readUInt32BE(20)!==32) failures.push('material texture must be 32x32: '+name);}
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
else console.log(`${check?'Verified':'Generated'} ${expected.size} resources; ${Object.keys(lang).filter(k=>/^(tile|item)\./.test(k)&&k.endsWith('.name')).length} visible item variants.`);
