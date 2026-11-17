package zone.moddev.mc.ironagearchitecture.client;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.*;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Each of the finite roof states is baked once. No world reads or per-house meshes. */
public final class SmartThatchBakedModel implements IBakedModel {
    private final IBakedModel base;private final TextureAtlasSprite surface;private final List<BakedQuad> quads;
    public SmartThatchBakedModel(IBakedModel base,IBlockState state,boolean helper){this.base=base;surface=atlas("smart_thatch");List<BakedQuad> out=new ArrayList<>();{TextureAtlasSprite edge=atlas("smart_thatch_edge");ThatchShape shape=ThatchShape.of(state.getValue(SmartThatchBlock.PROFILE),state.getValue(SmartThatchBlock.FACING));for(ThatchGeometry.Face face:(helper?(state.getValue(SmartThatchBlock.LINKED)?ThatchGeometry.underside(shape):ThatchGeometry.EMPTY):ThatchGeometry.mesh(shape)).faces)add(out,face,face.surface||face.normal[1]<-.1?surface:edge);}quads=Collections.unmodifiableList(out);}
    private static TextureAtlasSprite atlas(String name){return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("ironagearchitecture:blocks/"+name);}
    private static void add(List<BakedQuad> out,ThatchGeometry.Face face,TextureAtlasSprite sprite){VertexFormat format=DefaultVertexFormats.ITEM;UnpackedBakedQuad.Builder builder=new UnpackedBakedQuad.Builder(format);builder.setTexture(sprite);builder.setApplyDiffuseLighting(false);double[] n=face.normal;builder.setQuadOrientation(EnumFacing.getFacingFromVector((float)n[0],(float)n[1],(float)n[2]));float shade=(float)(.6*n[0]*n[0]+(n[1]>0?1:.5)*n[1]*n[1]+.8*n[2]*n[2]);for(int i=0;i<4;i++){double[] p=face.vertices[Math.min(i,face.vertices.length-1)];boolean acrossX=Math.abs(n[0])>Math.abs(n[2]);double u=acrossX?p[2]:p[0],v=(face.surface||n[1]<-.1)?(acrossX?p[0]:p[2]):Math.abs(n[1])>.5?p[2]:p[1];float tu=sprite.getInterpolatedU(.008+15.984*u),tv=sprite.getInterpolatedV(.008+15.984*v);for(int e=0;e<format.getElementCount();e++)switch(format.getElement(e).getUsage()){case POSITION:builder.put(e,(float)p[0],(float)p[1],(float)p[2],1);break;case COLOR:builder.put(e,shade,shade,shade,1);break;case UV:builder.put(e,tu,tv,0,1);break;case NORMAL:builder.put(e,(float)n[0],(float)n[1],(float)n[2],0);break;default:builder.put(e);break;}}out.add(builder.build());}
    @Override public List<BakedQuad> getQuads(@Nullable IBlockState state,@Nullable EnumFacing side,long seed){return side==null?quads:Collections.emptyList();}
    @Override public boolean isAmbientOcclusion(){return false;}@Override public boolean isGui3d(){return true;}@Override public boolean isBuiltInRenderer(){return false;}@Override public TextureAtlasSprite getParticleTexture(){return surface;}@Override public ItemCameraTransforms getItemCameraTransforms(){return base.getItemCameraTransforms();}@Override public ItemOverrideList getOverrides(){return ItemOverrideList.NONE;}
}
