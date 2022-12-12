package reika.dragonapi.instantiable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;

public class MultiStageProgressBar {

    private final ArrayList<ProgressBar> bars = new ArrayList<>();

    public MultiStageProgressBar() {

    }

    public MultiStageProgressBar addBar(int dur) {
        return this.addBar(new ProgressBar(dur));
    }

    public MultiStageProgressBar addBar(ProgressBar.DurationCallback b) {
        return this.addBar(new ProgressBar(b));
    }

    public MultiStageProgressBar addBar(ProgressBar b) {
        bars.add(b);
        return this;
    }

    public boolean tick() {
        return this.tick(1);
    }

    public boolean tick(int amt) {
        for (int i = 0; i < bars.size(); i++) {
            ProgressBar b = bars.get(i);
            int ticked = b.tickNoRollover(amt);
            amt -= ticked;
            if (i == bars.size() - 1 && b.isComplete())
                return true;
            if (amt <= 0)
                return false;
        }
        return false;
    }

    public int getScaledBar(int slot, int len) {
        return bars.get(slot).getScaled(len);
    }

    public int getTick(int slot) {
        return bars.get(slot).getTick();
    }

    public void saveAdditional(CompoundTag nbt) {
        ListTag li = new ListTag();
        for (ProgressBar b : bars) {
            CompoundTag tag = new CompoundTag();
            b.saveAdditional(tag);
            li.add(tag);
        }
        nbt.put("bars", li);
    }

    public void load(CompoundTag nbt) {
        bars.clear();
        ListTag li = nbt.getList("bars", Tag.TAG_COMPOUND);
        for (Object o : li) {  //li.tagList -- TODO might be wrong
            CompoundTag tag = (CompoundTag) o;
            ProgressBar b = new ProgressBar(0);
            b.load(tag);
            this.addBar(b);
        }
    }

}
