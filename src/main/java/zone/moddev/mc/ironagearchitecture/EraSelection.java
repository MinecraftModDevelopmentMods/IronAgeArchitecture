package zone.moddev.mc.ironagearchitecture;

import java.util.*;

/** Restart-time survival selection. Unknown IDs never silently enable an era. */
public final class EraSelection {
    public final Set<Era> allowed;
    public final List<String> unknown;
    private EraSelection(EnumSet<Era> allowed,List<String> unknown) {
        this.allowed=Collections.unmodifiableSet(allowed);this.unknown=Collections.unmodifiableList(unknown);
    }
    public static EraSelection parse(String... values) {
        EnumSet<Era> allowed=EnumSet.noneOf(Era.class);List<String> unknown=new ArrayList<>();
        for(String input:values) {
            String id=input.trim().toLowerCase(Locale.ROOT);
            if(id.isEmpty()) continue;
            if(id.equals("all")||id.equals("*")) {allowed.addAll(EnumSet.allOf(Era.class));continue;}
            boolean found=false;
            for(Era era:Era.values()) if(era.id.equals(id)) {allowed.add(era);found=true;break;}
            if(!found) unknown.add(input);
        }
        return new EraSelection(allowed,unknown);
    }
    public boolean permits(Set<Era> membership) {return !Collections.disjoint(allowed,membership);}
}
