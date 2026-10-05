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

import reika.dragonapi.exception.MisuseException;

public record CommutativePair<V>(V o1, V o2) {

    public CommutativePair {
        if (o1 == null || o2 == null)
            throw new MisuseException("You cannot create a pair with null!");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CommutativePair b) {
            if (b.o1.equals(o1) && b.o2.equals(o2))
                return true;
            //reverse
            return b.o1.equals(o2) && b.o2.equals(o1);
        }
        return false;
    }

}
