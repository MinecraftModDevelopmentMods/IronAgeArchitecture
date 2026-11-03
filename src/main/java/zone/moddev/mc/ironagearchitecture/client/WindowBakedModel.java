package zone.moddev.mc.ironagearchitecture.client;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.*;
import net.minecraft.util.*;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.property.IExtendedBlockState;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Both cells render only their own clipped portion, including on opposite sides of a chunk border. */
public final class WindowBakedModel implements IBakedModel {
    private final IBakedModel base;private final IBlockState fallback;private final Boolean pane;
    private final Map<String,List<BakedQuad>> cache=new HashMap<>();
    public WindowBakedModel(IBakedModel base,IBlockState state){this.base=base;fallback=state;pane=null;}
    public WindowBakedModel(IBakedModel base,boolean diamond){this.base=base;fallback=null;pane=diamond;}
    private TextureAtlasSprite sprite(String name){return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(name);}
    @Override public synchronized List<BakedQuad> getQuads(@Nullable IBlockState state,@Nullable EnumFacing side,long seed) {
        if(side!=null) return Collections.emptyList();BlockRenderLayer layer=MinecraftForgeClient.getRenderLayer();
        WindowView view=state instanceof IExtendedBlockState?((IExtendedBlockState)state).getValue(WindowView.PROPERTY):null;
        if(view==null&&fallback!=null&&fallback.getBlock() instanceof WindowBlock) view=new WindowView(fallback,0,false);
        if(view==null&&pane==null) return Collections.emptyList();
        String key=(pane!=null?"pane"+pane:view.state.withProperty(WindowBlock.POWERED,false)+":"+view.joins+":"+view.proxy)+":"+layer;List<BakedQuad> found=cache.get(key);if(found!=null) return found;
        WindowBlock block=view==null?null:(WindowBlock)view.state.getBlock();
        List<WindowGeometry.Face> faces=pane!=null?WindowGeometry.paneFaces(pane):WindowGeometry.faces(block.thin,block.diamond,block.right,view.state.getValue(WindowBlock.OPEN),view.state.getValue(WindowBlock.FACING),view.joins,view.proxy,block.back);
        List<BakedQuad> quads=new ArrayList<>();
        for(WindowGeometry.Face face:faces) {
            boolean glass=face.material==WindowGeometry.Material.GLASS;
            if(layer!=null&&(glass?layer!=BlockRenderLayer.TRANSLUCENT:layer!=BlockRenderLayer.SOLID)) continue;
            double[][] p=face.vertices;
            if(p.length==4) add(quads,p,face.material,block);
            else for(int i=1;i<p.length-1;i++) add(quads,new double[][]{p[0],p[i],p[i+1],p[i+1]},face.material,block);
        }
        found=Collections.unmodifiableList(quads);if(cache.size()>128) cache.clear();cache.put(key,found);return found;
    }
    private void add(List<BakedQuad> out,double[][] original,WindowGeometry.Material material,WindowBlock block) {
        double[][] p=new double[4][3];for(int v=0;v<4;v++) for(int a=0;a<3;a++) p[v][a]=(float)(original[v][a]/16);
        double nx=0,ny=0,nz=0,length=0;
        for(int i=1;i<3&&length<1e-9;i++) {double[] a=p[0],b=p[i],c=p[i+1];
            nx=(b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]);ny=(b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]);nz=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        }
        if(length<1e-9) return;nx/=length;ny/=length;nz/=length;
        String wood=block==null?"oak":block.wood;
        TextureAtlasSprite texture=sprite(material==WindowGeometry.Material.GLASS?"minecraft:blocks/glass_silver":material==WindowGeometry.Material.LEAD?"minecraft:blocks/iron_block":material==WindowGeometry.Material.DAUB?"ironagearchitecture:blocks/"+(block.finish==0?"cob":"limewashed_cob"):"ironagearchitecture:blocks/beam_"+wood);
        VertexFormat format=DefaultVertexFormats.ITEM;UnpackedBakedQuad.Builder builder=new UnpackedBakedQuad.Builder(format);
        builder.setTexture(texture);builder.setApplyDiffuseLighting(material!=WindowGeometry.Material.GLASS);builder.setQuadOrientation(EnumFacing.getFacingFromVector((float)nx,(float)ny,(float)nz));
        float color=material==WindowGeometry.Material.LEAD?.22F:1;
        for(int v=0;v<4;v++) for(int e=0;e<format.getElementCount();e++) switch(format.getElement(e).getUsage()) {
            case POSITION:builder.put(e,(float)p[v][0],(float)p[v][1],(float)p[v][2],1);break;
            case NORMAL:builder.put(e,(float)nx,(float)ny,(float)nz,0);break;
            case COLOR:builder.put(e,color,color,color,1);break;
            case UV:builder.put(e,texture.getInterpolatedU(.008+.999*Math.max(0,Math.min(16,original[v][3]))),texture.getInterpolatedV(.008+.999*Math.max(0,Math.min(16,original[v][4]))),0,1);break;
            default:builder.put(e);break;
        }
        out.add(builder.build());
    }
    @Override public boolean isAmbientOcclusion(){return false;}
    @Override public boolean isGui3d(){return true;}
    @Override public boolean isBuiltInRenderer(){return false;}
    @Override public TextureAtlasSprite getParticleTexture(){return sprite("minecraft:blocks/glass_silver");}
    @Override public ItemCameraTransforms getItemCameraTransforms(){return base.getItemCameraTransforms();}
    @Override public ItemOverrideList getOverrides(){return ItemOverrideList.NONE;}
}
