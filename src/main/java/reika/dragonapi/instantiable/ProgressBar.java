package reika.dragonapi.instantiable;

import net.minecraft.nbt.CompoundTag;

public class ProgressBar {

    private int tick;
    private int duration;

    private DurationCallback durationCall;

    public ProgressBar(int dur) {
        duration = dur;
    }

    public ProgressBar(DurationCallback call) {
        durationCall = call;
    }

    public boolean tick() {
        return this.tick(1);
    }

    public boolean tick(int amt) {
        this.updateDuration();
        if (tick + amt >= duration) {
            tick = (tick + amt) % duration;
            return true;
        } else {
            tick += amt;
            return false;
        }
    }

    public int tickNoRollover() {
        return this.tickNoRollover(1);
    }

    public int tickNoRollover(int amt) {
        this.updateDuration();
        int max = Math.min(amt, duration - tick);
        tick += max;
        return max;
    }

    public boolean isComplete() {
        this.updateDuration();
        return tick >= duration;
    }

    public int getScaled(int len) {
        this.updateDuration();
        return tick * len / duration;
    }

    private void updateDuration() {
        if (durationCall != null) {
            duration = durationCall.getDuration();
        }
    }

    public int getTick() {
        return tick;
    }

    public int getDuration() {
        return duration;
    }

    public void saveAdditional(CompoundTag tag) {
        tag.putInt("duration", duration);
        tag.putInt("tick", tick);
    }

    public void load(CompoundTag tag) {
        duration = tag.getInt("duration");
        tick = tag.getInt("tick");
    }

    public interface DurationCallback {

        int getDuration();

    }

}
