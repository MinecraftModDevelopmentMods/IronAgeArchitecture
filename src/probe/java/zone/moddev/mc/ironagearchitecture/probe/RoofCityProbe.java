package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import java.nio.file.*;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;
import zone.moddev.mc.ironagearchitecture.roof.*;

/** Bulk fixture seeding, explicitly separate from measured production construction. */
public final class RoofCityProbe {
    private final WorldServer world;private final MinecraftServer server;
    private int houses,quiet,parts,timber;private boolean finished;
    private RoofCityProbe(WorldServer w,MinecraftServer s){world=w;server=s;}
    public static void start(WorldServer w,MinecraftServer s){w.getGameRules().setOrCreateGameRule("doFireTick","false");w.getGameRules().setOrCreateGameRule("doMobSpawning","false");w.setSpawnPoint(new BlockPos(220,75,220));net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(new RoofCityProbe(w,s));}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){if(finished||e.phase!=TickEvent.Phase.END)return;try{if(houses<500){int i=houses++,x=32+(i%23)*16,z=32+(i/23)*16,width=6+i%7,length=7+(i/7)%7;for(int a=x;a<x+width;a++)for(int b=z;b<z+length;b++){world.setBlockState(new BlockPos(a,63,b),Blocks.STONEBRICK.getDefaultState(),2);if(a==x||a==x+width-1||b==z||b==z+length-1)for(int y=64;y<68;y++)if(!(a==x+3&&b==z&&y<66))world.setBlockState(new BlockPos(a,y,b),ArchitectureBlocks.SMART_COB.getStateFromMeta(i%2),2);world.setBlockState(new BlockPos(a,68,b),ArchitectureBlocks.findFloor(ArchitectureBlocks.WOODS[i%6],FloorShape.FULL,null).getDefaultState(),2);}RoofPlan p=new RoofPlan();p.minX=x;p.maxX=x+width-1;p.minZ=z;p.maxZ=z+length-1;p.floorY=68;p.pitch=i%3;p.wood=i%6;p.alongX=(i&1)==0;RoofPlanner.solve(p);if(!p.valid())throw new IllegalStateException(p.error);for(Map.Entry<Long,RoofCell> entry:new ArrayList<>(p.cells.entrySet())){List<RoofPart> ps=new ArrayList<>();for(RoofPart part:entry.getValue().parts)ps.add(part.kind==RoofPart.COVER&&part.material==0?part.withMaterial(1+i%3):part);p.cells.put(entry.getKey(),new RoofCell(p.id,BlockPos.fromLong(entry.getKey()),ps,entry.getValue().windows));}for(Map.Entry<Long,RoofCell> cell:p.cells.entrySet()){BlockPos at=BlockPos.fromLong(cell.getKey());world.getChunkFromBlockCoords(at);RoofData.get(world).put(world,at,cell.getValue());world.setBlockState(at,RoofDesignInteraction.state(cell.getValue(),at),2);}RoofData.get(world).savePlan(p);parts+=p.cells.size();timber+=p.timber();if(i%50==0)System.out.println("IAA_ROOF_CITY_PREPARED "+houses);return;}
        if(SmartChunkWork.pending(world)||SmartCobEvents.pending(world)||SmartWallEvents.pending(world)){quiet=0;return;}if(++quiet<40)return;server.saveAllWorlds(false);Map<String,Object> result=new LinkedHashMap<>();result.put("passed",true);result.put("houses",houses);result.put("frameCells",parts);result.put("timberCells",timber);result.put("preparationOnly",true);result.put("runtimeSha256",System.getProperty("iaa.runtimeSha256"));Files.write(Paths.get(System.getProperty("iaa.receipt")),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result).getBytes(java.nio.charset.StandardCharsets.UTF_8));finished=true;System.out.println("IAA_ROOF_CITY_PREPARE_PASS");server.initiateShutdown();
    }catch(Exception error){finished=true;error.printStackTrace();server.initiateShutdown();}}
}
