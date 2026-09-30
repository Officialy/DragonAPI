package reika.dragonapi.instantiable.modinteract;

import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IGridNodeService;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.AEKey;
import appeng.api.util.AECableType;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.interfaces.blockentity.MEGridHost;

/**
 * 1.7.10 {@code BasicAEInterface}: the {@code IGridBlock} a Reika machine handed to AE to become a channel-using
 * grid node, drawing {@link #setPowerCost a configurable idle power}, connectable on every side.
 *
 * <p>Modern AE2 replaced {@code IGridBlock}+{@code AEApi.createGridNode} with {@link IManagedGridNode}. This class
 * owns that node for its machine and is itself the machine's {@link IInWorldGridNodeHost} (exposed through the AE
 * capability by {@code AEHooks}) and {@link IActionHost} (the source machine for {@code MESystemReader}), since the
 * machine can no longer implement AE interfaces itself.
 *
 * <p>Lifecycle, mirroring AE2's own {@code AENetworkedBlockEntity}: call {@link #onReady()} from the machine's
 * {@code clearRemoved}, {@link #destroy()} from {@code setRemoved} and {@code onChunkUnloaded}, and
 * {@link #saveNode}/{@link #loadNode} from its save/load.
 */
public class BasicAEInterface implements IInWorldGridNodeHost, IActionHost, IGridNodeListener<BlockEntity> {

	private final BlockEntity tile;
	private final ItemStack item;
	private final IManagedGridNode node;
	private final StackWatcher watcher = new StackWatcher();

	private double powerCost = 1;
	private AECableType cableType = AECableType.GLASS;

	public BasicAEInterface(BlockEntity te, ItemStack is) {
		this(te, is, 1);
	}

	public BasicAEInterface(BlockEntity te, ItemStack is, double basePower) {
		if (!(te instanceof MEGridHost))
			throw new MisuseException("You cannot use a non-AE-gridHost block!");
		tile = te;
		item = is;
		powerCost = basePower;
		node = GridHelper.createManagedNode(te, this)
				.setFlags(GridFlags.REQUIRE_CHANNEL)
				.setIdlePowerUsage(basePower)
				.setInWorldNode(true)
				.setTagName("reika_ae_node")
				.addService(IStorageWatcherNode.class, watcher);
		if (is != null && !is.isEmpty())
			node.setVisualRepresentation(is);
		this.updateSides();
	}

	/** Adds a node service (eg an {@code ICraftingRequester}); must happen before the node is created in {@link #onReady()}. */
	public <T extends IGridNodeService> BasicAEInterface addService(Class<T> serviceClass, T service) {
		node.addService(serviceClass, service);
		return this;
	}

	public BasicAEInterface setCableType(AECableType type) {
		cableType = type;
		return this;
	}

	/** Schedules the node's creation for the first server tick. Call from the machine's {@code clearRemoved}. */
	public void onReady() {
		GridHelper.onFirstTick(tile, te -> node.create(te.getLevel(), te.getBlockPos()));
	}

	/** Call from the machine's {@code setRemoved} and {@code onChunkUnloaded}. */
	public void destroy() {
		node.destroy();
	}

	public void saveNode(ValueOutput output) {
		node.serialize(output);
	}

	public void loadNode(ValueInput input) {
		node.deserialize(input);
	}

	public void setPowerCost(double val) {
		if (val != powerCost) {
			powerCost = val;
			node.setIdlePowerUsage(val); //posts the idle-power-change grid event, as the 1.7.10 MENetworkPowerIdleChange did
		}
	}

	public double getIdlePowerUsage() {
		return powerCost;
	}

	public IManagedGridNode getManagedNode() {
		return node;
	}

	@Nullable
	public IGridNode getNode() {
		return node.getNode();
	}

	public ItemStack getMachineRepresentation() {
		return item;
	}

	public BlockEntity getMachine() {
		return tile;
	}

	/**
	 * V33a machines rebuilt their {@code MESystemReader} whenever their grid node changed, carrying the old reader's
	 * crafting jobs over. Returns the reader to keep: {@code old} if it is still on this node, a new one (with this
	 * machine as its action source) if the node changed, or null (old detached) if the node is gone.
	 * <p>Machines call this rather than constructing readers themselves, so their own bytecode never has to prove a
	 * {@code BasicAEInterface} is an {@code IActionHost} - which would load AE classes during verification.
	 */
	@Nullable
	public reika.dragonapi.modinteract.deepinteract.MESystemReader updateReader(@Nullable reika.dragonapi.modinteract.deepinteract.MESystemReader old) {
		IGridNode n = node.getNode();
		if (n == null) {
			if (old != null)
				old.detach();
			return null;
		}
		if (old != null && old.isOn(n))
			return old;
		return old == null ? new reika.dragonapi.modinteract.deepinteract.MESystemReader(n, this) : new reika.dragonapi.modinteract.deepinteract.MESystemReader(n, old);
	}

	/** V33a cable connection types: glass (default), covered, smart, dense... */
	public BasicAEInterface setCoveredCable() {
		return this.setCableType(AECableType.COVERED);
	}

	/** Receives this node's storage change notifications; see {@code MESystemReader.addCallback}. */
	public StackWatcher getWatcher() {
		return watcher;
	}

	protected Set<Direction> getConnectableSides() {
		return EnumSet.allOf(Direction.class);
	}

	protected final void updateSides() {
		node.setExposedOnSides(this.getConnectableSides());
	}

	@Nullable
	@Override
	public IGridNode getGridNode(Direction dir) {
		return node.getNode();
	}

	@Override
	public AECableType getCableConnectionType(Direction dir) {
		return cableType;
	}

	@Nullable
	@Override
	public IGridNode getActionableNode() {
		return node.getNode();
	}

	@Override
	public void onSaveChanges(BlockEntity nodeOwner, IGridNode node) {
		nodeOwner.setChanged();
	}

	/**
	 * AE2 pushes storage changes to {@link IStorageWatcherNode} services instead of 1.7.10's
	 * {@code IMEMonitorHandlerReceiver}; readers built on this node subscribe here.
	 */
	public static final class StackWatcher implements IStorageWatcherNode {

		private IStackWatcher watcher;
		private final java.util.List<Listener> listeners = new java.util.ArrayList<>();

		public void addListener(Listener l) {
			if (!listeners.contains(l))
				listeners.add(l);
			if (watcher != null)
				watcher.setWatchAll(true);
		}

		public void removeListener(Listener l) {
			listeners.remove(l);
			if (watcher != null && listeners.isEmpty())
				watcher.setWatchAll(false);
		}

		@Override
		public void updateWatcher(IStackWatcher newWatcher) {
			watcher = newWatcher;
			watcher.setWatchAll(!listeners.isEmpty());
		}

		@Override
		public void onStackChange(AEKey what, long amount) {
			for (Listener l : listeners)
				l.onStackChange(what, amount);
		}

		public interface Listener {
			void onStackChange(AEKey what, long amount);
		}

	}

}
