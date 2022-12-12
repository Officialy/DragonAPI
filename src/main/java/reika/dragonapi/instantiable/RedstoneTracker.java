package reika.dragonapi.instantiable;

import reika.dragonapi.base.BlockEntityBase;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

public class RedstoneTracker {

    private int value;

    public void update(BlockEntityBase te) {
        int last = value;
        value = te.getRedstoneOverride();
        if (last != value) {
            //ReikaJavaLibrary.pConsole(last+" > "+value);
            te.triggerBlockUpdate();
            ReikaWorldHelper.causeAdjacentUpdates(te.getLevel(), te.getBlockPos());
        }
    }

    public int getValue() {
        return value;
    }

}
