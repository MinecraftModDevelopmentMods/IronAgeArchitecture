package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import java.util.function.ToIntFunction;

/** Bounded LRU retention: evict cold geometry, never empty the whole working set. */
public final class MeshCache<K,V> {
    private final LinkedHashMap<K,V> entries=new LinkedHashMap<>(64,.75F,true);
    private final int capacity;private final ToIntFunction<V> weight;private int used;
    public MeshCache(int capacity,ToIntFunction<V> weight){this.capacity=capacity;this.weight=weight;}
    public synchronized V get(K key){return entries.get(key);}
    public synchronized void put(K key,V value){V old=entries.put(key,value);used+=Math.max(1,weight.applyAsInt(value))-(old==null?0:Math.max(1,weight.applyAsInt(old)));Iterator<Map.Entry<K,V>> i=entries.entrySet().iterator();while(used>capacity&&entries.size()>1&&i.hasNext()){used-=Math.max(1,weight.applyAsInt(i.next().getValue()));i.remove();}}
    public synchronized int size(){return entries.size();}
    public synchronized int weight(){return used;}
}
