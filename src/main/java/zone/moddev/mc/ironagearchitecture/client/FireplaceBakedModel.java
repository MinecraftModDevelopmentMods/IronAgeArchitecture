package zone.moddev.mc.ironagearchitecture.client;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;
import zone.moddev.mc.ironagearchitecture.block.FireplaceBlock;

/** Cache the local join shapes while keeping the baked model free of world references. */
final class FireplaceBakedModel implements IBakedModel {
    private final IBakedModel base;
    private final FireplaceBlock block;
    private final IBlockState state;
    private final TextureAtlasSprite masonry;
    private final ConcurrentMap<Integer, RoofBakedModel> meshes = new ConcurrentHashMap<>();

    FireplaceBakedModel(IBakedModel base, FireplaceBlock block, IBlockState state, TextureAtlasSprite masonry) {
        this.base = base;
        this.block = block;
        this.state = state;
        this.masonry = masonry;
    }

    @Override public List<BakedQuad> getQuads(@Nullable IBlockState extended, @Nullable EnumFacing side, long seed) {
        if (side != null) return Collections.emptyList();
        Integer mask = extended instanceof IExtendedBlockState ? ((IExtendedBlockState)extended).getValue(FireplaceBlock.JOINS) : null;
        return meshes.computeIfAbsent(mask == null ? 0 : mask,
            joins -> RoofBakedModel.fireplace(base, block, state, masonry, joins)).getQuads(null, null, seed);
    }

    @Override public boolean isAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean isBuiltInRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleTexture() { return masonry; }
    @Override public ItemCameraTransforms getItemCameraTransforms() { return base.getItemCameraTransforms(); }
    @Override public ItemOverrideList getOverrides() { return ItemOverrideList.NONE; }
}
