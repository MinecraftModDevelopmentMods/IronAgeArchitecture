package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;

/** Single-threaded server transactions; block callbacks defer until the complete column is committed. */
public final class WindowController {
    private static final ThreadLocal<Boolean> BUSY=ThreadLocal.withInitial(()->false);
    private static final Map<World,Set<BlockPos>> ENTITY_RETRIES=new WeakHashMap<>();
    private WindowController() { }
    public static boolean busy(){return BUSY.get();}
    public static List<BlockPos> column(World world,BlockPos pos) {
        List<BlockPos> result=new ArrayList<>();if(!world.isBlockLoaded(pos)) return result;IBlockState state=world.getBlockState(pos);if(!(state.getBlock() instanceof WindowBlock)) return result;
        WindowBlock block=(WindowBlock)state.getBlock();BlockPos low=pos;
        while(low.getY()>0&&block.matches(state,world.getBlockState(low.down()),true)) low=low.down();
        for(BlockPos at=low;at.getY()<world.getHeight()&&block.matches(state,world.getBlockState(at),true);at=at.up()) result.add(at);
        return result;
    }
    public static boolean owns(World world,BlockPos leaf,BlockPos owner) {
        if(world.getBlockState(leaf).getBlock()!=ArchitectureBlocks.WINDOW_LEAF) return false;
        return leaf.offset(world.getBlockState(leaf).getValue(WindowLeafBlock.FACING)).equals(owner);
    }
    static void message(EntityPlayer player,String key){if(player!=null) player.sendStatusMessage(new TextComponentTranslation("message.ironagearchitecture.window."+key));}
    public static boolean toggle(World world,BlockPos pos,EntityPlayer player) {
        List<BlockPos> parts=column(world,pos);if(parts.isEmpty()) return false;
        Boolean power=null;
        if(world.isAreaLoaded(pos.add(-2,0,-2),pos.add(2,0,2))) {power=false;for(BlockPos part:parts) power|=world.isBlockPowered(part);}
        // A successful manual override also acknowledges a previously blocked power edge.
        return set(world,parts,!world.getBlockState(pos).getValue(WindowBlock.OPEN),power,player,true);
    }
    public static boolean set(World world,List<BlockPos> parts,boolean open,Boolean powered,EntityPlayer player,boolean sound) {
        if(parts.isEmpty()||world.isRemote||busy()) return false;
        Map<BlockPos,IBlockState> before=new LinkedHashMap<>();boolean changed=false;
        for(BlockPos pos:parts) {
            IBlockState state=world.getBlockState(pos);if(!(state.getBlock() instanceof WindowBlock)) return false;
            WindowBlock block=(WindowBlock)state.getBlock();BlockPos leaf=pos.offset(state.getValue(WindowBlock.FACING));
            if(!world.isBlockLoaded(leaf)){message(player,"blocked");return false;}
            if(open) {
                if(!world.isAirBlock(leaf)&&!owns(world,leaf,pos)){message(player,"blocked");return false;}
                if(!state.getValue(WindowBlock.OPEN)&&!world.checkNoEntityCollision(WindowGeometry.sweep(block.right,state.getValue(WindowBlock.FACING),block.back).offset(pos))){
                    message(player,"blocked");return false;
                }
            }
            before.put(pos,state);before.put(leaf,world.getBlockState(leaf));changed|=state.getValue(WindowBlock.OPEN)!=open;
        }
        boolean ok=true;BUSY.set(true);
        try {
            for(BlockPos pos:parts) {
                IBlockState old=before.get(pos);BlockPos leaf=pos.offset(old.getValue(WindowBlock.FACING));
                if(open) ok&=write(world,leaf,ArchitectureBlocks.WINDOW_LEAF.getDefaultState().withProperty(WindowLeafBlock.FACING,old.getValue(WindowBlock.FACING).getOpposite()));
                else if(owns(world,leaf,pos)) ok&=write(world,leaf,Blocks.AIR.getDefaultState());
                IBlockState next=old.withProperty(WindowBlock.OPEN,open);if(powered!=null) next=next.withProperty(WindowBlock.POWERED,powered);
                ok&=write(world,pos,next);
                if(!ok) break;
            }
            if(!ok) for(Map.Entry<BlockPos,IBlockState> entry:before.entrySet()) write(world,entry.getKey(),entry.getValue());
        } finally {BUSY.set(false);}
        notify(world,before.keySet());
        if(ok&&sound&&changed) world.playSound(null,parts.get(0),open?SoundEvents.BLOCK_WOODEN_DOOR_OPEN:SoundEvents.BLOCK_WOODEN_DOOR_CLOSE,SoundCategory.BLOCKS,1,1);
        return ok;
    }
    private static boolean write(World world,BlockPos pos,IBlockState state){return world.getBlockState(pos).equals(state)||world.setBlockState(pos,state,2);}
    private static void notify(World world,Collection<BlockPos> positions){for(BlockPos pos:positions){for(EnumFacing side:EnumFacing.values()){BlockPos next=pos.offset(side);if(world.isBlockLoaded(next)) world.getBlockState(next).neighborChanged(world,next,world.getBlockState(pos).getBlock());}world.markBlockRangeForRenderUpdate(pos.add(-1,-1,-1),pos.add(1,1,1));}}
    public static void removeLeaf(World world,BlockPos pos,IBlockState state) {
        if(world.isRemote||!(state.getBlock() instanceof WindowBlock)) return;
        BlockPos leaf=pos.offset(state.getValue(WindowBlock.FACING));if(!world.isBlockLoaded(leaf)||!owns(world,leaf,pos)) return;
        boolean previous=busy();BUSY.set(true);try{world.setBlockState(leaf,Blocks.AIR.getDefaultState(),2);}finally{BUSY.set(previous);}
    }
    public static void neighbor(World world,BlockPos pos) {
        if(busy()||world.isRemote) return;List<BlockPos> parts=column(world,pos);if(parts.isEmpty()) return;
        if(!world.isAreaLoaded(pos.add(-2,0,-2),pos.add(2,0,2))) return;
        IBlockState first=world.getBlockState(parts.get(0));boolean power=false;
        for(BlockPos part:parts) power|=world.isBlockPowered(part);
        if(power!=first.getValue(WindowBlock.POWERED)) {
            if(!set(world,parts,power,power,null,true)&&power) rememberEntityBlocker(world,parts);
            return;
        }
        repair(world,pos);
    }
    private static void rememberEntityBlocker(World world,List<BlockPos> parts) {
        // Block removal already sends an update; only moving entities need this retry.
        for(BlockPos pos:parts) {IBlockState state=world.getBlockState(pos);BlockPos leaf=pos.offset(state.getValue(WindowBlock.FACING));if(!world.isBlockLoaded(leaf)||!world.isAirBlock(leaf)&&!owns(world,leaf,pos)) return;}
        for(BlockPos pos:parts) {
            IBlockState state=world.getBlockState(pos);WindowBlock block=(WindowBlock)state.getBlock();
            if(!world.checkNoEntityCollision(WindowGeometry.sweep(block.right,state.getValue(WindowBlock.FACING),block.back).offset(pos))) {ENTITY_RETRIES.computeIfAbsent(world,key->new HashSet<>()).add(parts.get(0).toImmutable());return;}
        }
    }
    /** No idle window scan: only failed, entity-blocked power edges enter this short-lived queue. */
    public static void retryEntities(World world) {
        Set<BlockPos> waiting=ENTITY_RETRIES.remove(world);if(waiting==null||world.isRemote) return;
        for(BlockPos pos:waiting) if(world.isBlockLoaded(pos)&&world.getBlockState(pos).getBlock() instanceof WindowBlock) neighbor(world,pos);
    }
    public static void repair(World world,BlockPos pos) {
        if(busy()||world.isRemote) return;IBlockState state=world.getBlockState(pos);
        if(state.getBlock() instanceof WindowLeafBlock) {
            BlockPos owner=pos.offset(state.getValue(WindowLeafBlock.FACING));if(!world.isBlockLoaded(owner)) return;
            IBlockState parent=world.getBlockState(owner);
            if(!(parent.getBlock() instanceof WindowBlock)||!parent.getValue(WindowBlock.OPEN)||!owner.offset(parent.getValue(WindowBlock.FACING)).equals(pos)) {
                BUSY.set(true);try{world.setBlockState(pos,Blocks.AIR.getDefaultState(),2);}finally{BUSY.set(false);}
            } else repair(world,owner);
        } else if(state.getBlock() instanceof WindowBlock&&state.getValue(WindowBlock.OPEN)) {
            BlockPos leaf=pos.offset(state.getValue(WindowBlock.FACING));if(!world.isBlockLoaded(leaf)||owns(world,leaf,pos)) return;
            List<BlockPos> parts=column(world,pos);
            if(!set(world,parts,true,null,null,false)) set(world,parts,false,null,null,false);
        }
    }
    static boolean place(World world,BlockPos pos,IBlockState state,EntityPlayer player) {
        WindowBlock block=(WindowBlock)state.getBlock();Boolean open=null,power=null;
        for(BlockPos other:new BlockPos[]{pos.down(),pos.up()}) {
            IBlockState neighbor=world.getBlockState(other);if(!block.matches(state,neighbor,true)) continue;
            if(open!=null&&open!=neighbor.getValue(WindowBlock.OPEN)){message(player,"join");return false;}
            open=neighbor.getValue(WindowBlock.OPEN);power=neighbor.getValue(WindowBlock.POWERED);
        }
        if(open!=null) state=state.withProperty(WindowBlock.OPEN,open).withProperty(WindowBlock.POWERED,power);
        BlockPos leaf=pos.offset(state.getValue(WindowBlock.FACING));
        if(state.getValue(WindowBlock.OPEN)&&(!world.isBlockLoaded(leaf)||!world.isAirBlock(leaf)||!world.checkNoEntityCollision(WindowGeometry.sweep(block.right,state.getValue(WindowBlock.FACING),block.back).offset(pos)))){message(player,"blocked");return false;}
        for(net.minecraft.util.math.AxisAlignedBB box:WindowGeometry.boxes(block.thin,block.right,state.getValue(WindowBlock.OPEN),state.getValue(WindowBlock.FACING),0,false,block.back)) if(!world.checkNoEntityCollision(box.offset(pos))) return false;
        IBlockState previous=world.getBlockState(pos);boolean ok;BUSY.set(true);
        try {ok=write(world,pos,state);if(ok&&state.getValue(WindowBlock.OPEN)) ok=write(world,leaf,ArchitectureBlocks.WINDOW_LEAF.getDefaultState().withProperty(WindowLeafBlock.FACING,state.getValue(WindowBlock.FACING).getOpposite()));if(!ok) write(world,pos,previous);}
        finally{BUSY.set(false);}
        if(ok){notify(world,Arrays.asList(pos,leaf));neighbor(world,pos);}return ok;
    }
}
