package zone.moddev.mc.ironagearchitecture.roof;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import zone.moddev.mc.ironagearchitecture.IronAgeArchitecture;

/** The common handler never resolves a client class on a headless server. */
public final class WorkbenchGuiHandler implements IGuiHandler {
    public Object getServerGuiElement(int id,EntityPlayer p,World w,int x,int y,int z){TileEntity t=w.getTileEntity(new BlockPos(x,y,z));return id==0&&t instanceof ArchitectWorkbench.Desk?new ArchitectWorkbench.Storage(p.inventory,(ArchitectWorkbench.Desk)t):null;}
    public Object getClientGuiElement(int id,EntityPlayer p,World w,int x,int y,int z){TileEntity t=w.getTileEntity(new BlockPos(x,y,z));return id==0&&t instanceof ArchitectWorkbench.Desk?IronAgeArchitecture.proxy.roofGui(p,(ArchitectWorkbench.Desk)t):null;}
}
