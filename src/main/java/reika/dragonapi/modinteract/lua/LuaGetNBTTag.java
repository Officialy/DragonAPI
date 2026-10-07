///*******************************************************************************
// * @author Reika Kalseki
// *
// * Copyright 2017
// *
// * All rights reserved.
// * Distribution of the software in any form is only allowed with
// * explicit, prior permission from the owner.
// ******************************************************************************/
package reika.dragonapi.modinteract.lua;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LuaGetNBTTag extends LuaMethod {

	public LuaGetNBTTag() {
		super("getNBTTag", BlockEntity.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		if (te.getLevel() == null)
			throw new LuaMethodException("Block entity is not in a world");
		//1.7.10 te.writeToNBT(nbt)
		CompoundTag nbt = te.saveWithoutMetadata(te.getLevel().registryAccess());
		Object o = null;
		String tag = (String)args[0];
		Tag b = nbt.get(tag);
		if (b != null) {
			o = switch (b) {
				case ByteTag t -> t.value();
				case DoubleTag t -> t.value();
				case FloatTag t -> t.value();
				case IntTag t -> t.value();
				case LongTag t -> t.value();
				case ShortTag t -> t.value();
				case StringTag t -> t.value();
				case IntArrayTag t -> t.getAsIntArray();
				case ByteArrayTag t -> t.getAsByteArray();
				//not an NBT type in 1.7.10; returned as an array like the other two array tags
				case LongArrayTag t -> t.getAsLongArray();
				//1.7.10 LIST and COMPOUND: the tag's string form
				case CollectionTag t -> t.toString();
				case CompoundTag t -> t.toString();
				default -> null;
			};
		}
		return o != null ? new Object[]{o} : null;
	}

	@Override
	public String getDocumentation() {
		return "Returns the value of an NBT tag.\nArgs: tagName\nReturns: tagValue";
	}

	@Override
	public String getArgsAsString() {
		return "String tagName";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.ARRAY;
	}

}
