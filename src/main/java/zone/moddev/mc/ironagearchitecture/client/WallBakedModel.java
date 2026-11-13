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
import zone.moddev.mc.ironagearchitecture.block.SmartWallBlock;
import zone.moddev.mc.ironagearchitecture.block.JettyFloorBlock;
import zone.moddev.mc.ironagearchitecture.block.SurfaceGeometry;
import zone.moddev.mc.ironagearchitecture.block.TimberCorners;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.Grain;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.Part;

/** Shared clipped timber meshes; joins need no tile entities or saved connection bits. */
public final class WallBakedModel implements IBakedModel {
    private final IBakedModel base;
    private final FrameBlock block;
    private final IBlockState state;
    private final TextureAtlasSprite daub,wood,end,boards;
    private final Map<Long,List<BakedQuad>> cache=new ConcurrentHashMap<>();
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
        Integer patch=extended instanceof IExtendedBlockState?((IExtendedBlockState)extended).getValue(TimberCorners.PROPERTY):null;
        if(block instanceof SmartWallBlock) {
            patch=mask;
            mask=extended instanceof IExtendedBlockState?((IExtendedBlockState)extended).getValue(SmartWallBlock.LAYOUT):0;
        }
        int joins=mask==null?0:mask,corners=patch==null?0:patch;long key=(joins&0xffffffffL)|((long)corners<<32);
        List<BakedQuad> found=cache.get(key);
        if(found!=null) return found;
        List<BakedQuad> quads=new ArrayList<>();
        for(SurfaceGeometry.Face face:SurfaceGeometry.faces(block.parts(state,joins,corners),(block instanceof SmartWallBlock?EnumFacing.NORTH:state.getValue(FrameBlock.FACING)))) {
            double[][] p=face.points;
            if(p.length==4) add(quads,face.part,p);
            else for(int i=1;i<p.length-1;i++) add(quads,face.part,new double[][]{p[0],p[i],p[i+1],p[i+1]});
        }
        found=Collections.unmodifiableList(quads);
        if(cache.size()>128) cache.clear();
        cache.put(key,found);
        return found;
    }
    private void add(List<BakedQuad> quads,Part part,double[][] points) {
        EnumFacing facing=block instanceof SmartWallBlock?EnumFacing.NORTH:state.getValue(BlockHorizontal.FACING);
        double[][] positions=new double[4][3];
        for(int v=0;v<4;v++) {
            double[] point=points[part.horizontal?3-v:v];
            double[] transformed=part.transform(point[0],point[1],point[2],facing);
            for(int axis=0;axis<3;axis++) positions[v][axis]=(float)(transformed[axis]/16);
        }
        // Clipping can leave slivers narrower than a render vertex's float precision.
        // Discard them after conversion, rather than emitting collapsed triangles at joins.
        double area=0;
        for(int i=1;i<3;i++) {
            double[] a=positions[0],b=positions[i],c=positions[i+1];
            double x=(b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]);
            double y=(b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]);
            double z=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
            area+=x*x+y*y+z*z;
        }
        if(area<1e-18) return;
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
        if(part.timber!=null) texture=sprite("ironagearchitecture:blocks/beam_"+part.timber);
        double[] origin=part.transform(0,0,0,facing),normalPoint=part.transform(nx,ny,nz,facing);
        float rx=(float)(normalPoint[0]-origin[0]),ry=(float)(normalPoint[1]-origin[1]),rz=(float)(normalPoint[2]-origin[2]);
        VertexFormat format=DefaultVertexFormats.ITEM;
        UnpackedBakedQuad.Builder builder=new UnpackedBakedQuad.Builder(format);
        builder.setTexture(texture);builder.setApplyDiffuseLighting(true);
        builder.setQuadOrientation(EnumFacing.getFacingFromVector(rx,ry,rz));
        for(int vertex=0;vertex<points.length;vertex++) {
            // Horizontal ties exchange local Y/Z; reverse winding to keep their outside faces.
            double[] point=points[part.horizontal?points.length-1-vertex:vertex];
            double[] p=positions[vertex];
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
                case POSITION:builder.put(element,(float)p[0],(float)p[1],(float)p[2],1);break;
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
