package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.text.TextComponentString;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.block.*;

/** A disposable street showing genuine additional rooms and their exposed underside. */
public final class JettyPreview {
    public void build(World world) {
        for(int x=-14;x<=106;x++) for(int z=-16;z<=20;z++) world.setBlockState(new BlockPos(x,63,z),
            (z<0?Blocks.COBBLESTONE:Blocks.GRASS).getDefaultState(),2);
        house(world,0,"dark_oak",new double[]{0,.5},false,1);
        house(world,20,"spruce",new double[]{0,1},false,1);
        house(world,40,"dark_oak",new double[]{0,.5,1},false,1);
        house(world,62,"oak",new double[]{0,.5,1.5},true,0);
        house(world,86,"spruce",new double[]{0,1,2},true,1);
        String[] labels={"Half-block jetty","Whole-block jetty","Half + half","Half + whole","Whole + whole"};
        int[] positions={4,24,44,66,90};
        for(int i=0;i<labels.length;i++) sign(world,new BlockPos(positions[i],64,-10),labels[i]);
        // Six woods and both finishes, with visible cut ends and all corner profiles.
        for(int w=0;w<6;w++) for(int finish=0;finish<2;finish++) {
            int ox=w*12,oz=15+finish*3;
            for(int p=0;p<5;p++) {
                WallProfile profile=new WallProfile[]{WallProfile.THIN,WallProfile.SHORT_LEFT,WallProfile.CORNER_L,WallProfile.CORNER_HALF_LEFT,WallProfile.CORNER_QUARTER}[p];
                PartialWallBlock block=ArchitectureBlocks.findPartial(ArchitectureBlocks.WOODS[w],profile.corner?FrameFamily.TERMINAL:FrameFamily.POST,profile);
                world.setBlockState(new BlockPos(ox+p*2,64,oz),block.getDefaultState().withProperty(FrameBlock.PANEL,finish*2),2);
            }
        }
        world.setSpawnPoint(new BlockPos(7,64,-9));
    }
    private void sign(World world,BlockPos pos,String label) {
        world.setBlockState(pos,Blocks.STANDING_SIGN.getDefaultState(),2);
        TileEntitySign sign=(TileEntitySign)world.getTileEntity(pos);
        sign.signText[0]=new TextComponentString(label);sign.signText[1]=new TextComponentString("Iron Age Architecture");
        sign.markDirty();
    }
    private void house(World world,int ox,String wood,double[] offsets,boolean twoSides,int finish) {
        int top=64;
        for(int storey=0;storey<offsets.length;storey++) {
            double offset=offsets[storey],minX=ox-(twoSides?offset:0),maxX=ox+9,minZ=-offset,maxZ=9;
            int base=64+storey*5;top=base+4;
            for(int x=(int)Math.floor(minX);x<Math.ceil(maxX);x++) for(int z=(int)Math.floor(minZ);z<Math.ceil(maxZ);z++) {
                int room=0,wall=0;
                for(int q=0;q<4;q++) {
                    double px=x+(q%2)*.5+.25,pz=z+(q/2)*.5+.25;
                    if(px>=minX&&px<maxX&&pz>=minZ&&pz<maxZ) {
                        room|=1<<q;
                        if(px<minX+.5||px>=maxX-.5||pz<minZ+.5||pz>=maxZ-.5) wall|=1<<q;
                    }
                }
                if(room!=0) placeFloor(world,new BlockPos(x,base-1,z),wood,room,storey>0&&z==Math.floor(minZ)&&Math.floorMod(x-ox,3)==1,
                    storey>0&&offsets[storey]-offsets[storey-1]>.5);
                if(wall==0) continue;
                boolean corner=(x==Math.floor(minX)||x==Math.ceil(maxX)-1)&&(z==Math.floor(minZ)||z==Math.ceil(maxZ)-1);
                for(int y=0;y<4;y++) {
                    boolean front=z==Math.floor(minZ),window=front&&x>=ox+2&&x<=ox+3&&(y==1||y==2);
                    if(window) continue;
                    FrameFamily family=y==0||y==3?FrameFamily.RAIL:Math.floorMod(front?x-ox:z,3)==1?FrameFamily.POST:
                        storey==1&&wood.equals("spruce")?FrameFamily.STUD:FrameFamily.INFILL;
                    int pattern=y==3?1:family==FrameFamily.STUD?1:0;
                    if(front&&!wood.equals("spruce")&&(x==ox+5&&y==1||x==ox+6&&y==2)) {family=FrameFamily.BRACE;pattern=0;}
                    placeWall(world,new BlockPos(x,base+y,z),wood,wall,corner,family,finish,pattern,minX,maxX,minZ,maxZ);
                }
                if(storey==offsets.length-1&&(z==Math.floor(minZ)||z==Math.ceil(maxZ)-1)) {
                    int rise=Math.min(x-((int)Math.floor(minX)-1),ox+9-x);
                    for(int y=0;y<rise;y++) placeWall(world,new BlockPos(x,top+y,z),wood,wall,corner,FrameFamily.INFILL,finish,0,minX,maxX,minZ,maxZ);
                }
            }
        }
        // Existing slate roof, with a deliberate eave above the half-grid facade.
        double last=offsets[offsets.length-1];int minX=(int)Math.floor(ox-(twoSides?last:0)),minZ=(int)Math.floor(-last)-1;
        int width=ox+9-minX;
        RoofBlock roof=(RoofBlock)net.minecraft.block.Block.getBlockFromName("ironagearchitecture:slate_roof");
        RoofBlock ridge=(RoofBlock)net.minecraft.block.Block.getBlockFromName("ironagearchitecture:slate_ridge");
        int count=ox+9-(minX-1)+1,centre=(minX-1)+count/2;
        for(int x=minX-1;x<=ox+9;x++) for(int z=minZ;z<=10;z++) {
            int step=Math.min(x-(minX-1),ox+9-x);
            world.setBlockState(new BlockPos(x,top+step,z),(count%2==1&&x==centre?ridge:roof).getDefaultState().withProperty(FrameBlock.FACING,
                count%2==1&&x==centre?EnumFacing.NORTH:x<centre?EnumFacing.EAST:EnumFacing.WEST),2);
        }
    }
    private void placeFloor(World world,BlockPos pos,String wood,int mask,boolean bracket,boolean fullReach) {
        for(FloorShape shape:FloorShape.values()) for(EnumFacing facing:EnumFacing.HORIZONTALS) if(shape.rotatedMask(facing)==mask) {
            JettyFloorBlock block=ArchitectureBlocks.findFloor(wood,shape,null);
            EnumFacing local=JettyFloorBlock.unrotate(EnumFacing.NORTH,facing);
            int panel=local.getAxis()==EnumFacing.Axis.X?1:0;
            if(bracket&&JettyFloorGeometry.bracketAnchor(shape,local,fullReach)!=null) {
                block=ArchitectureBlocks.findFloor(wood,shape,local);if(fullReach) panel|=2;
            }
            world.setBlockState(pos,block.getDefaultState().withProperty(FrameBlock.FACING,facing).withProperty(FrameBlock.PANEL,panel),2);return;
        }
        throw new IllegalStateException("Unrepresented floor footprint "+mask);
    }
    private void placeWall(World world,BlockPos pos,String wood,int mask,boolean corner,FrameFamily family,int finish,int pattern,
        double minX,double maxX,double minZ,double maxZ) {
        if(corner) {
            double outerX=(pos.getX()<minX+.5?minX:maxX)-pos.getX(),outerZ=(pos.getZ()<minZ+.5?minZ:maxZ)-pos.getZ();
            for(WallProfile profile:WallProfile.values()) if(profile.corner) for(EnumFacing facing:EnumFacing.HORIZONTALS) {
                if(footprint(profile,facing)!=mask) continue;
                double[] anchor=WallGeometry.rotate(profile.x0,0,profile.z0,facing);
                if(Math.abs(anchor[0]-outerX*16)>.001||Math.abs(anchor[2]-outerZ*16)>.001) continue;
                PartialWallBlock block=ArchitectureBlocks.findPartial(wood,FrameFamily.TERMINAL,profile);
                world.setBlockState(pos,block.getDefaultState().withProperty(FrameBlock.FACING,facing).withProperty(FrameBlock.PANEL,finish*2),2);return;
            }
        }
        for(WallProfile profile:new WallProfile[]{WallProfile.THIN,WallProfile.SHORT_LEFT,WallProfile.SHORT_RIGHT}) for(EnumFacing facing:EnumFacing.HORIZONTALS) {
            if(footprint(profile,facing)!=mask) continue;
            PartialWallBlock block=ArchitectureBlocks.findPartial(family==FrameFamily.INFILL?null:wood,family,profile);
            int panel=family==FrameFamily.INFILL||family==FrameFamily.TERMINAL?finish:finish*2+pattern;
            world.setBlockState(pos,block.getDefaultState().withProperty(FrameBlock.FACING,facing).withProperty(FrameBlock.PANEL,panel),2);return;
        }
        throw new IllegalStateException("Unrepresented wall footprint "+mask+" at "+pos);
    }
    public static int footprint(WallProfile profile,EnumFacing facing) {
        int result=0;
        for(int q=0;q<4;q++) if(profile.contains(q%2*8+4,q/2*8+4)) {
            double[] p=WallGeometry.rotate(q%2*8+4,0,q/2*8+4,facing);
            result|=1<<((p[0]<8?0:1)+(p[2]<8?0:2));
        }
        return result;
    }
}
