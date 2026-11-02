package zone.moddev.mc.ironagearchitecture.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.property.IExtendedBlockState;
import zone.moddev.mc.ironagearchitecture.block.FrameBlock;
import zone.moddev.mc.ironagearchitecture.block.JettyFloorBlock;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.Grain;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.Part;

/** Shared clipped timber meshes; joins need no tile entities or saved connection bits. */
public final class WallBakedModel implements IBakedModel {
    private final IBakedModel base;
    private final FrameBlock block;
    private final IBlockState state;
    private final TextureAtlasSprite daub,wood,end,boards;
    private final Map<Integer,List<BakedQuad>> cache=new ConcurrentHashMap<>();
    public WallBakedModel(IBakedModel base,FrameBlock block,IBlockState state) {
        this.base=base;this.block=block;this.state=state;
        daub=sprite("ironagearchitecture:blocks/"+(block.finish(state.getValue(FrameBlock.PANEL))==0?"cob":"limewashed_cob"));
        wood=sprite("ironagearchitecture:blocks/beam_"+(block.wood==null?"oak":block.wood));
        end=sprite("minecraft:blocks/log_"+("dark_oak".equals(block.wood)?"big_oak":block.wood==null?"oak":block.wood)+"_top");
        boards=sprite("minecraft:blocks/planks_"+("dark_oak".equals(block.wood)?"big_oak":block.wood==null?"oak":block.wood));
    }
    private TextureAtlasSprite sprite(String location) { return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location); }
    @Override public List<BakedQuad> getQuads(@Nullable IBlockState extended,@Nullable EnumFacing side,long seed) {
        if(side!=null) return Collections.emptyList();
        Integer mask=extended instanceof IExtendedBlockState?((IExtendedBlockState)extended).getValue(FrameBlock.CONNECTIONS):null;
        int key=mask==null?0:mask;
        List<BakedQuad> found=cache.get(key);
        if(found!=null) return found;
        List<BakedQuad> quads=new ArrayList<>();
        for(Part part:block.parts(state,key)) {
            int n=part.outline.length;
            if(n==4) {
                add(quads,part,new double[][]{point(part,0,false),point(part,3,false),point(part,2,false),point(part,1,false)});
                add(quads,part,new double[][]{point(part,0,true),point(part,1,true),point(part,2,true),point(part,3,true)});
            } else for(int i=1;i<n-1;i++) {
                add(quads,part,new double[][]{point(part,0,false),point(part,i+1,false),point(part,i,false),point(part,i,false)});
                add(quads,part,new double[][]{point(part,0,true),point(part,i,true),point(part,i+1,true),point(part,i+1,true)});
            }
            for(int i=0;i<n;i++) {
                int j=(i+1)%n;
                add(quads,part,new double[][]{point(part,i,false),point(part,j,false),point(part,j,true),point(part,i,true)});
            }
        }
        found=Collections.unmodifiableList(quads);
        if(cache.size()>128) cache.clear();
        cache.put(key,found);
        return found;
    }
    private static double[] point(Part part,int i,boolean back) { return new double[]{part.outline[i][0],part.outline[i][1],back?part.back:part.front}; }
    private void add(List<BakedQuad> quads,Part part,double[][] points) {
        double[] a=points[0],b=points[1],c=points[2];
        double nx=(b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]);
        double ny=(b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]);
        double nz=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
        double length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(length<1e-9) return;
        nx/=length;ny/=length;nz/=length;
        boolean alongEnd=part.grain==Grain.VERTICAL&&Math.abs(ny)>.9
                ||part.grain==Grain.EAST_WEST&&Math.abs(nx)>.9
                ||part.grain==Grain.NORTH_SOUTH&&Math.abs(nz)>.9;
        TextureAtlasSprite texture=part.grain==Grain.DAUB?daub:part.grain==Grain.BOARDS_X||part.grain==Grain.BOARDS_Z?boards:alongEnd?end:wood;
        EnumFacing facing=state.getValue(BlockHorizontal.FACING);
        double[] origin=part.transform(0,0,0,facing),normalPoint=part.transform(nx,ny,nz,facing);
        float rx=(float)(normalPoint[0]-origin[0]),ry=(float)(normalPoint[1]-origin[1]),rz=(float)(normalPoint[2]-origin[2]);
        VertexFormat format=DefaultVertexFormats.ITEM;
        UnpackedBakedQuad.Builder builder=new UnpackedBakedQuad.Builder(format);
        builder.setTexture(texture);builder.setApplyDiffuseLighting(true);
        builder.setQuadOrientation(EnumFacing.getFacingFromVector(rx,ry,rz));
        for(double[] point:points) {
            double[] p=part.transform(point[0],point[1],point[2],facing);
            double u,v;
            if(alongEnd) {u=Math.abs(ny)>.9?point[0]:Math.abs(nx)>.9?point[2]:point[0];v=Math.abs(ny)>.9?point[2]:16-point[1];}
            else if(part.grain==Grain.RISING||part.grain==Grain.FALLING) {
                double x=part.grain==Grain.RISING?point[0]:16-point[0];
                u=8+(x-point[1])/2;v=(x+point[1])/2;
            } else if(part.grain==Grain.BOARDS_X||part.grain==Grain.BOARDS_Z) {
                if(Math.abs(ny)>.9) {u=part.grain==Grain.BOARDS_X?point[2]:point[0];v=part.grain==Grain.BOARDS_X?point[0]:point[2];}
                else {u=Math.abs(nx)>.9?point[2]:point[0];v=16-point[1];}
            }
            else if(part.grain==Grain.EAST_WEST) {u=Math.abs(ny)>.9?point[2]:point[1];v=point[0];}
            else if(part.grain==Grain.NORTH_SOUTH) {u=Math.abs(ny)>.9?point[0]:point[1];v=point[2];}
            else if(Math.abs(ny)>.9) {u=point[0];v=point[2];}
            else {u=Math.abs(nx)>.9?point[2]:point[0];v=16-point[1];}
            // Keep edge faces inside their sprite, including at oblique mipmapped floor edges.
            float tu=texture.getInterpolatedU(.008+.999*Math.max(0,Math.min(16,u))),tv=texture.getInterpolatedV(.008+.999*Math.max(0,Math.min(16,v)));
            for(int element=0;element<format.getElementCount();element++) switch(format.getElement(element).getUsage()) {
                case POSITION:builder.put(element,(float)(p[0]/16),(float)(p[1]/16),(float)(p[2]/16),1);break;
                case COLOR:builder.put(element,1,1,1,1);break;
                case UV:builder.put(element,tu,tv,0,1);break;
                case NORMAL:builder.put(element,rx,ry,rz,0);break;
                default:builder.put(element);break;
            }
        }
        quads.add(builder.build());
    }
    @Override public boolean isAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean isBuiltInRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleTexture() { return block instanceof JettyFloorBlock?boards:daub; }
    @Override public ItemCameraTransforms getItemCameraTransforms() { return base.getItemCameraTransforms(); }
    @Override public ItemOverrideList getOverrides() { return ItemOverrideList.NONE; }
}
