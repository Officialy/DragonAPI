/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.immutable;

public final class ImmutableArray<V> {

    public final int length;
    private final V[] data;

    public ImmutableArray(V[] arr) {
        data = arr;
        this.length = arr.length;
    }

    public V get(int i) {
        return data[i];
    }

}
