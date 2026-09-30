package reika.dragonapi.instantiable.modinteract;

import java.util.HashSet;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import reika.dragonapi.ModList;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.maps.ItemHashMap;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * 1.7.10 {@code MENetwork}: a snapshot of the ME storage reachable from a block - either through a grid host's own
 * node, or by walking the AE blocks next to a position - that can be listed and pulled from.
 *
 * <p>1.7.10 flood-filled {@code AEBaseTile}s and collected every {@code ICellProvider}'s cell handlers. Modern AE2
 * merges all of that into the grid's {@link MEStorage}, so the walk now stops at the first AE node found and uses
 * its grid's storage; the listed blocks are that grid's in-world nodes.
 */
public class MENetwork {

	private final HashSet<BlockPos> blocks = new HashSet<>();
	private MEStorage storage;
	private final Level world;

	private MENetwork(Level world) {
		this.world = world;
		if (!ModList.APPENG.isLoaded()) {
			throw new MisuseException("How do you plan to create an ME network object without AE installed?!");
		}
	}

	public boolean isEmpty() {
		return blocks.isEmpty();
	}

	public int getSize() {
		return blocks.size();
	}

	public ItemHashMap<Long> getMEContents() {
		ItemHashMap<Long> cache = new ItemHashMap<Long>().enableNBT();
		if (storage == null)
			return cache;
		KeyCounter items = storage.getAvailableStacks();
		for (Object2LongMap.Entry<AEKey> item : items) {
			if (item.getKey() instanceof AEItemKey key) {
				cache.put(key.toStack(), item.getLongValue());
			}
		}
		return cache;
	}

	public int removeFromMESystem(ItemStack is, int diff) {
		int rem = 0;
		if (storage == null)
			return rem;
		KeyCounter items = storage.getAvailableStacks();
		for (Object2LongMap.Entry<AEKey> item : items) {
			if (item.getKey() instanceof AEItemKey iae && ReikaItemHelper.matchStacks(is, iae.getReadOnlyStack())) {
				int dec = (int)Math.min(item.getLongValue(), diff);
				long removed = storage.extract(iae, dec, Actionable.MODULATE, IActionSource.empty());
				if (removed > 0) {
					diff -= (int)removed;
					rem += (int)removed;
					if (diff <= 0)
						return rem;
				}
			}
		}
		return rem;
	}

	@Override
	public String toString() {
		return blocks.toString() + " > " + storage;
	}

	public static MENetwork getFromGridHost(BlockEntity te, IInWorldGridNodeHost host, Direction dir) {
		MENetwork net = new MENetwork(te.getLevel());
		IGridNode node = host.getGridNode(dir);
		if (node != null)
			net.populate(node.getGrid());
		return net;
	}

	public static MENetwork getFromGridHost(IGridNode ign) {
		MENetwork net = new MENetwork(ign.getLevel());
		net.populate(ign.getGrid());
		return net;
	}

	private void populate(@Nullable IGrid ig) {
		if (ig == null)
			return;
		storage = ig.getStorageService().getInventory();
		for (IGridNode n : ig.getNodes()) {
			if (n.getOwner() instanceof BlockEntity be && be.getLevel() == world)
				blocks.add(be.getBlockPos());
		}
	}

	public static MENetwork getConnectedTo(BlockEntity te) {
		return getConnectedTo(te.getLevel(), te.getBlockPos());
	}

	public static MENetwork getConnectedTo(Level world, BlockPos pos) {
		MENetwork net = new MENetwork(world);
		for (Direction dir : Direction.values()) {
			BlockPos p = pos.relative(dir);
			IInWorldGridNodeHost host = GridHelper.getNodeHost(world, p);
			if (host != null) {
				IGridNode node = host.getGridNode(dir.getOpposite());
				if (node != null && node.getGrid() != null) {
					net.populate(node.getGrid());
					break;
				}
			}
		}
		return net;
	}

}
