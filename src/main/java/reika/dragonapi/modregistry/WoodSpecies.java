package reika.dragonapi.modregistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/** An explicit species association; no metadata, registry-name guesses or mutable stack keys. */
public final class WoodSpecies {
    private final List<BlockState> logs, leaves, saplings;
    private final ItemStack logTemplate, leafTemplate, saplingTemplate;
    private final boolean persistentLeavesAreNatural;

    /** Lists contain every admitted state, with the preferred state first. */
    public WoodSpecies(List<BlockState> logs, List<BlockState> leaves, List<BlockState> saplings,
            boolean persistentLeavesAreNatural) {
        this(logs, leaves, saplings, persistentLeavesAreNatural, null, null, null);
    }

    private WoodSpecies(List<BlockState> logs, List<BlockState> leaves, List<BlockState> saplings,
            boolean persistentLeavesAreNatural, ItemStack log, ItemStack leaf, ItemStack sapling) {
        this.logs = states(logs);
        this.leaves = states(leaves);
        this.saplings = states(saplings);
        if (this.logs.isEmpty() || this.leaves.isEmpty())
            throw new IllegalArgumentException("A wood species requires logs and leaves");
        this.persistentLeavesAreNatural = persistentLeavesAreNatural;
        logTemplate = log == null ? null : log.copyWithCount(1);
        leafTemplate = leaf == null ? null : leaf.copyWithCount(1);
        saplingTemplate = sapling == null ? null : sapling.copyWithCount(1);
    }

    /** For species whose variants have distinct registered blocks. */
    public static WoodSpecies ofBlocks(Block log, Block leaf, Block sapling, boolean persistentLeavesAreNatural) {
        return new WoodSpecies(allStates(log), allStates(leaf), sapling == null ? List.of() : allStates(sapling),
                persistentLeavesAreNatural);
    }

    /** Preserve an integration's actual item components while expanding transient world states. */
    public static WoodSpecies ofItems(ItemStack log, ItemStack leaf, ItemStack sapling, boolean persistentLeavesAreNatural) {
        return new WoodSpecies(matchingStates(log), matchingStates(leaf), sapling == null ? List.of() : matchingStates(sapling),
                persistentLeavesAreNatural, log, leaf, sapling);
    }

    private static List<BlockState> matchingStates(ItemStack stack) {
        var base = stateOf(stack);
        if (base == null) throw new IllegalArgumentException("A tree association requires a nonempty block item");
        var component = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        for (var entry : component.properties().entrySet()) {
            var property = base.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (property == null || property.getValue(entry.getValue()).isEmpty())
                throw new IllegalArgumentException("Invalid tree item state property: " + entry);
        }
        return allStates(base.getBlock()).stream().filter(state -> component.properties().keySet().stream().allMatch(name -> {
            var property = base.getBlock().getStateDefinition().getProperty(name);
            return transientProperty(property) || state.getValue(property).equals(base.getValue(property));
        })).toList();
    }

    private static List<BlockState> states(List<BlockState> states) {
        return List.copyOf(new LinkedHashSet<>(Objects.requireNonNull(states)));
    }

    private static List<BlockState> allStates(Block block) {
        var result = new LinkedHashSet<BlockState>();
        result.add(Objects.requireNonNull(block).defaultBlockState());
        result.addAll(block.getStateDefinition().getPossibleStates());
        return List.copyOf(result);
    }

    public List<BlockState> logs() { return logs; }
    public List<BlockState> leaves() { return leaves; }
    public List<BlockState> saplings() { return saplings; }
    public List<ItemStack> logItems() { return logTemplate == null ? items(logs) : List.of(logTemplate.copy()); }
    public List<ItemStack> leafItems() { return leafTemplate == null ? items(leaves) : List.of(leafTemplate.copy()); }
    public List<ItemStack> saplingItems() { return saplingTemplate == null ? items(saplings) : List.of(saplingTemplate.copy()); }

    public boolean isNaturalLeaf(BlockState state) {
        return leaves.contains(state) && (persistentLeavesAreNatural
                || !state.hasProperty(BlockStateProperties.PERSISTENT)
                || !state.getValue(BlockStateProperties.PERSISTENT));
    }

    /** Read only the native placement component; damage and display components are not species. */
    public static BlockState stateOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return null;
        return stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
                .apply(item.getBlock().defaultBlockState());
    }

    // Item prototypes become bound with loaded server data, after block registration. Do not
    // construct stacks while a species is registered or cache an early/empty item association.
    private static List<ItemStack> items(List<BlockState> states) {
        var result = new ArrayList<ItemStack>();
        for (var state : states) {
            var stack = new ItemStack(state.getBlock());
            if (stack.isEmpty()) continue; // Block-only variants have no obtainable block item.
            var component = BlockItemStateProperties.EMPTY;
            for (var property : state.getProperties()) {
                // These are world conditions/orientation, not an inventory species discriminator.
                if (transientProperty(property)) continue;
                component = with(component, property, state);
            }
            if (!component.isEmpty()) stack.set(DataComponents.BLOCK_STATE, component);
            if (result.stream().noneMatch(existing -> ItemStack.isSameItemSameComponents(existing, stack))) result.add(stack);
        }
        return List.copyOf(result);
    }

    private static boolean transientProperty(Property<?> property) {
        return property == BlockStateProperties.AXIS || property == BlockStateProperties.DISTANCE
                || property == BlockStateProperties.PERSISTENT || property == BlockStateProperties.WATERLOGGED
                || property == BlockStateProperties.STAGE;
    }

    private static <T extends Comparable<T>> BlockItemStateProperties with(
            BlockItemStateProperties component, Property<T> property, BlockState state) {
        return component.with(property, state);
    }
}
