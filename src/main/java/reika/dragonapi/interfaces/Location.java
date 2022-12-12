package reika.dragonapi.interfaces;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public interface Location {

    void writeToTag(CompoundTag data);

    CompoundTag writeToTag();

    void saveAdditional(String tag, CompoundTag NBT);

    double getDistanceTo(double x, double y, double z);

    Block getBlock(BlockGetter world);

    BlockEntity getBlockEntity(BlockGetter world);

    HitResult asMovingPosition(Direction s, Vec3 vec);

}
