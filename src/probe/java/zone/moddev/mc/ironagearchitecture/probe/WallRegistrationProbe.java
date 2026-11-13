package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Counts real registry entries, not creative items or baked model variants. */
final class WallRegistrationProbe {
    private static void require(boolean ok,String message) {if(!ok) throw new IllegalStateException("Wall registration: "+message);}
    static void verify() throws Exception {
        int registered=0;
        for(Block block:Block.REGISTRY) if(IronAgeArchitecture.MODID.equals(block.getRegistryName().getResourceDomain())) registered++;
        require(registered==ArchitectureBlocks.REGISTERED.size(),"registry count");
        if(ArchitectureConfig.frames) {
            for(String wood:ArchitectureBlocks.WOODS) {
                for(FrameFamily family:FrameFamily.values()) if(family!=FrameFamily.INFILL) {
                    require(present(family.registryPrefix+"_"+wood)==ArchitectureConfig.registerManualWalls(),"manual family "+wood+" "+family);
                    for(WallProfile profile:WallProfile.values()) if(!profile.corner||family==FrameFamily.TERMINAL)
                        require(present("partial_"+profile.key()+"_"+family.registryPrefix+"_"+wood)==ArchitectureConfig.registerManualWalls(),"manual profile "+wood+" "+profile);
                }
                require(present("smart_daub_corner_"+wood)==ArchitectureConfig.retainLegacyWallIds,"legacy corner "+wood);
                SmartWallBlock full=ArchitectureBlocks.findSmart(wood,0),thin=ArchitectureBlocks.findSmart(wood,1);
                Set<IBlockState> states=new HashSet<>();
                for(int meta=0;meta<16;meta++) {
                    IBlockState state=full.getStateFromMeta(meta);states.add(state);
                    require(full.getMetaFromState(state)==meta,"full/corner metadata round trip");
                    require(full.profile(state)==(meta<8?0:2),"full/corner footprint bit");
                    require(full.finish(state.getValue(FrameBlock.PANEL))==((meta>>2)&1),"finish bit");
                    require(full.mask(state)==SmartWallGeometry.mask(meta<8?0:2,EnumFacing.getHorizontal(meta&3)),"facing bits");
                    require(full.isFullCube(state)==(meta<8),"corner cube classification");
                    require(full.getUseNeighborBrightness(state)==(meta>=8),"corner neighbour lighting");
                    IBlockState oldThin=thin.getStateFromMeta(meta);
                    require(thin.getMetaFromState(oldThin)==(meta&7)&&thin.profile(oldThin)==1,"unchanged thin metadata");
                }
                require(states.size()==16,"all sixteen full/corner states used");
            }
        }
        verifyConfigUpgrade();
        System.out.println("IAA_WALL_REGISTRY_PASS blockIds="+registered+" manualIds="+(ArchitectureConfig.frames&&ArchitectureConfig.registerManualWalls()?144:0)
            +" smartIds="+ArchitectureBlocks.SMART_WALLS.size()+" packedStates=16 retainLegacy="+ArchitectureConfig.retainLegacyWallIds);
    }
    private static boolean present(String name) {return Block.REGISTRY.containsKey(new ResourceLocation(IronAgeArchitecture.MODID,name));}
    private static void verifyConfigUpgrade() throws Exception {
        boolean cob=ArchitectureConfig.cob,smartCob=ArchitectureConfig.smartCob,frames=ArchitectureConfig.frames,thatch=ArchitectureConfig.thatch,slate=ArchitectureConfig.slate,
            windows=ArchitectureConfig.windows,clay=ArchitectureConfig.clayTiles,fireplaces=ArchitectureConfig.fireplaces,retain=ArchitectureConfig.retainLegacyWallIds;
        BuildingMode mode=ArchitectureConfig.buildingMode;EraSelection eras=ArchitectureConfig.survivalEras;
        Path directory=Paths.get("wall-config-matrix",UUID.randomUUID().toString());Files.createDirectories(directory);
        try {
            Path fresh=directory.resolve("fresh.cfg");ArchitectureConfig.load(fresh.toFile());
            require(ArchitectureConfig.buildingMode==BuildingMode.AUTOMATIC&&!ArchitectureConfig.retainLegacyWallIds&&!ArchitectureConfig.registerManualWalls(),"fresh automatic defaults");
            ArchitectureConfig.load(fresh.toFile());require(!ArchitectureConfig.retainLegacyWallIds,"fresh config reload stays compact");
            Path old=directory.resolve("old-alpha.cfg");Files.write(old,"blocks {\n B:timberFrames=true\n}\n".getBytes(StandardCharsets.UTF_8));
            ArchitectureConfig.load(old.toFile());require(ArchitectureConfig.buildingMode==BuildingMode.AUTOMATIC&&ArchitectureConfig.retainLegacyWallIds,"old config keeps IDs");
            for(BuildingMode selected:BuildingMode.values()) {
                Path explicit=directory.resolve(selected.name()+".cfg");
                Files.write(explicit,("building {\n S:mode="+selected.name()+"\n B:retainLegacyWallIds=false\n}\n").getBytes(StandardCharsets.UTF_8));
                ArchitectureConfig.load(explicit.toFile());
                require(ArchitectureConfig.buildingMode==selected&&!ArchitectureConfig.retainLegacyWallIds&&ArchitectureConfig.registerManualWalls()==selected.manual(),"explicit mode "+selected);
            }
        } finally {
            ArchitectureConfig.cob=cob;ArchitectureConfig.smartCob=smartCob;ArchitectureConfig.frames=frames;ArchitectureConfig.thatch=thatch;ArchitectureConfig.slate=slate;
            ArchitectureConfig.windows=windows;ArchitectureConfig.clayTiles=clay;ArchitectureConfig.fireplaces=fireplaces;
            ArchitectureConfig.retainLegacyWallIds=retain;ArchitectureConfig.buildingMode=mode;ArchitectureConfig.survivalEras=eras;
        }
    }
}
