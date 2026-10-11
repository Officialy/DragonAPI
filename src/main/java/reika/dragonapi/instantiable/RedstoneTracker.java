package reika.dragonapi.instantiable;

import reika.dragonapi.base.BlockEntityBase;

public class RedstoneTracker {

    private int value;

    public void update(BlockEntityBase te) {
        int last = value;
        value = te.getRedstoneOverride();
        if (last != value) {
            te.triggerBlockUpdate();
            te.getLevel().updateNeighbourForOutputSignal(te.getBlockPos(), te.getBlockState().getBlock());
        }
    }

    public int getValue() {
        return value;
    }

}
