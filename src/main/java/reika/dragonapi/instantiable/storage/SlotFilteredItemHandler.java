package reika.dragonapi.instantiable.storage;

import java.util.function.IntPredicate;

import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * An automation-facing view of an item handler that only allows insertion into, and extraction from, chosen slots:
 * the modern form of 1.7.10's {@code ISidedInventory.canInsertItem/canExtractItem}. The owner's GUI keeps using the
 * unfiltered handler.
 */
public class SlotFilteredItemHandler extends DelegatingResourceHandler<ItemResource> {

	private final IntPredicate canInsert;
	private final IntPredicate canExtract;

	public SlotFilteredItemHandler(ResourceHandler<ItemResource> delegate, IntPredicate canInsert, IntPredicate canExtract) {
		super(delegate);
		this.canInsert = canInsert;
		this.canExtract = canExtract;
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return canInsert.test(index) && super.isValid(index, resource);
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return canInsert.test(index) ? super.insert(index, resource, amount, transaction) : 0;
	}

	@Override
	public int insert(ItemResource resource, int amount, TransactionContext transaction) {
		int inserted = 0;
		for (int i = 0; i < this.size() && inserted < amount; i++)
			inserted += this.insert(i, resource, amount - inserted, transaction);
		return inserted;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return canExtract.test(index) ? super.extract(index, resource, amount, transaction) : 0;
	}

	@Override
	public int extract(ItemResource resource, int amount, TransactionContext transaction) {
		int extracted = 0;
		for (int i = 0; i < this.size() && extracted < amount; i++)
			extracted += this.extract(i, resource, amount - extracted, transaction);
		return extracted;
	}

}
