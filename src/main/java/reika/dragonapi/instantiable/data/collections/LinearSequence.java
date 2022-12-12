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


import java.util.LinkedList;

public class LinearSequence {

    private final LinkedList sequence = new LinkedList();

    private int currentIndex;

    public LinearSequence addObject(Object o) {
        sequence.addLast(o);
        return this;
    }

    public Object getEntry() {
        return sequence.get(currentIndex);
    }

    public void step() {
        currentIndex++;
    }

    public Object getNext() {
        int index = currentIndex;
        this.step();
        return sequence.get(index);
    }

}
