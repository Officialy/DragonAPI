/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.collections;


import java.util.HashSet;


public class TimedSet<V> {

    private final HashSet<V> data = new HashSet();
    private long lastTime;

    public boolean add(long time, V val) {
        if (time != this.lastTime) {
            this.clear();
        }
        this.lastTime = time;
        return this.data.add(val);
    }

    public boolean contains(V val) {
        return this.data.contains(val);
    }

    public int size() {
        return this.data.size();
    }

    public void clear() {
        this.data.clear();
    }

}
