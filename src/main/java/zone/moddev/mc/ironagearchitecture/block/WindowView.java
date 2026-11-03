package zone.moddev.mc.ironagearchitecture.block;

import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/** Render-only snapshot; no world/client objects or saved data. */
public final class WindowView {
    public static final IUnlistedProperty<WindowView> PROPERTY=new IUnlistedProperty<WindowView>() {
        public String getName(){return "window_view";} public boolean isValid(WindowView view){return true;}
        public Class<WindowView> getType(){return WindowView.class;} public String valueToString(WindowView view){return String.valueOf(view);}
    };
    public final IBlockState state;public final int joins;public final boolean proxy;
    public WindowView(IBlockState state,int joins,boolean proxy){this.state=state;this.joins=joins;this.proxy=proxy;}
    @Override public String toString(){return state+":"+joins+":"+proxy;}
}
