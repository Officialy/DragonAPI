package reika.dragonapi.interfaces.registry;

import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.instantiable.data.immutable.BlockKey;

public interface TreeType {

    BlockKey getItem();

    Block getLogID();

    Block getLeafID();

    default List<BlockState> getLogStates() {
        return getLogID().getStateDefinition().getPossibleStates();
    }

    default List<BlockState> getLeafStates() {
        return getLeafID().getStateDefinition().getPossibleStates();
    }

    Block getSaplingID();

    Block getPlankID();

    boolean canBePlacedSideways();

    boolean exists();

    BlockKey getBasicLeaf();

    BlockBox getTypicalMaximumSize();

    /** Maximum breadth-first breaking waves; ordinary trees retain the original thirty-wave limit. */
    default int getRecursiveBreakDepth() {
        return 30;
    }
}
