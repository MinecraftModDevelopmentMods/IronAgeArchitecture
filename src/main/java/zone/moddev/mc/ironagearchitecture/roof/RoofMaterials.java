package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import zone.moddev.mc.ironagearchitecture.*;

/** Canonical material units, independent of the number of mesh fragments. */
public final class RoofMaterials {
    private RoofMaterials(){}
    public static ItemStack item(RoofPart p){switch(p.kind){case RoofPart.SOFFIT:if(p.material==6)return ArchitectureBlocks.SOFFIT==null?null:new ItemStack(ArchitectureBlocks.SOFFIT);case RoofPart.TIMBER:case RoofPart.FASCIA:return new ItemStack(ArchitectureBlocks.ROOF_TIMBER,1,p.material);case RoofPart.COVER:return p.material==0?null:RoofConstruction.coverItem(p.material);case RoofPart.WINDOW:if(ArchitectureBlocks.WINDOWS.isEmpty())return null;return new ItemStack(ArchitectureBlocks.findWindow(ArchitectureBlocks.WOODS[p.material/4],(p.material%4)/2,false,(p.material&1)==0,false));case RoofPart.GUTTER:return p.material==2?(ArchitectureBlocks.GUTTER_OUTLET==null?null:new ItemStack(ArchitectureBlocks.GUTTER_OUTLET)):(ArchitectureBlocks.GUTTER==null?null:new ItemStack(ArchitectureBlocks.GUTTER));case RoofPart.WALL:String name=p.material<2?ArchitectureBlocks.FINISHES[p.material]:p.material<4?"wattle_and_daub":"clay_tile_hanging";if(p.material==2||p.material==3)return ArchitectureBlocks.DAUB==null?null:new ItemStack(ArchitectureBlocks.DAUB,1,p.material-2);Block b=Block.getBlockFromName(IronAgeArchitecture.MODID+":"+name);return b==null||b==Blocks.AIR?null:new ItemStack(b);default:return null;}}
    public static String key(ItemStack s){return s.getItem().getRegistryName()+"#"+s.getMetadata();}
    public static final class Stock {
        final Map<String,ItemStack> items=new TreeMap<>();
        public void add(ItemStack s,int n){if(s==null||n<=0)return;String k=key(s);ItemStack old=items.get(k);if(old==null){old=s.copy();old.stackSize=0;items.put(k,old);}old.stackSize+=n;}
        public int count(ItemStack s){ItemStack old=items.get(key(s));return old==null?0:old.stackSize;}
        public boolean take(ItemStack s,int n){if(s==null||n==0)return true;String k=key(s);ItemStack old=items.get(k);if(old==null||old.stackSize<n)return false;old.stackSize-=n;if(old.stackSize==0)items.remove(k);return true;}
        public int units(){int n=0;for(ItemStack s:items.values())n+=s.stackSize;return n;}
        public List<ItemStack> list(){List<ItemStack> out=new ArrayList<>();for(ItemStack s:items.values())out.add(s.copy());return out;}
        public NBTTagList write(){NBTTagList list=new NBTTagList();for(ItemStack s:items.values()){ItemStack one=s.copy();one.stackSize=1;NBTTagCompound n=one.writeToNBT(new NBTTagCompound());n.setInteger("quantity",s.stackSize);list.appendTag(n);}return list;}
        public static Stock read(NBTTagList list){if(list.tagCount()>128)throw new IllegalArgumentException("Too many material kinds");Stock out=new Stock();for(int i=0;i<list.tagCount();i++){NBTTagCompound n=list.getCompoundTagAt(i);ItemStack s=ItemStack.loadItemStackFromNBT(n);int q=n.getInteger("quantity");if(s==null||q<0||q>8192)throw new IllegalArgumentException("Invalid material reservation");out.add(s,q);}return out;}
    }
}
