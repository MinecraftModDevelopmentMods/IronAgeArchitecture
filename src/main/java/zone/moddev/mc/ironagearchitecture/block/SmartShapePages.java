package zone.moddev.mc.ironagearchitecture.block;

import java.io.File;
import java.util.*;
import java.util.function.LongFunction;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.*;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/** Small authoritative overlays over the untouched legacy snapshot, saved by MapStorage. */
final class SmartShapePages {
    private final MapStorage storage;
    private final String prefix;
    private final Map<Long,Page> pages=new HashMap<>();
    private final LongFunction<NBTTagCompound> snapshot;

    static long key(int chunkX,int chunkZ){return SmartWallData.chunkKey(chunkX>>1,chunkZ>>1);}
    static File directory(World world){MapStorage storage=world.getPerWorldStorage();ISaveHandler handler=ReflectionHelper.getPrivateValue(MapStorage.class,storage,"saveHandler","field_75751_a");return handler.getMapFileFromName("iaa_shape_location").getParentFile();}
    SmartShapePages(World world,String prefix,LongFunction<NBTTagCompound> snapshot) {
        this.storage=world.getPerWorldStorage();this.prefix=prefix+"_";this.snapshot=snapshot;
        // Use this dimension's MapStorage handler, which may differ from the world's
        // public save handler. No chunks are loaded and this directory is read once.
        File folder=directory(world);
        File[] files=folder.listFiles((dir,name)->name.startsWith(this.prefix)&&name.endsWith(".dat"));
        if(files==null){if(folder.exists()&&!folder.isDirectory())throw new IllegalStateException("Invalid smart shape data directory");return;}
        Arrays.sort(files,Comparator.comparing(File::getName));
        for(File file:files){String name=file.getName().substring(0,file.getName().length()-4);long key=Long.parseUnsignedLong(name.substring(this.prefix.length()),16);
            Page page=(Page)storage.getOrLoadData(Page.class,name);
            if(page==null||page.loaded==null||page.loaded.getInteger("pageFormat")!=1||page.loaded.getLong("page")!=key)
                throw new IllegalStateException("Cannot read smart shape page "+file.getName()+"; keep the file and restore a backup");
            page.snapshot=snapshot;page.key=key;pages.put(key,page);
        }
    }
    Collection<NBTTagCompound> loaded(){List<NBTTagCompound> out=new ArrayList<>();for(Page page:pages.values())out.add(page.loaded);return out;}
    void restored(){for(Page page:pages.values())page.loaded=null;}
    void dirty(long key){Page page=pages.get(key);if(page==null){page=new Page(prefix+Long.toHexString(key));page.key=key;page.snapshot=snapshot;pages.put(key,page);storage.setData(page.mapName,page);}page.markDirty();}

    /** Public constructor is required by MapStorage's reflective loader. */
    public static final class Page extends WorldSavedData {
        private long key;private NBTTagCompound loaded;private LongFunction<NBTTagCompound> snapshot;
        public Page(String name){super(name);}
        @Override public void readFromNBT(NBTTagCompound root){loaded=root;setDirty(false);}
        @Override public NBTTagCompound writeToNBT(NBTTagCompound root){NBTTagCompound value=snapshot.apply(key);value.setInteger("pageFormat",1);value.setLong("page",key);return value;}
    }
}
