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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforgespi.language.ModFileScanData;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.ModList;

public abstract class LuaMethod {

	public final String displayName;
	private final Class requiredClass;

	//mods construct on parallel loader threads, each registering its own methods
	private static final Map<MethodKey, LuaMethod> methods = new ConcurrentHashMap<>();

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
		if (methods.putIfAbsent(mk, this) != null)
			throw new IllegalArgumentException("This method is a duplicate of one that already exists!");
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

	/**
	 * Runs {@code m} against {@code te}. 1.7.10 had one entry point per computer mod ({@code invokeCC} wrapping a
	 * {@link LuaMethodException} into a ComputerCraft {@code LuaException}, {@code invokeOC} into a RuntimeException);
	 * the ComputerCraft translation now lives in {@link reika.dragonapi.modinteract.CCCompat}, which is only loaded when
	 * CC: Tweaked is, so this half stays free of computer-mod types.
	 * <p>OPENCOMPUTERS-PORT: {@code invokeOC} (LuaMethodException/InterruptedException -> RuntimeException) returns
	 * with an OpenComputers 26.3 build; OC has none.
	 */
	public static Object[] call(LuaMethod m, BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		return m.invoke(te, args);
	}

	/**
	 * Every registered method valid for {@code te}, in the stable order a peripheral's method indices refer to. Where
	 * two mods register the same name for classes this block entity both extends (getTemperature, getName), only the
	 * one bound to the more specific class is kept, since a computer can only call a name once.
	 */
	public static LuaMethod[] getMethodsFor(BlockEntity te) {
		HashMap<String, LuaMethod> byName = new HashMap<>();
		for (LuaMethod l : methods.values()) {
			if (!l.isValidFor(te))
				continue;
			LuaMethod prev = byName.get(l.displayName);
			if (prev == null || prev.isLessSpecificThan(l))
				byName.put(l.displayName, l);
		}
		ArrayList<LuaMethod> li = new ArrayList<>(byName.values());
		li.sort(Comparator.comparing((LuaMethod l) -> l.displayName));
		return li.toArray(new LuaMethod[0]);
	}

	private boolean isLessSpecificThan(LuaMethod other) {
		if (requiredClass == other.requiredClass)
			return false;
		if (requiredClass == null)
			return true;
		if (other.requiredClass == null)
			return false;
		if (requiredClass.isAssignableFrom(other.requiredClass))
			return true;
		if (other.requiredClass.isAssignableFrom(requiredClass))
			return false;
		//unrelated interfaces/classes: deterministic tie-break
		return requiredClass.getName().compareTo(other.requiredClass.getName()) > 0;
	}

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
		return displayName.hashCode() ^ java.util.Objects.hashCode(requiredClass);
	}

	@Override
	public final String toString() {
		String name = requiredClass != null ? requiredClass.getSimpleName() : "Any BlockEntity";
		return displayName+"() for "+name;
	}

	private MethodKey getKey() {
		return new MethodKey(this);
	}

	/**
	 * Instantiates (and so registers) every concrete LuaMethod in {@code folder}. 1.7.10 walked the classpath; under
	 * FML's module layer that finds nothing outside a dev run, so this reads NeoForge's mod file scan data instead,
	 * which lists every class of every loaded mod in dev and production alike.
	 */
	public static void registerMethods(String folder) {
		String prefix = folder.endsWith(".") ? folder : folder+".";
		ArrayList<String> names = new ArrayList<>();
		for (ModFileScanData data : net.neoforged.fml.ModList.get().getAllScanData()) {
			for (ModFileScanData.ClassData cd : data.getClasses()) {
				String name = cd.clazz().getClassName();
				if (name.startsWith(prefix) && !name.contains("$"))
					names.add(name);
			}
		}
		Collections.sort(names);
		int before = methods.size();
		for (String name : names) {
			try {
				Class<?> c = Class.forName(name, false, LuaMethod.class.getClassLoader());
				if (!LuaMethod.class.isAssignableFrom(c) || Modifier.isAbstract(c.getModifiers()) || c.isAnnotationPresent(Deprecated.class))
					continue;
				if (c.isAnnotationPresent(ModTileDependent.class)) {
					boolean present = true;
					for (String s : c.getAnnotation(ModTileDependent.class).value()) {
						if (!classExists(s)) {
							present = false;
							break;
						}
					}
					if (!present)
						continue;
				}
				if (c.isAnnotationPresent(ModDependentMethod.class)) {
					ModList mod = c.getAnnotation(ModDependentMethod.class).value();
					if (!mod.isLoaded()) {
						continue;
					}
				}
				c.getDeclaredConstructor().newInstance();
			}
			catch (ReflectiveOperationException | LinkageError e) {
				throw new RuntimeException("Could not load LuaMethod "+name+"!", e);
			}
		}
		DragonAPI.LOGGER.info("Registered {} LuaMethods from {} ({} total).", methods.size()-before, folder, methods.size());
	}

	private static boolean classExists(String name) {
		return LuaMethod.class.getClassLoader().getResource(name.replace('.', '/')+".class") != null;
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

    private record MethodKey(String name, Class parent) {

        private MethodKey(LuaMethod m) {
            this(m.displayName, m.requiredClass);
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
