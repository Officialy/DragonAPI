package reika.dragonapi.modinteract.deepinteract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.hooks.ticking.TickHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.data.KeyedItemStack;
import reika.dragonapi.instantiable.data.maps.ItemHashMap;
import reika.dragonapi.instantiable.data.maps.MultiMap;
import reika.dragonapi.instantiable.modinteract.BasicAEInterface;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * 1.7.10 {@code MESystemReader}: a machine's (or player's) view of the ME network its grid node is on - read the
 * contents, insert and extract (exact, NBT-blind or fuzzy), request autocrafting, and get told when watched items
 * change. Also hosts the global {@link MESystemEffect} registry that runs effects against every grid.
 *
 * <p>Ported to AE2's modern API: {@code IStorageGrid}/{@code IMEMonitor}/{@code IAEItemStack} became
 * {@link IStorageService}/{@link MEStorage}/{@link AEItemKey}, the crafting job future became an
 * {@link ICraftingPlan} future, and the {@code IMEMonitorHandlerReceiver} listener became the node's
 * {@link BasicAEInterface.StackWatcher} (with a polling fallback for nodes Reika does not own). Damage-wildcard
 * matching has no modern equivalent (damage is a component); NBT-blind matching now means component-blind.
 */
public class MESystemReader implements BasicAEInterface.StackWatcher.Listener {

	private static final Collection<MESystemEffect> systemEffects = new ArrayList<>();

	private static final Comparator<Object2LongMap.Entry<AEKey>> sizeSorter = (o1, o2) -> -Long.compare(o1.getLongValue(), o2.getLongValue());

	private final IGridNode node;
	private final IActionSource actionSource;

	private ICraftingRequester requester = null;

	private final IdentityHashMap<Future<ICraftingPlan>, CraftCompleteCallback> crafting = new IdentityHashMap<>();
	private final MultiMap<CraftCompleteCallback, ICraftingLink> craftingLinks = new MultiMap<CraftCompleteCallback, ICraftingLink>().setNullEmpty();
	private final MultiMap<KeyedItemStack, ChangeCallback> changeCallbacks = new MultiMap<>();
	private final ArrayList<ChangeCallback> globalChangeCallbacks = new ArrayList<>();

	/** Polling fallback state, used when the node has no {@link BasicAEInterface.StackWatcher} to push changes. */
	private final HashMap<KeyedItemStack, Long> lastCounts = new HashMap<>();
	private long lastSignature = Long.MIN_VALUE;
	private BasicAEInterface.StackWatcher watcher;

	public final boolean isEmpty;

	public MESystemReader(IGridNode ign, Player ep) {
		this(ign, IActionSource.ofPlayer(ep));
	}

	public MESystemReader(IGridNode ign, IActionHost iah) {
		this(ign, IActionSource.ofMachine(iah));
	}

	/** For loading all the data of the old reader */
	public MESystemReader(IGridNode ign, MESystemReader reader) {
		this(ign, reader.actionSource);

		crafting.putAll(reader.crafting);
		for (CraftCompleteCallback ccc : reader.craftingLinks.keySet()) {
			for (ICraftingLink l : reader.craftingLinks.get(ccc))
				craftingLinks.addValue(ccc, l);
		}

		requester = reader.requester;
		reader.detach();
	}

	private MESystemReader(IGridNode ign, IActionSource src) {
		node = ign;
		actionSource = src;

		IGrid ig = ign != null ? ign.getGrid() : null;
		if (ig == null) {
			isEmpty = true;
			return;
		}
		Iterator<IGridNode> iterator = ig.getNodes().iterator();
		isEmpty = !iterator.hasNext() || (iterator.next() == node && ig.size() == 1);
	}

	public MESystemReader setRequester(ICraftingRequester icr) {
		requester = icr;
		return this;
	}

	public MESystemReader addCallback(ItemStack is, ChangeCallback call) {
		changeCallbacks.addValue(new KeyedItemStack(is).setSimpleHash(true).setIgnoreNBT(true), call);
		this.attach();
		return this;
	}

	public MESystemReader addGlobalCallback(ChangeCallback call) {
		globalChangeCallbacks.add(call);
		this.attach();
		return this;
	}

	public void clearCallbacks() {
		changeCallbacks.clear();
		globalChangeCallbacks.clear();
		lastCounts.clear();
		this.detach();
	}

	private void attach() {
		if (watcher == null && node != null) {
			BasicAEInterface.StackWatcher w = node.getService(appeng.api.networking.storage.IStorageWatcherNode.class) instanceof BasicAEInterface.StackWatcher sw ? sw : null;
			if (w != null) {
				watcher = w;
				watcher.addListener(this);
			}
		}
	}

	/** Stops receiving storage notifications; called when a newer reader replaces this one. */
	public void detach() {
		if (watcher != null) {
			watcher.removeListener(this);
			watcher = null;
		}
	}

	@Nullable
	private MEStorage getStorage() {
		if (node == null || node.getGrid() == null)
			return null;
		IStorageService isg = node.getGrid().getStorageService();
		return isg != null ? isg.getInventory() : null;
	}

	@Nullable
	private KeyCounter getCachedContents() {
		if (node == null || node.getGrid() == null)
			return null;
		IStorageService isg = node.getGrid().getStorageService();
		return isg != null ? isg.getCachedInventory() : null;
	}

	@Nullable
	private ICraftingService getCraftingGrid() {
		if (node == null || node.getGrid() == null)
			return null;
		return node.getGrid().getCraftingService();
	}

	/** Now NBT-Sensitive */
	public ItemHashMap<Long> getMESystemContents() {
		ItemHashMap<Long> map = new ItemHashMap<Long>().enableNBT();
		KeyCounter kc = this.getCachedContents();
		if (kc == null)
			return map;
		for (Object2LongMap.Entry<AEKey> e : kc) {
			if (e.getKey() instanceof AEItemKey key && e.getLongValue() > 0) {
				map.put(key.toStack(), e.getLongValue());
			}
		}
		return map;
	}

	public Collection<ItemStack> getRawMESystemContents() {
		Collection<ItemStack> c = new ArrayList<>();
		KeyCounter kc = this.getCachedContents();
		if (kc == null)
			return c;
		for (Object2LongMap.Entry<AEKey> e : kc) {
			if (e.getKey() instanceof AEItemKey key && e.getLongValue() > 0) {
				c.add(key.toStack((int)Math.min(Integer.MAX_VALUE, e.getLongValue())));
			}
		}
		return c;
	}

	@Nullable
	public static AEItemKey createAEStack(ItemStack is) {
		return AEItemKey.of(is);
	}

	/** Returns how many items removed */
	public long removeItem(ItemStack is, boolean simulate, boolean nbt) {
		if (is == null || is.isEmpty() || this.getStorage() == null)
			return 0;
		if (!nbt) {
			KeyCounter kc = this.getCachedContents();
			if (kc == null)
				return 0;
			AEItemKey most = null;
			long mostAmt = 0;
			for (Object2LongMap.Entry<AEKey> e : kc) {
				if (e.getKey() instanceof AEItemKey key && key.getItem() == is.getItem() && (most == null || e.getLongValue() >= mostAmt)) {
					most = key;
					mostAmt = e.getLongValue();
				}
			}
			if (most == null)
				return 0;
			ExtractedItem ei = this.extract(most, is.getCount(), simulate);
			return ei != null ? ei.amount : 0;
		}
		ExtractedItem ei = this.extract(createAEStack(is), is.getCount(), simulate);
		return ei != null ? ei.amount : 0;
	}

	/** Returns how many items removed. But Fuzzy! :D
	 * @param allowMultiple */
	public ExtractedItemGroup removeItemFuzzy(ItemStack is, boolean simulate, FuzzyMode fz, boolean oredict, boolean nbt, boolean allowMultiple) {
		if (is == null || is.isEmpty() || this.getStorage() == null)
			return null;
		ArrayList<Object2LongMap.Entry<AEKey>> li = new ArrayList<>();
		for (Object2LongMap.Entry<AEKey> e : this.getFuzzyItemList(is, fz, oredict)) {
			AEItemKey iae = (AEItemKey)e.getKey();
			if ((oredict || is.getItem() == iae.getItem()) && (!nbt || ItemStack.isSameItemSameComponents(is, iae.getReadOnlyStack()))) {
				li.add(e);
			}
		}
		li.sort(sizeSorter);
		long wanted = is.getCount();
		ExtractedItemGroup ret = new ExtractedItemGroup();
		for (Object2LongMap.Entry<AEKey> e : li) {
			long amt = Math.min(wanted, e.getLongValue());
			ExtractedItem ei = this.extract((AEItemKey)e.getKey(), amt, simulate);
			if (ei == null)
				continue;
			ret.addItem(ei);
			wanted -= Math.min(amt, ei.amount);
			if (wanted <= 0)
				break;
			if (!allowMultiple)
				break;
		}
		return ret.isEmpty() ? null : ret;
	}

	/** The item keys fuzzy-matching the stack, with how many of each can actually be extracted right now. */
	private Collection<Object2LongMap.Entry<AEKey>> getFuzzyItemList(ItemStack is, FuzzyMode fz, boolean oredict) {
		KeyCounter kc = this.getCachedContents();
		Collection<Object2LongMap.Entry<AEKey>> c2 = new ArrayList<>();
		if (kc == null)
			return c2;
		AEItemKey ae = createAEStack(is);
		HashSet<AEKey> found = new HashSet<>();
		for (Object2LongMap.Entry<AEKey> e : kc.findFuzzy(ae, fz))
			found.add(e.getKey());
		if (oredict) { //1.7.10 AE's fuzzy list also matched ore dictionary equivalents; the modern analogue is a shared common tag
			for (Object2LongMap.Entry<AEKey> e : kc) {
				if (e.getKey() instanceof AEItemKey key && sharesCommonTag(is, key.getReadOnlyStack()))
					found.add(key);
			}
		}
		KeyCounter ret = new KeyCounter();
		for (AEKey key : found) {
			if (!(key instanceof AEItemKey iae))
				continue;
			ExtractedItem ei = this.extract(iae, Long.MAX_VALUE, true);
			if (ei != null)
				ret.add(iae, ei.amount);
		}
		for (Object2LongMap.Entry<AEKey> e : ret)
			c2.add(e);
		return c2;
	}

	@Nullable
	private ExtractedItem extract(AEItemKey ae, long amount, boolean simulate) {
		MEStorage mon = this.getStorage();
		if (ae == null || mon == null || amount <= 0)
			return null;
		long got = mon.extract(ae, amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE, actionSource);
		return got > 0 ? new ExtractedItem(ae.toStack(), got) : null;
	}

	/** Returns how many items NOT added */
	public long addItem(ItemStack is, boolean simulate) {
		MEStorage mon = this.getStorage();
		if (is == null || is.isEmpty())
			return 0;
		if (mon == null)
			return is.getCount();
		long added = mon.insert(createAEStack(is), is.getCount(), simulate ? Actionable.SIMULATE : Actionable.MODULATE, actionSource);
		return is.getCount() - added;
	}

	public long getItemCount(ItemStack is, boolean nbt) {
		return this.removeItem(ReikaItemHelper.getSizedItemStack(is, Integer.MAX_VALUE), true, nbt);
	}

	public long getFuzzyItemCount(ItemStack is, FuzzyMode fz, boolean ore, boolean nbt) {
		ExtractedItemGroup ei = this.removeItemFuzzy(ReikaItemHelper.getSizedItemStack(is, Integer.MAX_VALUE), true, fz, ore, nbt, true);
		return ei != null ? ei.count() : 0;
	}

	public void triggerFuzzyCrafting(Level world, ItemStack is, long amt, CalculationCallback callback, CraftCompleteCallback callback2) {
		this.triggerCrafting(world, is, amt, callback, callback2);
	}

	/** Triggers the native crafting system to craft a given amount of a given item. Callbacks are optional. */
	public void triggerCrafting(Level world, ItemStack is, long amt, CalculationCallback callback, CraftCompleteCallback callback2) {
		ICraftingService cache = this.getCraftingGrid();
		if (cache == null)
			return;
		AEItemKey iae = createAEStack(is);
		if (iae == null)
			return;
		ICraftingSimulationRequester sim = () -> actionSource;
		Future<ICraftingPlan> f = cache.beginCraftingCalculation(world, sim, iae, amt, CalculationStrategy.REPORT_MISSING_ITEMS);
		if (callback != null)
			calculationCallbacks.put(f, callback);
		//1.7.10 tracked every job so the link could be submitted on tick, even with no completion callback
		crafting.put(f, callback2);
	}

	private final IdentityHashMap<Future<ICraftingPlan>, CalculationCallback> calculationCallbacks = new IdentityHashMap<>();

	/** You are required to call this on your reader if you want things like crafting triggers or change callbacks to work. */
	public void tick() {
		ICraftingService cache = this.getCraftingGrid();
		if (cache != null) {

			HashSet<Future<ICraftingPlan>> removeCalls = new HashSet<>();
			for (Future<ICraftingPlan> f : crafting.keySet()) {
				if (f.isDone()) {
					try {
						ICraftingPlan job = f.get();
						CalculationCallback calc = calculationCallbacks.remove(f);
						if (calc != null)
							calc.calculationComplete(job);
						if (!job.simulation()) {
							ICraftingSubmitResult res = cache.submitJob(job, requester, null, true, actionSource);
							ICraftingLink l = res.link();
							if (l == null) {
								DragonAPI.LOGGER.error(job + " to craft " + job.finalOutput() + " returned a null link! (" + res.errorCode() + ")");
							}
							else {
								CraftCompleteCallback ccc = crafting.get(f);
								if (ccc != null) {
									craftingLinks.addValue(ccc, l);
									ccc.onCraftingLinkReturned(l);
								}
							}
						}
						removeCalls.add(f);
					}
					catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
						removeCalls.add(f);
					}
				}
			}
			for (Future<ICraftingPlan> f : removeCalls)
				crafting.remove(f);

			MultiMap<CraftCompleteCallback, ICraftingLink> removeLinks = new MultiMap<CraftCompleteCallback, ICraftingLink>().setNullEmpty();
			for (CraftCompleteCallback ccc : craftingLinks.keySet()) {
				Collection<ICraftingLink> c = craftingLinks.get(ccc);
				if (c != null && !c.isEmpty()) {
					for (ICraftingLink l : c) {
						if (l.isDone()) {
							ccc.onCraftingComplete(l);
							removeLinks.addValue(ccc, l);
						}
					}
				}
			}
			for (CraftCompleteCallback ccc : removeLinks.keySet()) {
				Collection<ICraftingLink> c = removeLinks.get(ccc);
				if (c != null) {
					for (ICraftingLink l : c) {
						craftingLinks.remove(ccc, l);
					}
				}
			}
		}

		if (watcher == null && (!changeCallbacks.isEmpty() || !globalChangeCallbacks.isEmpty()))
			this.pollChanges();
	}

	/** Fallback for nodes with no push watcher: compare against the last seen amounts. */
	private void pollChanges() {
		KeyCounter kc = this.getCachedContents();
		if (kc == null)
			return;
		if (!globalChangeCallbacks.isEmpty()) {
			long sig = kc.size();
			for (Object2LongMap.Entry<AEKey> e : kc)
				sig = sig * 31 + e.getKey().hashCode() * 17L + e.getLongValue();
			if (sig != lastSignature) {
				boolean first = lastSignature == Long.MIN_VALUE;
				lastSignature = sig;
				if (!first) {
					for (ChangeCallback cc : globalChangeCallbacks)
						cc.onItemChange(null);
				}
			}
		}
		for (KeyedItemStack ks : changeCallbacks.keySet()) {
			long amt = 0;
			ItemStack ref = ks.getItemStack();
			for (Object2LongMap.Entry<AEKey> e : kc) {
				if (e.getKey() instanceof AEItemKey key && key.getItem() == ref.getItem())
					amt += e.getLongValue();
			}
			Long prev = lastCounts.put(ks, amt);
			if (prev != null && prev != amt) {
				for (ChangeCallback cc : changeCallbacks.get(ks))
					cc.onItemChange(createAEStack(ref));
			}
		}
	}

	@Override
	public void onStackChange(AEKey what, long amount) {
		for (ChangeCallback cc : globalChangeCallbacks) {
			cc.onItemChange(null);
		}
		if (what instanceof AEItemKey iae) {
			KeyedItemStack ks = new KeyedItemStack(iae.toStack()).setSimpleHash(true).setIgnoreNBT(true);
			Collection<ChangeCallback> c = changeCallbacks.get(ks);
			if (c != null) {
				for (ChangeCallback cc : c) {
					cc.onItemChange(iae);
				}
			}
		}
	}

	/** The 1.7.10 "ore dictionary overlap": both items carry the same {@code c:} (common) tag. */
	public static boolean sharesCommonTag(ItemStack is1, ItemStack is2) {
		if (is1.isEmpty() || is2.isEmpty())
			return false;
		return is1.getItem().builtInRegistryHolder().tags().anyMatch(t -> "c".equals(t.location().getNamespace()) && is2.is((TagKey<Item>)t));
	}

	public static Collection<IGrid> getAllMENetworks() {
		try {
			return new ArrayList<>(TickHandler.instance().getGridList());
		}
		catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	public static void registerMESystemEffect(MESystemEffect effect) {
		systemEffects.add(effect);
	}

	/** 1.7.10 {@code EffectHandler}: server tick end, run each effect on every non-empty grid at its frequency. */
	public static void tickEffects(MinecraftServer server) {
		if (systemEffects.isEmpty())
			return;
		ServerLevel overworld = server.overworld();
		if (overworld == null)
			return;
		Collection<IGrid> grids = null;
		long tick = overworld.getGameTime();
		for (MESystemEffect effect : systemEffects) {
			if (tick % effect.getTickFrequency() == 0) {
				if (grids == null) {
					grids = getAllMENetworks();
				}
				for (IGrid g : grids) {
					if (!g.isEmpty()) {
						effect.performEffect(g);
					}
				}
			}
		}
	}

	public interface CraftCompleteCallback {

		void onCraftingLinkReturned(ICraftingLink link);
		void onCraftingComplete(ICraftingLink link);

	}

	/** 1.7.10 {@code ICraftingCallback}: told when the crafting calculation finishes. */
	public interface CalculationCallback {

		void calculationComplete(ICraftingPlan job);

	}

	public interface ChangeCallback {

		/** May be null if a total AE list rebuild. Assume that means what you care about changed. */
		void onItemChange(@Nullable AEKey iae);

	}

	public enum MatchMode {
		EXACT(0xffcc00, "Exact Match"),
		EXACTNONBT(0xA5FF00, "Exact Match, Ignore NBT"),
		FUZZY(0x00aaff, "Fuzzy Match"),
		FUZZYORE(0x00ff00, "Fuzzy/Ore Match"),
		FUZZYNBT(0xaa00ff, "Fuzzy/Ore Match, Ignore NBT");

		public final int color;
		public final String desc;

		public static final MatchMode[] list = values();

		MatchMode(int c, String s) {
			color = c;
			desc = s;
		}

		public long countItems(MESystemReader net, ItemStack is) {
			return switch (this) {
				case EXACT -> net.getItemCount(is, true);
				case EXACTNONBT -> net.getItemCount(is, false);
				case FUZZY -> net.getFuzzyItemCount(is, FuzzyMode.IGNORE_ALL, false, true);
				case FUZZYORE -> net.getFuzzyItemCount(is, FuzzyMode.IGNORE_ALL, true, true);
				case FUZZYNBT -> net.getFuzzyItemCount(is, FuzzyMode.IGNORE_ALL, true, false);
			};
		}

		public ExtractedItemGroup removeItems(MESystemReader net, ItemStack is, boolean simulate, boolean allowMultiple) {
			switch (this) {
				case EXACT: {
					long amt = net.removeItem(is, simulate, true);
					return amt != 0 ? new ExtractedItemGroup(new ExtractedItem(is, amt)) : null;
				}
				case EXACTNONBT: {
					long amt = net.removeItem(is, simulate, false);
					return amt != 0 ? new ExtractedItemGroup(new ExtractedItem(is, amt)) : null;
				}
				case FUZZY:
					return net.removeItemFuzzy(is, simulate, FuzzyMode.IGNORE_ALL, false, true, allowMultiple);
				case FUZZYORE:
					return net.removeItemFuzzy(is, simulate, FuzzyMode.IGNORE_ALL, true, true, allowMultiple);
				case FUZZYNBT:
					return net.removeItemFuzzy(is, simulate, FuzzyMode.IGNORE_ALL, true, false, allowMultiple);
			}
			return null;
		}

		public MatchMode next() {
			return list[(this.ordinal() + 1) % list.length];
		}

		public boolean compare(ItemStack is1, ItemStack is2) {
			return switch (this) {
				case EXACT -> ReikaItemHelper.matchStacks(is1, is2) && ItemStack.isSameItemSameComponents(is1, is2);
				case EXACTNONBT -> ReikaItemHelper.matchStacks(is1, is2);
				case FUZZY -> is1.getItem() == is2.getItem() && ItemStack.isSameItemSameComponents(is1, is2);
				case FUZZYNBT -> is1.getItem() == is2.getItem();
				case FUZZYORE -> is1.getItem() == is2.getItem() || sharesCommonTag(is1, is2);
			};
		}
	}

	public static final class ExtractedItem {

		private final ItemStack item;
		public final long amount;

		private ExtractedItem(ItemStack is, long amt) {
			item = is;
			amount = amt;
		}

		public ItemStack getItem() {
			return ReikaItemHelper.getSizedItemStack(item, (int)Math.min(Integer.MAX_VALUE, amount));
		}

	}

	public static final class ExtractedItemGroup {

		private final ArrayList<ExtractedItem> items = new ArrayList<>();

		private ExtractedItemGroup() {

		}

		private ExtractedItemGroup(ExtractedItem... li) {
			for (ExtractedItem ei : li)
				this.addItem(ei);
		}

		private void addItem(ExtractedItem ei) {
			items.add(ei);
		}

		public long count() {
			long ret = 0;
			for (ExtractedItem ei : items) {
				ret += ei.amount;
			}
			return ret;
		}

		public boolean isEmpty() {
			return items.isEmpty();
		}

		public ExtractedItem getBiggest() {
			ExtractedItem ret = null;
			for (ExtractedItem ei : items) {
				if (ret == null || ei.amount > ret.amount) {
					ret = ei;
				}
			}
			return ret;
		}

		public Collection<ExtractedItem> getItems() {
			return Collections.unmodifiableCollection(items);
		}

	}

	public interface MESystemEffect {

		void performEffect(IGrid grid);

		/** The smaller you make this, the more computationally expensive it becomes. */
		int getTickFrequency();

	}

	public static abstract class ItemInSystemEffect implements MESystemEffect {

		private final ItemStack itemKey;
		private final boolean anyVariant;

		public ItemInSystemEffect(ItemStack is) {
			this(is, false);
		}

		/** @param anyVariant count every stack of the item regardless of components (1.7.10's {@code WILDCARD_VALUE} damage) */
		public ItemInSystemEffect(ItemStack is, boolean anyVariant) {
			itemKey = is;
			this.anyVariant = anyVariant;
		}

		@Override
		public final void performEffect(IGrid grid) {
			if (grid.isEmpty() || !grid.getNodes().iterator().hasNext())
				return;
			MESystemReader me = null;
			Iterator<IActionHost> iah = grid.getMachines(IActionHost.class).iterator();
			try {
				if (iah.hasNext()) {
					IActionHost ia = iah.next();
					if (ia.getActionableNode() != null)
						me = new MESystemReader(ia.getActionableNode(), ia);
				}
				if (me == null) {
					IGridNode ign2 = grid.getPivot();
					if (ign2 == null)
						ign2 = grid.getNodes().iterator().next();
					IGridNode fake = ign2;
					me = new MESystemReader(fake, (IActionHost)() -> fake);
				}
			}
			catch (Exception e) {
				DragonAPI.LOGGER.error("Detected invalid ME system when running " + this + ": " + grid.getNodes() + "\n; Threw exception on access: ", e);
			}
			if (me == null)
				return;
			long amt;
			if (anyVariant) { //count every variant, not just the largest single stack the NBT-blind extract finds
				amt = 0;
				KeyCounter kc = me.getCachedContents();
				if (kc != null) {
					for (Object2LongMap.Entry<AEKey> e : kc) {
						if (e.getKey() instanceof AEItemKey key && key.getItem() == itemKey.getItem())
							amt += e.getLongValue();
					}
				}
			}
			else {
				amt = me.getItemCount(itemKey, !itemKey.getComponentsPatch().isEmpty());
			}
			if (amt > 0) {
				this.doEffect(grid, amt);
			}
		}

		protected abstract void doEffect(IGrid grid, long amt);

	}
}
