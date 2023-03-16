package reika.dragonapi.interfaces.registry;

import net.minecraft.world.level.block.Block;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.instantiable.data.immutable.BlockKey;

public interface TreeType {

    BlockKey getItem();

    Block getLogID();

    Block getLeafID();

    Block getSaplingID();

    Block getPlankID();

    boolean canBePlacedSideways();

    boolean exists();

    BlockKey getBasicLeaf();

    BlockBox getTypicalMaximumSize();
}
