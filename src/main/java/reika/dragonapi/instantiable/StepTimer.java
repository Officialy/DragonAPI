/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable;

import reika.dragonapi.libraries.java.ReikaRandomHelper;
import net.minecraft.nbt.CompoundTag;

import java.util.Random;

public final class StepTimer {

	private int value;
	private int cap;

	public StepTimer(int top) {
		cap = top;
	}

	public StepTimer stagger() {
		value = ReikaRandomHelper.getSafeRandomInt(cap);
		return this;
	}

	public void update() {
		value++;
	}

	public void update(int time) {
		value += time;
	}

	public boolean isAtCap() {
		return value >= cap;
	}

	public boolean checkCap() {
		boolean cap = this.isAtCap();
		if (cap)
			this.reset();
		return cap;
	}

	public void reset() {
		value = 0;
	}

	public void randomizeTick(Random r) {
		this.setTick(r.nextInt(cap));
	}

	public int getTick() {
		return value;
	}

	public void setTick(int tick) {
		value = tick;
	}

	public int getCap() {
		return cap;
	}

	public StepTimer setCap(int val) {
		cap = val;
		return this;
	}

	public float getFraction() {
		return (float) value / (float) cap;
	}

	@Override
	public String toString() {
		return "Timer @ " + value + "/" + cap;
	}

	protected void writeSyncTag(CompoundTag NBT, String id) {
		NBT.putInt(id + "cap", cap);
		NBT.putInt(id + "tick", value);
	}

	protected void readSyncTag(CompoundTag NBT, String id) {
		cap = NBT.getInt(id + "cap");
		value = NBT.getInt(id + "tick");
	}

}
