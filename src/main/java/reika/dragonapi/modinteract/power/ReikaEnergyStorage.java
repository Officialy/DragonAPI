package reika.dragonapi.modinteract.power;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.EnergyStorage;

public class ReikaEnergyStorage extends EnergyStorage {

    public final BlockEntity blockEntity;

    public ReikaEnergyStorage(int capacity, int maxReceive, int maxTransfer, BlockEntity blockEntity) {
        super(capacity, maxReceive, maxTransfer);
        this.blockEntity = blockEntity;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0) {
            blockEntity.setChanged();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0) {
            blockEntity.setChanged();
        }
        return extracted;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }
}
