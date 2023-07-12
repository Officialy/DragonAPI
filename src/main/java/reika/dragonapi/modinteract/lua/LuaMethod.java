/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.modinteract.lua;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import net.minecraft.world.level.block.entity.BlockEntity;
import reika.dragonapi.ModList;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;

public abstract class LuaMethod {

	public final String displayName;
	private final Class requiredClass;

	private static final HashMap<MethodKey, LuaMethod> methods = new HashMap();

	/*
	private static final LuaMethod tanks = new LuaGetTanks();
	private static final LuaMethod readTank = new LuaReadTank();
	private static final LuaMethod getSlot = new LuaGetSlot();
	private static final LuaMethod getSizeInv = new LuaInvSize();
	private static final LuaMethod printInv = new LuaPrintInv();
	private static final LuaMethod getCoords = new LuaGetCoords();
	private static final LuaMethod isFull = new LuaIsFull();
	private static final LuaMethod isTankFull = new LuaIsTankFull();
	private static final LuaMethod hasItem = new LuaHasItem();
	private static final LuaMethod trigger = new LuaTriggerAction();
	private static final LuaMethod placer = new LuaGetPlacer();
	private static final LuaMethod nbt = new LuaGetNBTTag();

	private static final LuaMethod getRFStorage;
	private static final LuaMethod getRFCapacity;
	//private static final LuaMethod getEUStorage = new LuaGetStoredEU();
	//private static final LuaMethod getEUCapacity = new LuaGetEUCapacity();

	private static final LuaMethod fluidColor = new LuaFluidColor();
	private static final LuaMethod getBlock = new LuaGetBlock();
	 */

	static {
		/*
		if (PowerTypes.RF.isLoaded()) {
			getRFStorage = new LuaGetStoredRF();
			getRFCapacity = new LuaGetRFCapacity();
		}
		else {
			getRFStorage = null;
			getRFCapacity = null;
		}*/
		registerMethods("reika.dragonapi.modinteract.lua");
	}

	public LuaMethod(String name, Class requiredParent) {
		displayName = name;

		requiredClass = requiredParent;

		MethodKey mk = this.getKey();
		if (methods.containsKey(mk))
			throw new IllegalArgumentException("This method is a duplicate of one that already exists!");
		else
			methods.put(mk, this);
	}

	public static final Collection<LuaMethod> getMethods() {
		return Collections.unmodifiableCollection(methods.values());
	}

	public static final LuaMethod getMethod(String name, Class c) {
		return methods.get(new MethodKey(name, c));
	}

	public static final int getNumberMethods() {
		return methods.size();
	}

/*todo	@ModDependent(ModList.COMPUTERCRAFT)
	public static Object[] invokeCC(LuaMethod m, BlockEntity te, Object[] args) throws LuaException, InterruptedException {
		try {
			return m.invoke(te, args);
		}
		catch (LuaMethodException e) {
			throw new LuaException(e.getMessage());
		}
	}

	@ModDependent(ModList.OPENCOMPUTERS)
	public static Object[] invokeOC(LuaMethod m, BlockEntity te, Object[] args) throws RuntimeException {
		try {
			return m.invoke(te, args);
		}
		catch (LuaMethodException e) {
			throw new RuntimeException(e);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}*/

	protected abstract Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException;

	public abstract String getDocumentation();

	public boolean isDocumented() {
		return true;
	}

	public final boolean isClassInstanceOf(Class<? extends BlockEntity> te) {
		return requiredClass == null || requiredClass.isAssignableFrom(te);
	}

	public final boolean isValidFor(BlockEntity te) {
		return requiredClass == null || requiredClass.isAssignableFrom(te.getClass());
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof LuaMethod) {
			return ((LuaMethod)o).displayName.equals(displayName) && requiredClass == ((LuaMethod)o).requiredClass;
		}
		else
			return false;
	}

	@Override
	public final int hashCode() {
		return displayName.hashCode() ^ requiredClass.hashCode();
	}

	@Override
	public final String toString() {
		String name = requiredClass != null ? requiredClass.getSimpleName() : "Any BlockEntity";
		return displayName+"() for "+name;
	}

	private MethodKey getKey() {
		return new MethodKey(this);
	}

	public static void registerMethods(String folder) {
		try {
			for (Class c : ReikaJavaLibrary.getAllClassesFromPackage(folder, LuaMethod.class, true, true)) {
				if (c.isAnnotationPresent(ModTileDependent.class)) {
					String[] vals = ((ModTileDependent)c.getAnnotation(ModTileDependent.class)).value();
					for (String s : vals) {
//				todo		if (!ReikaASMHelper.checkForClass(s)) {
//							continue;
//						}
					}
				}
				if (c.isAnnotationPresent(ModDependentMethod.class)) {
					ModList mod = ((ModDependentMethod)c.getAnnotation(ModDependentMethod.class)).value();
					if (!mod.isLoaded()) {
						continue;
					}
				}
				c.newInstance();
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Could not load LuaMethods!", e);
		}
	}

	/** Without "( )" */
	public abstract String getArgsAsString();

	public abstract ReturnType getReturnType();

	public enum ReturnType {
		VOID("void"),
		INTEGER("int"),
		LONG("long"),
		ARRAY("Object[]"),
		STRING("String"),
		BOOLEAN("boolean"),
		FLOAT("float");

		public final String displayName;

		ReturnType(String name) {
			displayName = name;
		}

	}

	private static class MethodKey {

		private final String name;
		private final Class parent;

		private MethodKey(LuaMethod m) {
			this(m.displayName, m.requiredClass);
		}

		private MethodKey(String s, Class c) {
			name = s;
			parent = c;
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	public @interface ModTileDependent {
		String[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	public @interface ModDependentMethod {
		ModList value();
	}

	public static final class LuaMethodException extends Exception {

		public LuaMethodException(Exception e)  {
			super(e);
		}

		public LuaMethodException(String s, Exception e)  {
			super(s, e);
		}

		public LuaMethodException(String s)  {
			super(s);
		}
	}

}
