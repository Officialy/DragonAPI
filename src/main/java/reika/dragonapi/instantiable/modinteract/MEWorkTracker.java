package reika.dragonapi.instantiable.modinteract;

import appeng.api.stacks.AEKey;
import reika.dragonapi.modinteract.deepinteract.MESystemReader.ChangeCallback;

/** 1.7.10 {@code MEWorkTracker}: flags work when the ME contents change, and anyway every {@link #tickRate} ticks. */
public class MEWorkTracker implements ChangeCallback {

	public final int tickRate;

	private long age = 0;
	private boolean hasWork = true;

	public MEWorkTracker() {
		this(200);
	}

	public MEWorkTracker(int r) {
		tickRate = r;
	}

	public void tick() {
		age++;
		hasWork |= age % tickRate == 0;
	}

	public void markDirty() {
		hasWork = true;
	}

	public void reset() {
		hasWork = false;
	}

	public boolean hasWork() {
		return hasWork;
	}

	@Override
	public void onItemChange(AEKey iae) {
		hasWork = true;
	}

}
