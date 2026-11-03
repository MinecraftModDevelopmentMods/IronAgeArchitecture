package zone.moddev.mc.ironagearchitecture.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.block.FrameBlock;

/** Invalidate the bounded, render-derived rail runs after a live client block update. */
final class WallRenderListener implements IWorldEventListener {
    @Override public void notifyBlockUpdate(World world,BlockPos pos,IBlockState old,IBlockState next,int flags) {
        if(old.getBlock() instanceof FrameBlock||next.getBlock() instanceof FrameBlock
            ||old.getBlock() instanceof zone.moddev.mc.ironagearchitecture.block.WindowBlock||next.getBlock() instanceof zone.moddev.mc.ironagearchitecture.block.WindowBlock
            ||old.getBlock() instanceof zone.moddev.mc.ironagearchitecture.block.WindowLeafBlock||next.getBlock() instanceof zone.moddev.mc.ironagearchitecture.block.WindowLeafBlock
            ||world.getBlockState(pos.up()).getBlock() instanceof zone.moddev.mc.ironagearchitecture.block.JettyFloorBlock)
            world.markBlockRangeForRenderUpdate(pos.add(-16,-1,-16),pos.add(16,1,16));
    }
    @Override public void notifyLightSet(BlockPos pos) { }
    @Override public void markBlockRangeForRenderUpdate(int a,int b,int c,int d,int e,int f) { }
    @Override public void playSoundToAllNearExcept(EntityPlayer p,SoundEvent s,SoundCategory c,double x,double y,double z,float v,float pitch) { }
    @Override public void playRecord(SoundEvent sound,BlockPos pos) { }
    @Override public void spawnParticle(int id,boolean ignore,double x,double y,double z,double vx,double vy,double vz,int... data) { }
    @Override public void onEntityAdded(Entity entity) { }
    @Override public void onEntityRemoved(Entity entity) { }
    @Override public void broadcastSound(int id,BlockPos pos,int data) { }
    @Override public void playEvent(EntityPlayer player,int type,BlockPos pos,int data) { }
    @Override public void sendBlockBreakProgress(int id,BlockPos pos,int progress) { }
}
