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
import net.minecraftforge.common.property.IExtendedBlockState;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.roof.*;

/** Chunk-baked fixed primitives. The cache key has no world position or roof identity. */
public final class RoofFrameBakedModel implements IBakedModel {
    private final IBakedModel base;
    private static final Map<List<Object>,List<BakedQuad>> CACHE=Collections.synchronizedMap(new LinkedHashMap<List<Object>,List<BakedQuad>>(256,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<List<Object>,List<BakedQuad>> e){return size()>4096;}});
    public RoofFrameBakedModel(IBakedModel base){this.base=base;}
    public static void clear(){CACHE.clear();}
    @Override public List<BakedQuad> getQuads(@Nullable IBlockState s,@Nullable EnumFacing side,long seed){if(side!=null||!(s instanceof IExtendedBlockState))return Collections.emptyList();RoofCell cell=((IExtendedBlockState)s).getValue(RoofFrameBlock.CELL);if(cell==null)return Collections.emptyList();int wood=s.getValue(RoofFrameBlock.WOOD),cover=RoofFrameBlock.cover(s);List<Object> key=Arrays.asList(new RoofMeshes.Key(cell,cover),wood);List<BakedQuad> cached=CACHE.get(key);if(cached!=null)return cached;List<BakedQuad> quads=new ArrayList<>();TextureAtlasSprite timber=sprite("beam_"+ArchitectureBlocks.WOODS[wood]),skin=sprite(cover==1?"smart_thatch":cover==2?"slate":"clay_tile"),edge=sprite(cover==1?"smart_thatch_edge":cover==2?"slate":"clay_tile");for(RoofMeshes.Face f:RoofMeshes.get(cell,cover).faces){TextureAtlasSprite texture=f.skin?(Math.abs(f.normal[1])>.1?skin:edge):timber;VertexFormat format=DefaultVertexFormats.ITEM;UnpackedBakedQuad.Builder b=new UnpackedBakedQuad.Builder(format);b.setTexture(texture);b.setApplyDiffuseLighting(false);double[] n=f.normal;float shade=(float)(.6*n[0]*n[0]+(n[1]>0?1:.5)*n[1]*n[1]+.8*n[2]*n[2]);b.setQuadOrientation(EnumFacing.getFacingFromVector((float)n[0],(float)n[1],(float)n[2]));for(int i=0;i<4;i++){double[] p=f.vertices[Math.min(i,f.vertices.length-1)];double u=Math.abs(n[0])>.8?p[2]:p[0],v=Math.abs(n[1])>.1?p[2]:p[1];float tu=texture.getInterpolatedU(.008+15.984*u),tv=texture.getInterpolatedV(.008+15.984*v);for(int e=0;e<format.getElementCount();e++)switch(format.getElement(e).getUsage()){case POSITION:b.put(e,(float)p[0],(float)p[1],(float)p[2],1);break;case COLOR:b.put(e,shade,shade,shade,1);break;case NORMAL:b.put(e,(float)n[0],(float)n[1],(float)n[2],0);break;case UV:b.put(e,tu,tv,0,1);break;default:b.put(e);break;}}quads.add(b.build());}cached=Collections.unmodifiableList(quads);CACHE.put(key,cached);return cached;}
    private static TextureAtlasSprite sprite(String name){return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("ironagearchitecture:blocks/"+name);}
    public boolean isAmbientOcclusion(){return false;}public boolean isGui3d(){return true;}public boolean isBuiltInRenderer(){return false;}public TextureAtlasSprite getParticleTexture(){return sprite("beam_oak");}public ItemCameraTransforms getItemCameraTransforms(){return base.getItemCameraTransforms();}public ItemOverrideList getOverrides(){return ItemOverrideList.NONE;}
}
