package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.*;

/** Ordinary container storage. Active construction is scheduled centrally, never by tile ticks. */
public final class ArchitectWorkbench extends Block {
    public ArchitectWorkbench(){super(Material.WOOD);setHardness(2.5F);setSoundType(SoundType.WOOD);setLightOpacity(0);}
    @Override public boolean isOpaqueCube(IBlockState s){return false;}
    @Override public boolean isFullCube(IBlockState s){return false;}
    @Override public boolean hasTileEntity(IBlockState state){return true;}
    @Override public TileEntity createTileEntity(World w,IBlockState s){return new Desk();}
    @Override public boolean canPlaceBlockAt(World w,BlockPos p){return super.canPlaceBlockAt(w,p)&&RoofConstruction.floor(w,p.down());}
    @Override public boolean onBlockActivated(World w,BlockPos p,IBlockState s,EntityPlayer player,EnumHand hand,@Nullable ItemStack held,EnumFacing side,float x,float y,float z){if(!w.isRemote){if(!RoofConstruction.floor(w,p.down())){player.sendStatusMessage(new TextComponentString("Place the workbench on a full timber attic floor."));return true;}player.openGui(IronAgeArchitecture.instance,0,w,p.getX(),p.getY(),p.getZ());}return true;}
    @Override public void breakBlock(World w,BlockPos p,IBlockState s){TileEntity t=w.getTileEntity(p);if(t instanceof Desk){Desk d=(Desk)t;if(!w.isRemote){RoofConstruction.cancel(d);InventoryHelper.dropInventoryItems(w,p,d);}}super.breakBlock(w,p,s);}
    public static final class Desk extends TileEntity implements IInventory {
        @Override public boolean onlyOpsCanSetNbt(){return true;}
        private final ItemStack[] inventory=new ItemStack[27];
        public RoofPlan drawing,prior;
        public String status="Detect the attic floor to begin.";
        public boolean active,preview,creative,automatic=true;
        public int index,remaining,revision;
        public final List<Long> operations=new ArrayList<>();
        public final Set<Long> present=new HashSet<>();
        @Override public NBTTagCompound getUpdateTag(){return super.writeToNBT(new NBTTagCompound());}
        @Override public void onLoad(){if(!world.isRemote&&active)RoofConstruction.enqueue(this);}
        @Override public void readFromNBT(NBTTagCompound n){super.readFromNBT(n);Arrays.fill(inventory,null);NBTTagList slots=n.getTagList("items",10);for(int i=0;i<slots.tagCount();i++){NBTTagCompound a=slots.getCompoundTagAt(i);int slot=a.getInteger("slot");if(slot>=0&&slot<inventory.length)inventory[slot]=ItemStack.loadItemStackFromNBT(a);}drawing=n.hasKey("drawing")?RoofPlan.read(n.getCompoundTag("drawing")):null;prior=n.hasKey("prior")?RoofPlan.read(n.getCompoundTag("prior")):null;status=n.getString("status");active=n.getBoolean("active");creative=n.getBoolean("creative");preview=n.getBoolean("preview");automatic=!n.hasKey("automatic")||n.getBoolean("automatic");index=n.getInteger("index");remaining=n.getInteger("remaining");operations.clear();present.clear();NBTTagList ops=n.getTagList("operations",4),seen=n.getTagList("present",4);for(int i=0;i<ops.tagCount();i++)operations.add(((NBTTagLong)ops.get(i)).getLong());for(int i=0;i<seen.tagCount();i++)present.add(((NBTTagLong)seen.get(i)).getLong());if(index<0||index>operations.size()||Math.abs(remaining)>1024||operations.size()>2048)throw new IllegalStateException("Invalid roof construction progress");}
        @Override public NBTTagCompound writeToNBT(NBTTagCompound n){super.writeToNBT(n);NBTTagList slots=new NBTTagList();for(int i=0;i<inventory.length;i++)if(inventory[i]!=null){NBTTagCompound a=inventory[i].writeToNBT(new NBTTagCompound());a.setInteger("slot",i);slots.appendTag(a);}n.setTag("items",slots);if(drawing!=null)n.setTag("drawing",(active?drawing:drawing.settingsCopy()).write(new NBTTagCompound()));if(prior!=null)n.setTag("prior",prior.write(new NBTTagCompound()));n.setString("status",status);n.setBoolean("active",active);n.setBoolean("creative",creative);n.setBoolean("preview",preview);n.setBoolean("automatic",automatic);n.setInteger("index",index);n.setInteger("remaining",remaining);NBTTagList ops=new NBTTagList(),seen=new NBTTagList();for(long p:operations)ops.appendTag(new NBTTagLong(p));for(long p:present)seen.appendTag(new NBTTagLong(p));n.setTag("operations",ops);n.setTag("present",seen);return n;}
        public void give(ItemStack stack){if(stack==null)return;for(int i=0;i<inventory.length&&stack.stackSize>0;i++){ItemStack old=inventory[i];if(old!=null&&old.isItemEqual(stack)&&ItemStack.areItemStackTagsEqual(old,stack)){int count=Math.min(64-old.stackSize,stack.stackSize);old.stackSize+=count;stack.stackSize-=count;}}for(int i=0;i<inventory.length&&stack.stackSize>0;i++)if(inventory[i]==null){int count=Math.min(64,stack.stackSize);inventory[i]=stack.copy();inventory[i].stackSize=count;stack.stackSize-=count;}while(stack.stackSize>0){ItemStack part=stack.copy();part.stackSize=Math.min(64,stack.stackSize);stack.stackSize-=part.stackSize;Block.spawnAsEntity(world,pos,part);}markDirty();}
        public int count(net.minecraft.item.Item item,int meta){int n=0;for(ItemStack s:inventory)if(s!=null&&s.getItem()==item&&(meta<0||s.getMetadata()==meta))n+=s.stackSize;return n;}
        public void take(net.minecraft.item.Item item,int meta,int count){for(int i=0;i<inventory.length&&count>0;i++){ItemStack s=inventory[i];if(s==null||s.getItem()!=item||(meta>=0&&s.getMetadata()!=meta))continue;int used=Math.min(count,s.stackSize);s.stackSize-=used;count-=used;if(s.stackSize==0)inventory[i]=null;}if(count!=0)throw new IllegalStateException("Timber reservation changed");markDirty();}
        public int getSizeInventory(){return inventory.length;}public ItemStack getStackInSlot(int i){return inventory[i];}
        public ItemStack decrStackSize(int i,int count){ItemStack s=inventory[i];if(s==null)return null;ItemStack part=s.splitStack(count);if(s.stackSize<=0)inventory[i]=null;markDirty();return part;}
        public ItemStack removeStackFromSlot(int i){ItemStack s=inventory[i];inventory[i]=null;markDirty();return s;}
        public void setInventorySlotContents(int i,ItemStack s){inventory[i]=s;if(s!=null)s.stackSize=Math.min(s.stackSize,64);markDirty();}
        public int getInventoryStackLimit(){return 64;}public boolean isUsableByPlayer(EntityPlayer p){return !isInvalid()&&world.getTileEntity(pos)==this&&p.getDistanceSq(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
        public void openInventory(EntityPlayer p){}public void closeInventory(EntityPlayer p){}public boolean isItemValidForSlot(int i,ItemStack s){return !active&&s!=null&&(s.getItem()==ArchitectureBlocks.ROOF_TIMBER||s.getItem()==net.minecraft.init.Items.STICK||s.getItem()==net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.PLANKS));}
        public int getField(int id){return id==0?index:id==1?operations.size():0;}public void setField(int id,int value){}public int getFieldCount(){return 2;}
        public void clear(){Arrays.fill(inventory,null);markDirty();}public String getName(){return "Architect's Workbench";}public boolean hasCustomName(){return false;}public ITextComponent getDisplayName(){return new TextComponentString(getName());}
    }
    public static final class Storage extends Container {
        public final Desk desk;private int inventoryHash;
        public Storage(InventoryPlayer player,Desk desk){this.desk=desk;for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlotToContainer(new Slot(desk,col+row*9,12+col*18,98+row*18){@Override public boolean isItemValid(ItemStack s){return desk.isItemValidForSlot(getSlotIndex(),s);}@Override public boolean canTakeStack(EntityPlayer p){return !desk.active;}});for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlotToContainer(new Slot(player,col+row*9+9,12+col*18,159+row*18));for(int col=0;col<9;col++)addSlotToContainer(new Slot(player,col,12+col*18,217));}
        @Override public boolean canInteractWith(EntityPlayer p){return desk.isUsableByPlayer(p);}
        @Override public ItemStack transferStackInSlot(EntityPlayer p,int index){Slot slot=inventorySlots.get(index);if(!slot.getHasStack()||desk.active)return null;ItemStack old=slot.getStack(),copy=old.copy();if(index<27){if(!mergeItemStack(old,27,63,true))return null;}else{if(!desk.isItemValidForSlot(0,old)||!mergeItemStack(old,0,27,false))return null;}if(old.stackSize==0)slot.putStack(null);else slot.onSlotChanged();return copy;}
        @Override public void detectAndSendChanges(){super.detectAndSendChanges();int hash=1;for(int i=0;i<desk.getSizeInventory();i++){ItemStack s=desk.getStackInSlot(i);hash=31*hash+(s==null?0:31*(31*Item.getIdFromItem(s.getItem())+s.getMetadata())+s.stackSize);}if(!desk.active&&hash!=inventoryHash){inventoryHash=hash;for(EntityPlayer p:desk.getWorld().playerEntities)if(p instanceof EntityPlayerMP&&p.openContainer==this)RoofNetwork.reply((EntityPlayerMP)p,desk,RoofConstruction.previewing((EntityPlayerMP)p,desk));}}
    }
}
