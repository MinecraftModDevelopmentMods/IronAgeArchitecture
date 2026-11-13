package zone.moddev.mc.ironagearchitecture.block;

import java.util.Objects;

/** Frozen material geometry handed to render workers; no world access during baking. */
public final class SmartCobView {
    public final SmartCobProfile profile,above,below;
    public final boolean topSolid,bottomSolid;
    public SmartCobView(SmartCobProfile profile,SmartCobProfile above,SmartCobProfile below,boolean topSolid,boolean bottomSolid){
        this.profile=profile;this.above=above;this.below=below;this.topSolid=topSolid;this.bottomSolid=bottomSolid;
    }
    @Override public boolean equals(Object o){if(!(o instanceof SmartCobView))return false;SmartCobView v=(SmartCobView)o;return profile.equals(v.profile)&&Objects.equals(above,v.above)&&Objects.equals(below,v.below)&&topSolid==v.topSolid&&bottomSolid==v.bottomSolid;}
    @Override public int hashCode(){return Objects.hash(profile,above,below,topSolid,bottomSolid);}
}
