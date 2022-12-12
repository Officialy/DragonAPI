package reika.dragonapi.interfaces;

import net.minecraft.nbt.CompoundTag;

public interface ObjectToNBTSerializer<V> {

    CompoundTag save(V obj);

    V construct(CompoundTag tag);

}
