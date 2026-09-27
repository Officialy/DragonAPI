package reika.dragonapi.modinteract.power;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * A transactional FE store owned by a block entity, which is marked changed whenever the stored
 * amount changes. {@link SimpleEnergyHandler} reports external insertions and extractions once,
 * when their root transaction commits, so a simulated or aborted transfer never dirties the owner.
 */
public class ReikaEnergyStorage extends SimpleEnergyHandler {

    public final BlockEntity blockEntity;

    public ReikaEnergyStorage(int capacity, int maxReceive, int maxExtract, BlockEntity blockEntity) {
        super(capacity, maxReceive, maxExtract);
        this.blockEntity = blockEntity;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        blockEntity.setChanged();
    }

    public void setEnergy(int energy) {
        this.set(energy);
    }
}
