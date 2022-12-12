package reika.dragonapi.instantiable.gui;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class SubviewableList<E> {

	public final int subviewSize;
	private final List<E> data;
	private int viewOffset;

	public SubviewableList(List<E> li, int s) {
		data = li;
		this.subviewSize = s;
	}

	public List<E> getVisibleSublist() {
		this.viewOffset = Math.min(this.viewOffset, this.getMaxOffset());
		List<E> ret = new ArrayList<>();
		if (data.isEmpty())
			return ret;
		int i0 = this.viewOffset;
		int i1 = i0 + this.clampedSize();
		for (int i = i0; i < i1; i++) {
			ret.add(data.get(i));
		}
		return ret;
	}

	public E getEntryAtRelativeIndex(int idx) {
		idx -= this.viewOffset;
		return this.data.get(idx);
	}

	public int getAbsoluteIndex(int rel) {
		return rel + this.viewOffset;
	}

	public void stepOffset(int d) {
		this.viewOffset = Mth.clamp(this.viewOffset + d, 0, this.getMaxOffset());
	}

	public int size() {
		return this.data.size();
	}

	public int clampedSize() {
		return Math.min(this.data.size(), this.subviewSize);
	}

	private int getMaxOffset() {
		return Math.max(0, data.size() - this.subviewSize);
	}

}
