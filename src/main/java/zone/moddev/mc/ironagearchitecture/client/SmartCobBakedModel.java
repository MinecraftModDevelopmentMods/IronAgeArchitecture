package zone.moddev.mc.ironagearchitecture.client;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.*;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.property.IExtendedBlockState;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Cached chunk meshes with smooth side normals and contour-distance texture coordinates. */
public final class SmartCobBakedModel implements IBakedModel {
    private final IBakedModel base;private final TextureAtlasSprite texture;
    private final Map<SmartCobView,List<BakedQuad>> cache=new ConcurrentHashMap<>();
    private static final SmartCobProfile ITEM=SmartCobGeometry.solve(Collections.singleton(SmartCobGeometry.key(0,0)),new java.awt.geom.Area()).profiles.get(SmartCobGeometry.key(0,0));
    public SmartCobBakedModel(IBakedModel base,int finish){this.base=base;texture=Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("ironagearchitecture:blocks/smart_cob_"+(finish==0?"raw":"limewashed"));}
    @Override public List<BakedQuad> getQuads(@Nullable IBlockState state,@Nullable EnumFacing side,long seed) {
        if(side!=null)return Collections.emptyList();
        SmartCobView view=state instanceof IExtendedBlockState?((IExtendedBlockState)state).getValue(SmartCobBlock.VIEW):null;
        if(view==null)view=new SmartCobView(ITEM,null,null,false,false);
        List<BakedQuad> found=cache.get(view);if(found!=null)return found;List<BakedQuad> result=new ArrayList<>();
        for(SmartCobMesh.Face face:SmartCobMesh.faces(view))add(result,face);
        found=Collections.unmodifiableList(result);if(cache.size()>256)cache.clear();cache.put(view,found);return found;
    }
    private void add(List<BakedQuad> result,SmartCobMesh.Face face) {
        double total=0;
        for(int i=1;i<3;i++){double[] a=face.points[0],b=face.points[i],c=face.points[i+1];double x=(b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]),y=(b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]),z=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);total+=x*x+y*y+z*z;}
        if(total<1e-18)return;
        VertexFormat format=DefaultVertexFormats.ITEM;UnpackedBakedQuad.Builder builder=new UnpackedBakedQuad.Builder(format);
        builder.setTexture(texture);builder.setApplyDiffuseLighting(false);double[] n=face.normals[0];builder.setQuadOrientation(EnumFacing.getFacingFromVector((float)n[0],(float)n[1],(float)n[2]));
        for(int v=0;v<4;v++) {
            double[] p=face.points[v],normal=face.normals[v];float shade=(float)(.6*normal[0]*normal[0]+(normal[1]>0?1:.5)*normal[1]*normal[1]+.8*normal[2]*normal[2]);
            float u=texture.getInterpolatedU(.008+15.984*Math.max(0,Math.min(1,face.uv[v][0]))),t=texture.getInterpolatedV(.008+15.984*Math.max(0,Math.min(1,face.uv[v][1])));
            for(int e=0;e<format.getElementCount();e++)switch(format.getElement(e).getUsage()) {
                case POSITION:builder.put(e,(float)p[0],(float)p[1],(float)p[2],1);break;
                case COLOR:builder.put(e,shade,shade,shade,1);break;
                case UV:builder.put(e,u,t,0,1);break;
                case NORMAL:builder.put(e,(float)normal[0],(float)normal[1],(float)normal[2],0);break;
                default:builder.put(e);break;
            }
        }
        result.add(builder.build());
    }
    @Override public boolean isAmbientOcclusion(){return false;}
    @Override public boolean isGui3d(){return true;}
    @Override public boolean isBuiltInRenderer(){return false;}
    @Override public TextureAtlasSprite getParticleTexture(){return texture;}
    @Override public ItemCameraTransforms getItemCameraTransforms(){return base.getItemCameraTransforms();}
    @Override public ItemOverrideList getOverrides(){return ItemOverrideList.NONE;}
}
