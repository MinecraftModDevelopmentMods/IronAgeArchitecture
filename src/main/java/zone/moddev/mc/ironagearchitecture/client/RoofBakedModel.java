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
import zone.moddev.mc.ironagearchitecture.block.DownpipeBlock;
import zone.moddev.mc.ironagearchitecture.block.FasciaBlock;
import zone.moddev.mc.ironagearchitecture.block.RoofBlock;
import zone.moddev.mc.ironagearchitecture.block.RoofGeometry;
import zone.moddev.mc.ironagearchitecture.block.RoofShape;
import zone.moddev.mc.ironagearchitecture.block.RoofTrimGeometry;
import zone.moddev.mc.ironagearchitecture.block.TrimmedRoofBlock;

/** Immutable smooth roof mesh. Triangles are represented as degenerate quads. */
public final class RoofBakedModel implements IBakedModel {
    private final List<BakedQuad> quads = new ArrayList<>();
    private final TextureAtlasSprite sprite;
    private final IBakedModel base;
    public RoofBakedModel(IBakedModel base, RoofBlock block, IBlockState state, TextureAtlasSprite sprite) {
        this(base, sprite);
        RoofShape shape = block.geometryShape(state);
        EnumFacing facing = state.getValue(BlockHorizontal.FACING);
        if (block instanceof TrimmedRoofBlock) {
            TrimmedRoofBlock.Kind kind = ((TrimmedRoofBlock) block).kind;
            TextureAtlasSprite wood = net.minecraft.client.Minecraft.getMinecraft().getTextureMapBlocks()
                .getAtlasSprite("ironagearchitecture:blocks/beam_dark_oak");
            boolean lined = block instanceof FasciaBlock && ((FasciaBlock)block).lined;
            addTrim(kind, kind == TrimmedRoofBlock.Kind.VERGE && state.getValue(TrimmedRoofBlock.RIGHT), facing, wood, lined);
            if (block instanceof FasciaBlock && state.getValue(FasciaBlock.FITTING) != 0)
                DrainMesh.gutter(state.getValue(FasciaBlock.FITTING) == 2, state.getValue(FasciaBlock.ENDS), ironFaces(facing));
            return;
        }
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

    private RoofBakedModel(IBakedModel base, TextureAtlasSprite sprite) {
        this.base = base;
        this.sprite = sprite;
    }

    static RoofBakedModel gutter(IBakedModel base, boolean outlet) {
        RoofBakedModel model = new RoofBakedModel(base, iron());
        DrainMesh.gutter(outlet, 3, (a,b,c,d) -> {
            // Raise the standalone fitting into the centre of the inventory model.
            model.ironFaces(EnumFacing.NORTH).add(inventoryPoint(a), inventoryPoint(b), inventoryPoint(c), inventoryPoint(d));
        });
        return model;
    }

    private static double[] inventoryPoint(double[] point) {
        return new double[]{point[0], point[1] + 6, point[2] - 6.5};
    }

    static RoofBakedModel pipe(IBakedModel base, DownpipeBlock block, IBlockState state) {
        RoofBakedModel model = new RoofBakedModel(base, iron());
        DrainMesh.pipe(block.path(state), model.ironFaces(state.getValue(DownpipeBlock.FACING)));
        return model;
    }

    private static TextureAtlasSprite iron() {
        return net.minecraft.client.Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/iron_block");
    }

    private DrainMesh.Face ironFaces(EnumFacing facing) {
        TextureAtlasSprite metal = iron();
        return (a,b,c,d) -> {
            double[][] pixels = {a,b,c,d};
            double[][] points = new double[4][3];
            for (int i = 0; i < 4; i++)
                for (int axis = 0; axis < 3; axis++) points[i][axis] = pixels[i][axis] / 16;
            add(points, facing, false, metal, null, .43F, .46F, .47F);
        };
    }

    private void addTrim(TrimmedRoofBlock.Kind kind, boolean right, EnumFacing facing, TextureAtlasSprite wood, boolean lined) {
        RoofTrimGeometry.Section[] sections = RoofTrimGeometry.sections(kind, right);
        for (RoofTrimGeometry.Section section : sections) {
            double[][] corners = {{section.x0,section.z0},{section.x1,section.z0},
                {section.x1,section.z1},{section.x0,section.z1}};
            double[][] top = new double[4][], bottom = new double[4][];
            for (int i = 0; i < 4; i++) {
                double x = corners[i][0], z = corners[i][1];
                double height = RoofTrimGeometry.height(kind, x, z);
                top[i] = new double[]{x,height,z};
                bottom[i] = new double[]{x,RoofTrimGeometry.underside(section,height),z};
            }
            add(new double[][]{top[0],top[3],top[2],top[1]}, facing, true);
            if (kind == TrimmedRoofBlock.Kind.FASCIA) addEaveUnderside(facing, wood, lined);
            else add(bottom, facing, true, section.thin ? wood : sprite, null);
            for (int side = 0; side < 4; side++) {
                int next = (side + 1) % 4;
                RoofTrimGeometry.Section neighbour = adjoining(sections, section, side);
                if (neighbour != null && neighbour.thin == section.thin) continue;
                double[] lowA = bottom[side], lowB = bottom[next];
                double[] highA = top[side].clone(), highB = top[next].clone();
                if (neighbour != null) {
                    highA[1] = RoofTrimGeometry.underside(neighbour, highA[1]);
                    highB[1] = RoofTrimGeometry.underside(neighbour, highB[1]);
                }
                double a = highA[1] - lowA[1], b = highB[1] - lowB[1];
                if (a <= 0 && b <= 0) continue;
                if (a < 0 || b < 0) {
                    double t = a / (a - b);
                    double[] join = {lowA[0] + (lowB[0] - lowA[0])*t, lowA[1] + (lowB[1] - lowA[1])*t,
                        lowA[2] + (lowB[2] - lowA[2])*t};
                    if (a < 0) lowA = highA = join;
                    else lowB = highB = join;
                }
                boolean board = neighbour == null && (kind == TrimmedRoofBlock.Kind.FASCIA && side == 2
                    || kind == TrimmedRoofBlock.Kind.VERGE && side == (right ? 1 : 3)
                    || kind == TrimmedRoofBlock.Kind.RIDGE_END && side == 0);
                add(new double[][]{lowA,highA,highB,lowB}, facing, false, board ? wood : sprite, board ? kind : null);
            }
        }
    }

    private void addEaveUnderside(EnumFacing facing, TextureAtlasSprite wood, boolean lined) {
        double edge = RoofTrimGeometry.EAVE;
        double backOfBoard = edge - 3.0 / 16;
        TextureAtlasSprite underside = lined ? net.minecraft.client.Minecraft.getMinecraft().getTextureMapBlocks()
            .getAtlasSprite("minecraft:blocks/planks_big_oak") : sprite;
        // Replace the original bottom with two strips on the same plane. The lip and lining
        // stay within the roof cell, leaving the outlet and the pipe below it clear.
        add(new double[][]{{0,0,0},{1,0,0},{1,0,backOfBoard},{0,0,backOfBoard}}, facing, true, underside, null);
        add(new double[][]{{0,0,backOfBoard},{1,0,backOfBoard},{1,0,edge},{0,0,edge}},
            facing, true, wood, TrimmedRoofBlock.Kind.FASCIA);
    }

    private static RoofTrimGeometry.Section adjoining(RoofTrimGeometry.Section[] sections, RoofTrimGeometry.Section section, int side) {
        for (RoofTrimGeometry.Section other : sections) {
            if (other == section) continue;
            if (side % 2 == 0 && other.x0 == section.x0 && other.x1 == section.x1
                    && (side == 0 ? other.z1 == section.z0 : other.z0 == section.z1)) return other;
            if (side % 2 == 1 && other.z0 == section.z0 && other.z1 == section.z1
                    && (side == 1 ? other.x0 == section.x1 : other.x1 == section.x0)) return other;
        }
        return null;
    }

    private void add(double[][] points, EnumFacing facing, boolean top) {
        add(points, facing, top, sprite, null);
    }

    private void add(double[][] points, EnumFacing facing, boolean top, TextureAtlasSprite texture,
            @Nullable TrimmedRoofBlock.Kind board) {
        add(points, facing, top, texture, board, 1, 1, 1);
    }

    private void add(double[][] points, EnumFacing facing, boolean top, TextureAtlasSprite texture,
            @Nullable TrimmedRoofBlock.Kind board, float red, float green, float blue) {
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
        builder.setTexture(texture);
        builder.setQuadOrientation(EnumFacing.getFacingFromVector((float)nx,(float)ny,(float)nz));
        builder.setApplyDiffuseLighting(true);
        for (int vertex = 0; vertex < 4; vertex++) {
            double[] p = rotated[vertex], source = points[vertex];
            boolean alongX = Math.abs(points[0][0]-points[2][0]) > 1e-8;
            float textureU = (float)((top || alongX ? source[0] : source[2]) * 16);
            double v = top ? source[2] : 1-source[1];
            if (board != null) v = RoofTrimGeometry.height(board, source[0], source[2]) - source[1];
            float textureV = (float)(Math.max(0,Math.min(1,v)) * 16);
            if (board == TrimmedRoofBlock.Kind.FASCIA) {
                // The beam sprite's grain runs vertically. Turn it along the length of the fascia.
                float acrossGrain = textureV;
                textureV = textureU;
                textureU = acrossGrain;
            }
            for (int element = 0; element < format.getElementCount(); element++) {
                switch (format.getElement(element).getUsage()) {
                    case POSITION: builder.put(element,(float)p[0],(float)p[1],(float)p[2],1); break;
                    case COLOR: builder.put(element,red,green,blue,1); break;
                case UV: builder.put(element,texture.getInterpolatedU(textureU),texture.getInterpolatedV(textureV),0,1); break;
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
