package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import zone.moddev.mc.ironagearchitecture.Era;
import zone.moddev.mc.ironagearchitecture.EraTabs;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.ironagearchitecture.block.FireplaceBlock;
import zone.moddev.mc.ironagearchitecture.block.FireplaceDraft;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.ArchitectureConfig;
import zone.moddev.mc.ironagearchitecture.block.RoofBlock;
import zone.moddev.mc.ironagearchitecture.client.RoofBakedModel;
import zone.moddev.mc.ironagearchitecture.block.FrameBlock;
import zone.moddev.mc.ironagearchitecture.block.JettyFloorBlock;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry;
import zone.moddev.mc.ironagearchitecture.client.WallBakedModel;
import zone.moddev.mc.ironagearchitecture.block.WindowBlock;
import zone.moddev.mc.ironagearchitecture.client.WindowBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;

/** Opt-in native client capture. No registration or launch path in the player mod. */
public final class ClientProbe extends ProbeProxy {
    private int phase, ticks, paletteRetries;
    @Override public void init() { if(Boolean.getBoolean("iaa.clientProbe")) FMLCommonHandler.instance().bus().register(this); }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getMinecraft();
        ticks++;
        if(phase==0&&mc.currentScreen instanceof GuiMainMenu&&ticks>60) {
            mc.gameSettings.pauseOnLostFocus=false;
            phase=1;ticks=0;
            String preview = System.getProperty("iaa.previewName",
                "Smart Walls Preview");
            if (new java.io.File(mc.mcDataDir, "saves/" + preview).exists())
                throw new IllegalStateException("Use a fresh world for the native preview");
            mc.launchIntegratedServer(preview,preview,new WorldSettings(137,GameType.CREATIVE,false,false,WorldType.FLAT));
        } else if(phase==1&&mc.player!=null) {
            phase=2;ticks=0;
            mc.displayGuiScreen(null);mc.gameSettings.hideGUI=true;mc.gameSettings.renderDistanceChunks=8;
            mc.gameSettings.showInventoryAchievementHint=false;
            mc.setIngameNotInFocus();
            mc.player.capabilities.allowFlying=true;mc.player.capabilities.isFlying=true;
            if (ArchitectureConfig.clayTiles && ArchitectureConfig.frames)
                lookAt(mc, -10, 12, -24, -9, 16, 0);
            else lookAt(mc,48,102,-85,48,73,4);
        } else if(phase==2&&ticks>260) {
            for(RoofBlock roof:ArchitectureBlocks.ROOFS) for(IBlockState state:roof.getBlockState().getValidStates()) {
                IBakedModel model=mc.getBlockRendererDispatcher().getModelForState(state);
                if(!(model instanceof RoofBakedModel)||model.getQuads(state,null,0).isEmpty()) throw new IllegalStateException("Roof model missing "+state);
                if (!model.getParticleTexture().getIconName().equals("ironagearchitecture:blocks/" + roof.textureKey()))
                    throw new IllegalStateException("Wrong roof sprite " + state);
            }
            for(FrameBlock wall:ArchitectureBlocks.meshes()) {
                for(IBlockState state:wall.getBlockState().getValidStates()) {
                    IBakedModel model=mc.getBlockRendererDispatcher().getModelForState(state);
                    if(!(model instanceof WallBakedModel)||model.getQuads(state,null,0).isEmpty()) throw new IllegalStateException("Wall model missing "+state);
                    for(net.minecraft.client.renderer.block.model.BakedQuad quad:model.getQuads(state,null,0))
                        if(quad.getSprite().getIconName().contains("missingno")) throw new IllegalStateException("Missing material sprite "+state);
                    if(wall instanceof JettyFloorBlock) verifyBracketShell((JettyFloorBlock)wall,state,model);
                }
                for(int meta=0;ArchitectureBlocks.VISIBLE.contains(wall)&&meta<wall.variants();meta++) {
                    IBakedModel model=mc.getRenderItem().getItemModelMesher().getItemModel(new ItemStack(wall,1,meta));
                    if(!(model instanceof WallBakedModel)||model.getQuads(null,null,0).isEmpty()) throw new IllegalStateException("Wall item model missing "+wall+" "+meta);
                }
            }
            for(WindowBlock window:ArchitectureBlocks.WINDOWS) for(IBlockState state:window.getBlockState().getValidStates()) {
                IBakedModel model=mc.getBlockRendererDispatcher().getModelForState(state);
                if(!(model instanceof WindowBakedModel)||model.getQuads(state,null,0).isEmpty()) throw new IllegalStateException("Missing window model "+state);
                for(net.minecraft.client.renderer.block.model.BakedQuad quad:model.getQuads(state,null,0)) if(quad.getSprite().getIconName().contains("missingno")) throw new IllegalStateException("Missing window texture "+state);
            }
            checkFireplaceModels(mc);
            if(ArchitectureBlocks.SMART_COB!=null)for(int finish=0;finish<2;finish++) {
                IBlockState state=ArchitectureBlocks.SMART_COB.getStateFromMeta(finish);IBakedModel model=mc.getBlockRendererDispatcher().getModelForState(state);
                if(!(model instanceof zone.moddev.mc.ironagearchitecture.client.SmartCobBakedModel)||model.getQuads(state,null,0).isEmpty()||model.getParticleTexture().getIconName().contains("missingno"))throw new IllegalStateException("Missing smart cob model or texture");
            }
            if (ArchitectureConfig.clayTiles) {
                for (net.minecraft.block.Block panel : new net.minecraft.block.Block[]{ArchitectureBlocks.CLAY_HANGING, ArchitectureBlocks.CLAY_HANGING_THIN}) {
                    for (IBlockState state : panel.getBlockState().getValidStates()) {
                        IBakedModel model = mc.getBlockRendererDispatcher().getModelForState(state);
                        if (!model.getParticleTexture().getIconName().equals("ironagearchitecture:blocks/clay_tile"))
                            throw new IllegalStateException("Wrong hanging tile sprite " + state);
                    }
                }
                checkDrainModels(mc);
                IBakedModel lining = mc.getBlockRendererDispatcher().getModelForState(
                    ArchitectureBlocks.CLAY_FASCIA_LINED.getDefaultState());
                if (lining.getQuads(null, null, 0).stream().noneMatch(
                    quad -> "minecraft:blocks/planks_big_oak".equals(quad.getSprite().getIconName())))
                    throw new IllegalStateException("Soffit lining missing from fascia mesh");
                IBakedModel soffit = mc.getRenderItem().getItemModelMesher().getItemModel(new ItemStack(ArchitectureBlocks.SOFFIT));
                if (soffit.getParticleTexture().getIconName().contains("missingno"))
                    throw new IllegalStateException("Missing soffit item model");
                phase=ArchitectureConfig.smartCob?90:80;ticks=0;
                if (phase == 70) {
                    setPreviewGameType(mc, GameType.SPECTATOR);
                    setPreviewDoor(mc, true);
                }
                return;
            }
            if(ArchitectureConfig.smartCob){phase=90;ticks=0;return;}
            capture(mc,"jettied-street-overview.png");
            lookAt(mc,-5,74,-7,3,69.5,2);phase=3;ticks=0;
        } else if(phase==3&&ticks>120) {
            capture(mc,"half-block-corner.png");lookAt(mc,24,65.6,-3.5,24,68.7,-.2);phase=4;ticks=0;
        } else if(phase==4&&ticks>120) {
            capture(mc,"whole-block-underside.png");
            lookAt(mc,23.5,66.1,-1.8,24.5,68.2,-.35);phase=40;ticks=0;
        } else if(phase==40&&ticks>100) {
            capture(mc,"full-bracket-left.png");lookAt(mc,25.7,66.1,-1.8,24.5,68.2,-.35);phase=41;ticks=0;
        } else if(phase==41&&ticks>100) {
            capture(mc,"full-bracket-right.png");lookAt(mc,24.5,65.9,-2,24.5,68.25,-.3);phase=42;ticks=0;
        } else if(phase==42&&ticks>100) {
            capture(mc,"full-bracket-base.png");lookAt(mc,4.5,66,-1.6,4.5,68.2,.1);phase=43;ticks=0;
        } else if(phase==43&&ticks>100) {
            capture(mc,"half-bracket-base.png");lookAt(mc,34,81,-10,43,73,1);phase=5;ticks=0;
        } else if(phase==5&&ticks>120) {
            capture(mc,"successive-half-jetties.png");lookAt(mc,55,80,-9,64,72,1);phase=6;ticks=0;
        } else if(phase==6&&ticks>120) {
            capture(mc,"two-sided-jetty.png");lookAt(mc,5,71,5,5,70,-1);phase=7;ticks=0;
        } else if(phase==7&&ticks>120) {
            capture(mc,"upper-room-interior.png");lookAt(mc,11,72,-13,12,69,0);phase=50;ticks=0;
        } else if(phase>=50&&phase<=57&&ticks>120) {
            String[] names={"glazed-house-and-comparison.png","closed-diamond-window.png","open-rectangular-casement.png","window-from-interior.png","leaded-window-woods.png","chunk-border-open-casement.png","horizontal-window-corner.png","horizontal-window-interior.png"};
            double[][] cameras={{3,66,-3,3,66.7,.15},{4.5,71,-3,3.5,70.8,-.3},{3,70,3,3,70.7,-.25},{15,68,18,15,65,23},{19,66,26,15.5,65,27.5},{-2,70,-3,1.3,70.9,-.35},{1.8,70,3,1,70.9,-.3}};
            capture(mc,names[phase-50]);
            if(phase<57){double[] c=cameras[phase-50];lookAt(mc,c[0],c[1],c[2],c[3],c[4],c[5]);phase++;ticks=0;return;}
            checkPalettes(mc);
            mc.displayGuiScreen(new PaletteGui(mc,Era.IRON_AGE,true));phase=8;ticks=0;
        } else if (phase>=60&&phase<=65) {
            double[][] cameras={{-62,25,-16,-46,17,1},{-44.5,11.7,-15,-45.6,16.5,-.4},
                {-51.8,15.5,-6.2,-55.2,18.8,-.6},{-31.8,10.4,-6,-35.4,14.2,-.4},
                {-32.7,8.9,-2.8,-34.5,10.55,.5},{-32.7,8.9,-2.8,-34.5,10.55,.5}};
            double[] camera=cameras[phase-60];
            lookAt(mc,camera[0],camera[1],camera[2],camera[3],camera[4],camera[5]);
            if (ticks<=130) return;
            String[] names={"rye-eaves-overview.png","rye-eaves-street.png",
                "rye-eaves-exposed.png","rye-eaves-lined.png","rye-eaves-shoe.png","rye-eaves-rain.png"};
            if (phase == 65 && !rainReady(mc)) {
                if (ticks > 300) throw new IllegalStateException("Preview rain did not reach the pipe foot");
                return;
            }
            capture(mc,names[phase-60]);
            if (phase == 64) setPreviewRain(mc, true);
            if (phase < 65) {phase++;ticks=0;return;}
            setPreviewRain(mc, false);
            checkPalettes(mc);
            mc.displayGuiScreen(new PaletteGui(mc,Era.IRON_AGE,true));phase=8;ticks=0;
        } else if(phase>=90&&phase<=96) {
            captureRoundedCob(mc);
        } else if (phase >= 80 && phase <= 87) {
            captureSmartWalls(mc);
        } else if (phase >= 70 && phase <= 75) {
            captureFireplacePreview(mc);
        } else if(phase==8) {
            if(!ensurePalette(mc,Era.IRON_AGE)) {ticks=0;return;}
            if(ticks>80) {capture(mc,"iron-age-palette.png");mc.displayGuiScreen(new PaletteGui(mc,Era.LATE_MEDIEVAL_TUDOR,false));phase=9;ticks=0;}
        } else if(phase==9) {
            if(!ensurePalette(mc,Era.LATE_MEDIEVAL_TUDOR)) {ticks=0;return;}
            if(ticks>80) {
                capture(mc,"late-medieval-tudor-palette.png");mc.displayGuiScreen(null);
                System.out.println("IAA_CLIENT_PROBE_PASS creativeIronAge="+EraProbe.sharedVariants()+" creativeLateMedievalTudor="+EraProbe.expectedVariants());phase=10;
                mc.shutdown();
            }
        }
    }
    private void captureRoundedCob(Minecraft mc) {
        double[][] cameras={{-39,10,-45,-39,14,4},{-71.5,10.2,-2.5,-68.6,12,.6},{-49,10.3,-7,-40,12.7,.8},
            {-6.5,10.2,14,-10.1,13,6},{-39.5,11.2,5,-39.5,12.7,.5},{-64,10,-18,-61,11.6,-10},{-36,32,16,-36,10,5}};
        String[] names={"rounded-cob-street.png","rounded-cob-cottage-corner.png","rounded-cob-oval.png","rounded-cob-bay.png","rounded-cob-interior.png","rounded-cob-square-comparison.png","rounded-cob-overhead-cutaway.png"};
        double[] c=cameras[phase-90];lookAt(mc,c[0],c[1],c[2],c[3],c[4],c[5]);
        if(ticks<=150)return;
        if(phase==90) {
            BlockPos at=new BlockPos(-69,11,0);net.minecraft.world.World server=mc.getIntegratedServer().worldServerForDimension(0);
            zone.moddev.mc.ironagearchitecture.block.SmartCobData expected=zone.moddev.mc.ironagearchitecture.block.SmartCobData.get(server),actual=zone.moddev.mc.ironagearchitecture.block.SmartCobData.get(mc.world);
            if(!actual.contains(at)||!expected.profile(at).equals(actual.profile(at)))throw new IllegalStateException("Cob contours did not reach the client");
            System.out.println("IAA_COB_CLIENT_PASS matchingProfiles=true");
        }
        capture(mc,names[phase-90]);
        if(phase==95)mc.getIntegratedServer().addScheduledTask(()->RoundedCobPreview.roofs(mc.getIntegratedServer().worldServerForDimension(0),false));
        if(phase<96){phase++;ticks=0;return;}
        mc.getIntegratedServer().addScheduledTask(()->{
            WorldServer world=mc.getIntegratedServer().worldServerForDimension(0);RoundedCobPreview.roofs(world,true);
            for(EntityPlayer player:world.playerEntities) {
                BlockPos spawn=RoundedCobPreview.SPAWN;player.setPositionAndUpdate(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5);
                player.inventory.setInventorySlotContents(0,new ItemStack(ArchitectureBlocks.SMART_COB,64,0));
                player.inventory.setInventorySlotContents(1,new ItemStack(ArchitectureBlocks.SMART_COB,64,1));
                player.inventory.setInventorySlotContents(2,new ItemStack(net.minecraft.init.Blocks.GLASS_PANE,64));
                player.inventory.setInventorySlotContents(3,new ItemStack(net.minecraft.init.Items.SPRUCE_DOOR,16));
                player.inventory.setInventorySlotContents(4,new ItemStack(net.minecraft.block.Block.getBlockFromName("ironagearchitecture:thatch_roof"),64));
                player.inventory.setInventorySlotContents(5,new ItemStack(net.minecraft.block.Block.getBlockFromName("ironagearchitecture:thatch_ridge"),32));
                player.inventory.setInventorySlotContents(6,new ItemStack(net.minecraft.init.Blocks.PLANKS,64,1));
            }
        });
        checkPalettes(mc);mc.displayGuiScreen(new PaletteGui(mc,Era.IRON_AGE,true));phase=8;ticks=0;
    }
    private void captureSmartWalls(Minecraft mc) {
        double[][] cameras={{-12,10,-25,-10,15,0},{-20,10,-10,-21.5,14,0},
            {-11,10,-9,-10.5,15,-.5},{12,11,-11,2,16,-.3},{10,12,6,4,16,-.5},{-17,10.2,1.2,-19.2,12.4,4}};
        String[] names={"smart-walls-street.png","smart-small-cottage.png","smart-half-jetty.png","smart-full-jetty.png","smart-corners-and-side.png","smart-l-shaped-return.png"};
        if(Boolean.getBoolean("iaa.surfacePreview")) {
            cameras=java.util.Arrays.copyOf(cameras,8);
            cameras[6]=new double[]{-20,10.1,11.2,-20,12.1,14.5};
            cameras[7]=new double[]{-10.1,12.5,-2.3,-10.6,15.2,-.7};
            names=java.util.Arrays.copyOf(names,8);
            names[6]="manual-brace-end-surfaces.png";names[7]="smart-jetty-brace-surfaces.png";
        }
        double[] camera=cameras[phase-80];
        lookAt(mc,camera[0],camera[1],camera[2],camera[3],camera[4],camera[5]);
        if(ticks<=150) return;
        if(phase==80) {
            net.minecraft.world.World server=mc.getIntegratedServer().worldServerForDimension(0);
            BlockPos at=new BlockPos(-11,15,-1);
            int expected=zone.moddev.mc.ironagearchitecture.block.SmartWallData.get(server).code(at);
            int actual=zone.moddev.mc.ironagearchitecture.block.SmartWallData.get(mc.world).code(at);
            if(expected==0||expected!=actual) throw new IllegalStateException("Smart layout did not reach the client");
            System.out.println("IAA_SMART_CLIENT_PASS serverLayout=true matchingClient=true");
        }
        capture(mc,names[phase-80]);
        if(phase<80+names.length-1){phase++;ticks=0;return;}
        mc.getIntegratedServer().addScheduledTask(()->{
            WorldServer world=mc.getIntegratedServer().worldServerForDimension(0);
            for(EntityPlayer player:world.playerEntities) {
                BlockPos spawn=SmartWallPreview.SPAWN;
                player.setPositionAndUpdate(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5);
                player.inventory.setInventorySlotContents(0,new ItemStack(ArchitectureBlocks.findSmart("spruce",0),64,1));
                player.inventory.setInventorySlotContents(1,new ItemStack(ArchitectureBlocks.findSmart("spruce",1),64,1));
                player.inventory.setInventorySlotContents(2,new ItemStack(ArchitectureBlocks.findWindow("spruce",1,true,true,false),32));
                player.inventory.setInventorySlotContents(3,new ItemStack(ArchitectureBlocks.findFloor("spruce",zone.moddev.mc.ironagearchitecture.block.FloorShape.FULL,null),64));
                player.inventory.setInventorySlotContents(4,new ItemStack(net.minecraft.block.Block.getBlockFromName("ironagearchitecture:clay_tile_roof"),64));
                player.inventory.setInventorySlotContents(5,ArchitectureConfig.buildingMode.manual()
                    ?new ItemStack(ArchitectureBlocks.findWall("spruce",zone.moddev.mc.ironagearchitecture.block.FrameFamily.POST),64,2)
                    :new ItemStack(ArchitectureBlocks.findSmart("spruce",0),64,1));
                player.inventory.setInventorySlotContents(6,new ItemStack(ArchitectureBlocks.findSmart("oak",0),64));
                player.inventory.setInventorySlotContents(7,new ItemStack(ArchitectureBlocks.findSmart("dark_oak",0),64,1));
            }
        });
        checkPalettes(mc);
        mc.displayGuiScreen(new PaletteGui(mc,Era.IRON_AGE,true));phase=8;ticks=0;
    }

    private void captureFireplacePreview(Minecraft mc) {
        double[][] cameras = {
            {-37.5, 10.03, 1.4, -37.5, 11.1, 4.0},
            {-37.1, 14.03, 1.68, -38.5, 15.05, 4.0},
            {-44.25, 12.02, 2.3, -44.5, 13.05, 6.0},
            {-32, 9.5, -12.4, -39.5, 17.5, 3.6},
            {-32, 9.5, -12.4, -39.5, 17.5, 3.6},
            {-30, 20, 9, -38.5, 20.8, 4.4}
        };
        String[] names = {"stone-inglenook.png", "upstairs-hearth.png", "brick-hearth.png",
            "rye-one-pot-smoking.png", "rye-both-pots-smoking.png", "rye-stack-and-collar.png"};
        mc.gameSettings.fovSetting = phase == 70 ? 86 : 70;
        double[] camera = cameras[phase - 70];
        lookAt(mc, camera[0], camera[1], camera[2], camera[3], camera[4], camera[5]);

        if (phase >= 73 && (ticks == 114 || ticks == 121 || ticks == 128)) {
            showOutlet(mc, RyeFireplacePreview.LOWER_POT, true);
            showOutlet(mc, RyeFireplacePreview.UPPER_POT, phase >= 74);
        }
        if (ticks <= 130) return;
        capture(mc, names[phase - 70]);
        if (phase == 70) setPreviewDoor(mc, false);
        if (phase == 70 || phase == 72 || phase == 73) setPreviewUpperFire(mc, phase != 72);
        if (phase < 75) {
            phase++;
            ticks = 0;
            return;
        }
        setPreviewGameType(mc, GameType.CREATIVE);
        System.out.println("IAA_FIREPLACE_PREVIEW_PASS onePot=true twoPots=true");
        checkPalettes(mc);
        mc.displayGuiScreen(new PaletteGui(mc, Era.IRON_AGE, true));
        phase = 8;
        ticks = 0;
    }

    private void setPreviewGameType(Minecraft mc, GameType gameType) {
        mc.getIntegratedServer().addScheduledTask(() -> {
            WorldServer world = mc.getIntegratedServer().worldServerForDimension(0);
            for (EntityPlayer player : world.playerEntities) {
                player.setGameType(gameType);
                if (gameType == GameType.CREATIVE) {
                    BlockPos spawn = RyeTilePreview.SPAWN;
                    player.setPositionAndUpdate(spawn.getX() + .5, spawn.getY(), spawn.getZ() + .5);
                }
            }
        });
    }

    private void setPreviewDoor(Minecraft mc, boolean open) {
        mc.getIntegratedServer().addScheduledTask(() -> {
            WorldServer world = mc.getIntegratedServer().worldServerForDimension(0);
            BlockPos door = new BlockPos(-38, 10, 1);
            world.setBlockState(door, world.getBlockState(door).withProperty(BlockDoor.OPEN, open), 3);
        });
    }

    private void setPreviewUpperFire(Minecraft mc, boolean lit) {
        mc.getIntegratedServer().addScheduledTask(() -> {
            WorldServer world = mc.getIntegratedServer().worldServerForDimension(0);
            BlockPos upper = RyeFireplacePreview.UPPER_FIRE;
            world.setBlockState(upper, world.getBlockState(upper).withProperty(FireplaceBlock.LIT, lit), 3);
        });
    }

    private void showOutlet(Minecraft mc, BlockPos pos, boolean expected) {
        IBlockState state = mc.world.getBlockState(pos);
        boolean active = FireplaceDraft.smokeOutlet(mc.world, pos, state) != null;
        if (active != expected) throw new IllegalStateException("Unexpected fireplace preview draft at " + pos);
        if (active) state.getBlock().randomDisplayTick(state, mc.world, pos, new java.util.Random(137L + ticks));
    }

    private static void checkFireplaceModels(Minecraft mc) {
        for (FireplaceBlock fireplace : ArchitectureBlocks.FIREPLACES) {
            for (IBlockState state : fireplace.getBlockState().getValidStates()) {
                IBakedModel model = mc.getBlockRendererDispatcher().getModelForState(state);
                java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads = model.getQuads(state, null, 0);
                if (quads.isEmpty() || quads.stream().anyMatch(quad -> quad.getSprite().getIconName().contains("missingno")))
                    throw new IllegalStateException("Missing fireplace mesh or texture " + state);
            }
            IBakedModel item = mc.getRenderItem().getItemModelMesher().getItemModel(new ItemStack(fireplace));
            if (item.getQuads(null, null, 0).isEmpty() || !fireplace.masonry.texture.equals(item.getParticleTexture().getIconName()))
                throw new IllegalStateException("Missing fireplace inventory model " + fireplace);
        }
    }

    private static void checkDrainModels(Minecraft mc) {
        for (ItemStack stack : ArchitectureBlocks.visibleStacks()) {
            String key = stack.getItem().getRegistryName().getResourcePath();
            if (!key.startsWith("iron_gutter") && !key.startsWith("iron_downpipe")) continue;
            IBakedModel model = mc.getRenderItem().getItemModelMesher().getItemModel(stack);
            if (!(model instanceof RoofBakedModel) || model.getQuads(null, null, 0).isEmpty()
                    || !"minecraft:blocks/iron_block".equals(model.getParticleTexture().getIconName()))
                throw new IllegalStateException("Missing drain item model " + stack);
        }
    }

    private static void setPreviewRain(Minecraft mc, boolean rain) {
        mc.getIntegratedServer().addScheduledTask(() -> {
            net.minecraft.world.storage.WorldInfo info = mc.getIntegratedServer().worldServerForDimension(0).getWorldInfo();
            info.setCleanWeatherTime(rain ? 0 : 12000);
            info.setRainTime(12000);
            info.setRaining(rain);
        });
    }

    private static boolean rainReady(Minecraft mc) {
        net.minecraft.util.math.BlockPos foot = RyeTilePreview.RAIN_FOOT;
        if (zone.moddev.mc.ironagearchitecture.block.DrainRun.canDrip(mc.world.getBlockState(foot), mc.world, foot)) {
            System.out.println("IAA_GUTTER_RAIN_PREVIEW_PASS");
            return true;
        }
        return false;
    }

    private void checkPalettes(Minecraft mc) {
            for(Era era:Era.values()) {
                java.util.List<ItemStack> items=new java.util.ArrayList<>();EraTabs.tab(era).displayAllRelevantItems(items);
                int expected=era==Era.IRON_AGE?EraProbe.sharedVariants():EraProbe.expectedVariants();
                if(era==Era.LATE_MEDIEVAL_TUDOR&&ArchitectureConfig.buildingMode.automatic()
                        &&!(items.get(0).getItem() instanceof zone.moddev.mc.ironagearchitecture.block.SmartWallItem))
                    throw new IllegalStateException("Smart walls should be first in the palette");
                if(items.size()!=expected) throw new IllegalStateException("Creative tab "+era+" expected "+expected+" got "+items.size());
                for(ItemStack item:items) if(!zone.moddev.mc.ironagearchitecture.EraCatalog.eras(item.getItem()).contains(era)) throw new IllegalStateException("Wrong era item "+item);
                for(ItemStack item:items) {
                    java.util.List<String> tooltip=new java.util.ArrayList<>();item.getItem().addInformation(item,mc.player,tooltip,false);
                    if(tooltip.isEmpty()) throw new IllegalStateException("Missing crafting hint "+item);
                    for(String line:tooltip) if(line.startsWith("hint.ironagearchitecture.")) throw new IllegalStateException("Untranslated crafting hint "+line);
                }
            }
    }
    private void verifyBracketShell(JettyFloorBlock floor,IBlockState state,IBakedModel model) {
        if(floor.bracket==null) return;
        java.util.List<WallGeometry.Part> parts=floor.parts(state,0);
        WallGeometry.Part bracket=parts.get(parts.size()-1);
        if(bracket.horizontal||bracket.grain!=WallGeometry.Grain.FALLING) return;
        double x=0,y=0;for(double[] p:bracket.outline) {x+=p[0]/bracket.outline.length;y+=p[1]/bracket.outline.length;}
        double[] centre=bracket.transform(x,y,(bracket.front+bracket.back)/2,state.getValue(FrameBlock.FACING));
        int checked=0;
        for(net.minecraft.client.renderer.block.model.BakedQuad quad:model.getQuads(state,null,0)) {
            int[] data=quad.getVertexData();int stride=data.length/4;double[][] points=new double[4][3];double height=0;
            for(int v=0;v<4;v++) for(int axis=0;axis<3;axis++) {points[v][axis]=16*Float.intBitsToFloat(data[v*stride+axis]);if(axis==1) height+=points[v][axis]/4;}
            if(height>=7.99) continue; // Floor timbers and corner ties are above the bracket.
            double[] a=points[0];double dot=0;
            for(int i=1;i<3&&Math.abs(dot)<1e-7;i++) {
                double[] b=points[i],c=points[i+1];
                double nx=(b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]);
                double ny=(b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]);
                double nz=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
                dot=nx*(a[0]-centre[0])+ny*(a[1]-centre[1])+nz*(a[2]-centre[2]);
            }
            if(dot<=1e-7) throw new IllegalStateException("Inward or degenerate baked bracket face "+state+" dot="+dot+" centre="+java.util.Arrays.toString(centre)+" points="+java.util.Arrays.deepToString(points));
            checked++;
        }
        if(checked<5) throw new IllegalStateException("Incomplete baked bracket shell "+state+" faces="+checked);
    }
    private boolean ensurePalette(Minecraft mc,Era era) {
        if(mc.currentScreen instanceof PaletteGui&&((PaletteGui)mc.currentScreen).getSelectedTabIndex()==EraTabs.tab(era).getTabIndex()) {paletteRetries=0;return true;}
        if(++paletteRetries==1) System.out.println("IAA_PALETTE_RETRY screen="+(mc.currentScreen==null?"null":mc.currentScreen.getClass().getName())+" creative="+mc.playerController.isInCreativeMode()+" selected="+(mc.currentScreen instanceof GuiContainerCreative?((GuiContainerCreative)mc.currentScreen).getSelectedTabIndex():-1)+" desired="+EraTabs.tab(era).getTabIndex());
        if(paletteRetries>100) throw new IllegalStateException("Creative palette would not remain open "+era);
        mc.displayGuiScreen(new PaletteGui(mc,era,false));return false;
    }
    private static final class PaletteGui extends GuiContainerCreative {
        private final Era era;private final boolean nextPage;
        PaletteGui(Minecraft mc,Era era,boolean nextPage) {super(mc.player);this.era=era;this.nextPage=nextPage;}
        @Override public void initGui() {
            super.initGui();
            try {
                if(nextPage) for(GuiButton button:buttonList) if(">".equals(button.displayString)&&button.enabled) {super.actionPerformed(button);break;}
                boolean selected=false;
                for(java.lang.reflect.Method method:GuiContainerCreative.class.getDeclaredMethods())
                    if(java.lang.reflect.Modifier.isPrivate(method.getModifiers())&&method.getReturnType()==Void.TYPE&&java.util.Arrays.equals(method.getParameterTypes(),new Class<?>[]{CreativeTabs.class})) {
                        method.setAccessible(true);method.invoke(this,EraTabs.tab(era));selected=true;break;
                    }
                if(!selected) throw new IllegalStateException("Creative tab setter missing");
                if(getSelectedTabIndex()!=EraTabs.tab(era).getTabIndex()) throw new IllegalStateException("Creative tab selection did not stick");
            } catch(Exception failure) {throw new IllegalStateException("Palette preview failed",failure);}
        }
    }
    private void lookAt(Minecraft mc,double x,double y,double z,double tx,double ty,double tz) {
        double dx=tx-x,dz=tz-z;
        float yaw=(float)(Math.toDegrees(Math.atan2(dz,dx))-90);
        float pitch=(float)Math.toDegrees(Math.atan2(y+mc.player.getEyeHeight()-ty,Math.sqrt(dx*dx+dz*dz)));
        mc.player.setPositionAndRotation(x,y,z,yaw,pitch);
    }
    private void capture(Minecraft mc,String name) {
        ScreenShotHelper.saveScreenshot(mc.mcDataDir,name,mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
    }
}
