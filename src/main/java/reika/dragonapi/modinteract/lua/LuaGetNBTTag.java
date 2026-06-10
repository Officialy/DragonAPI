///*******************************************************************************
// * @author Reika Kalseki
// *
// * Copyright 2017
// *
// * All rights reserved.
// * Distribution of the software in any form is only allowed with
// * explicit, prior permission from the owner.
// ******************************************************************************/
//package reika.dragonapi.modinteract.lua;
//
//import net.minecraft.nbt.CompoundTag;
//
//
//import net.minecraft.nbt.Tag;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import reika.dragonapi.libraries.ReikaNBTHelper;
//
//public class LuaGetNBTTag extends LuaMethod {
//
//	public LuaGetNBTTag() {
//		super("getNBTTag", BlockEntity.class);
//	}
//
//	@Override
//	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
//		CompoundTag nbt = new CompoundTag();
//		te.load(nbt);
//		Object o = null;
//		String tag = (String)args[0];
//		Tag b = nbt.get(tag);
//		if (b != null) {
//			switch(ReikaNBTHelper.getTagType(b)) {
//				case BYTE:
//					o = ((NBTTagByte)b).func_150290_f();
//					break;
//				case DOUBLE:
//					o = ((NBTTagDouble)b).func_150286_g();
//					break;
//				case FLOAT:
//					o = ((NBTTagFloat)b).func_150288_h();
//					break;
//				case INT:
//					o = ((NBTTagInt)b).func_150287_d();
//					break;
//				case LONG:
//					o = ((NBTTagLong)b).func_150291_c();
//					break;
//				case SHORT:
//					o = ((NBTTagShort)b).func_150289_e();
//					break;
//				case STRING:
//					o = ((NBTTagString)b).func_150285_a_();
//					break;
//				case INTA:
//					o = ((NBTTagIntArray)b).func_150302_c();
//					break;
//				case BYTEA:
//					o = ((NBTTagByteArray)b).func_150292_c();
//					break;
//				case LIST:
//				case COMPOUND:
//					o = b.toString();
//					break;
//				case END:
//					break;
//			}
//		}
//		return o != null ? new Object[]{o} : null;
//	}
//
//	@Override
//	public String getDocumentation() {
//		return "Returns the value of an NBT tag.\nArgs: tagName\nReturns: tagValue";
//	}
//
//	@Override
//	public String getArgsAsString() {
//		return "String tagName";
//	}
//
//	@Override
//	public ReturnType getReturnType() {
//		return ReturnType.ARRAY;
//	}
//
//}
