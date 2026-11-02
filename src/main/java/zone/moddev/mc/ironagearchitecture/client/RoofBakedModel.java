package zone.moddev.mc.ironagearchitecture.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import zone.moddev.mc.ironagearchitecture.block.RoofBlock;
import zone.moddev.mc.ironagearchitecture.block.RoofGeometry;
import zone.moddev.mc.ironagearchitecture.block.RoofShape;

/** Immutable smooth roof mesh. Triangles are represented as degenerate quads. */
public final class RoofBakedModel implements IBakedModel {
    private final List<BakedQuad> quads = new ArrayList<>();
    private final TextureAtlasSprite sprite;
    private final IBakedModel base;
    public RoofBakedModel(IBakedModel base, RoofBlock block, IBlockState state, TextureAtlasSprite sprite) {
        this.base = base; this.sprite = sprite;
        RoofShape shape = block.geometryShape(state);
        EnumFacing facing = state.getValue(BlockHorizontal.FACING);
        for (double[][] face : RoofGeometry.top(shape,block.isRidge())) add(face,facing,true);
        add(new double[][]{{0,0,0},{1,0,0},{1,0,1},{0,0,1}},facing,true);
        // Split perimeter edges at the ridge apex. Zero-area sides are omitted.
        for (int side = 0; side < 4; side++) {
            if (block.isRidge() && ((side == 0 && !block.capNorth(state)) || (side == 2 && !block.capSouth(state)))) continue;
            double[][] corners = {{0,0},{1,0},{1,1},{0,1}};
            double[] a = corners[side], b = corners[(side + 1) % 4];
            for (int segment = 0; segment < 2; segment++) {
                double t = segment / 2.0, u = (segment + 1) / 2.0;
                double ax = a[0] + (b[0]-a[0])*t, az = a[1] + (b[1]-a[1])*t;
                double bx = a[0] + (b[0]-a[0])*u, bz = a[1] + (b[1]-a[1])*u;
                double ah = RoofGeometry.height(shape,block.isRidge(),ax,az), bh = RoofGeometry.height(shape,block.isRidge(),bx,bz);
                if (ah + bh > 0) add(new double[][]{{ax,0,az},{ax,ah,az},{bx,bh,bz},{bx,0,bz}},facing,false);
            }
        }
    }
    private void add(double[][] points, EnumFacing facing, boolean top) {
        if (java.util.Arrays.equals(points[0],points[1]))
            points = new double[][]{points[0],points[2],points[3],points[3]};
        double[][] rotated = new double[4][];
        for (int i = 0; i < 4; i++) rotated[i] = RoofGeometry.rotate(points[i][0],points[i][1],points[i][2],facing);
        double[] a = rotated[0], b = rotated[1], c = rotated[2];
        double nx = (b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]);
        double ny = (b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]);
        double nz = (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
        double length = Math.sqrt(nx*nx+ny*ny+nz*nz);
        if (length < 1e-8) return;
        VertexFormat format = DefaultVertexFormats.ITEM;
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
        builder.setTexture(sprite);
        builder.setQuadOrientation(EnumFacing.getFacingFromVector((float)nx,(float)ny,(float)nz));
        builder.setApplyDiffuseLighting(true);
        for (int vertex = 0; vertex < 4; vertex++) {
            double[] p = rotated[vertex], source = points[vertex];
            boolean alongX = Math.abs(points[0][0]-points[2][0]) > 1e-8;
            float textureU = (float)((top || alongX ? source[0] : source[2]) * 16);
            float textureV = (float)(top ? source[2] * 16 : (1-source[1]) * 16);
            for (int element = 0; element < format.getElementCount(); element++) {
                switch (format.getElement(element).getUsage()) {
                    case POSITION: builder.put(element,(float)p[0],(float)p[1],(float)p[2],1); break;
                    case COLOR: builder.put(element,1,1,1,1); break;
                    case UV: builder.put(element,sprite.getInterpolatedU(textureU),sprite.getInterpolatedV(textureV),0,1); break;
                    case NORMAL: builder.put(element,(float)(nx/length),(float)(ny/length),(float)(nz/length),0); break;
                    default: builder.put(element); break;
                }
            }
        }
        quads.add(builder.build());
    }
    @Override public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long seed) { return side == null ? quads : Collections.emptyList(); }
    @Override public boolean isAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean isBuiltInRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleTexture() { return sprite; }
    @Override public ItemCameraTransforms getItemCameraTransforms() { return base.getItemCameraTransforms(); }
    @Override public ItemOverrideList getOverrides() { return ItemOverrideList.NONE; }
}
