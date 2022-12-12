package reika.dragonapi.libraries.java;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;

import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.loading.FMLLoader;
import reika.dragonapi.exception.VanillaIntegrityException;

public class ReikaObfuscationHelper {

    private static final boolean deobf = testDeobf();

    private static final HashMap<String, Method> methods = new HashMap<>();
    private static final HashMap<String, Field> fields = new HashMap<>();
    private static final HashMap<String, Class> classes = new HashMap<>();
    private static final HashMap<String, String> labels = new HashMap<>();

    private static final HashSet<ReflectiveAccessExceptionHandler> errorHandlers = new HashSet<>();

    private static boolean testDeobf() {/*
		try {
			Method m = ItemHoe.class.getMethod("onItemUse", ItemStack.class, Player.class, World.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class);
			return true;
		}
		catch (NoSuchMethodException e) {
			return false;
		}*/
//todo        return !FMLForgePlugin.RUNTIME_DEOBF;
        return true;
    }

    public static void registerExceptionHandler(ReflectiveAccessExceptionHandler h) {
        errorHandlers.add(h);
    }

    public static boolean isDeObfEnvironment() {
        return deobf;
    }

    private static Field getField(String deobf) {
        return fields.get(deobf);
    }

    private static Method getMethod(String deobf) {
        return methods.get(deobf);
    }

    public static Object get(String field, Object ref) {
        try {
            return getField(field).get(ref);
        }
        catch (Exception e) {
            handleException(e);
            return null;
        }
    }

    public static void set(String method, Object ref, Object val) {
        try {
            getField(method).set(ref, val);
        }
        catch (Exception e) {
            handleException(e);
        }
    }

    public static Object invoke(String method, Object ref, Object... args) {
        try {
            return getMethod(method).invoke(ref, args);
        }
        catch (Exception e) {
            handleException(e);
            return null;
        }
    }

    private static void handleException(Exception e) {
        boolean print = true;
        for (ReflectiveAccessExceptionHandler h : errorHandlers) {
            print &= h.handleException(e);
        }
        if (print)
            e.printStackTrace();
    }

    public static String getLabelName(String deobf) {
        String sg = labels.get(deobf);
        if (sg == null)
            throw new IllegalArgumentException("Tried to get obfuscated name for non-mapped deobf field/method "+deobf+"!");
        if (isDeObfEnvironment())
            return deobf;
        else
            return sg;
    }

    private static void addClass(String key, String n) {
        try {
            Class c = Class.forName(n);
            classes.put(key, c);
            ReikaJavaLibrary.pConsole("DRAGONAPI: Registering reflexive field access to class "+c); //cannot use logger
        }
        catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Tried to register nonexistent class "+n, e);
        }
    }

    private static void addField(String deobf, String obf, boolean isVisible, String k) {
        addField(deobf, obf, isVisible, classes.get(k));
    }

    private static void addField(String deobf, String obf, boolean isVisible, Class c) {
        try {
            String sg = isDeObfEnvironment() ? deobf : obf;
            Field f;
            if (isVisible)
                f = c.getField(sg);
            else {
                f = c.getDeclaredField(sg);
                f.setAccessible(true);
            }
            fields.put(deobf, f);
            labels.put(deobf, obf);
            ReikaJavaLibrary.pConsole("DRAGONAPI: Registering reflexive field access to "+c+"."+deobf+" (obfuscated as "+obf+")"); //cannot use logger
        }
        catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Tried to register nonexistent field "+deobf+"/"+obf, e);
        }
    }

    private static void addMethod(String deobf, String obf, boolean isVisible, Class c, Class... args) {
        try {
            String sg = isDeObfEnvironment() ? deobf : obf;
            Method m;
            if (isVisible)
                m = c.getMethod(sg, args);
            else {
                m = c.getDeclaredMethod(sg, args);
                m.setAccessible(true);
            }
            methods.put(deobf, m);
            labels.put(deobf, obf);
            ReikaJavaLibrary.pConsole("DRAGONAPI: Registering reflexive method access to "+c+"."+deobf+" (obfuscated as "+obf+")"); //cannot use logger
        }
        catch (NoSuchMethodException e) {
            //throw new VanillaIntegrityException("Tried to register nonexistent method "+deobf+"/"+obf+". Check signature.");
            throw new VanillaIntegrityException(deobf, c, args);
        }
    }

/*    static {
        addClass("SeedEntry", ForgeHooks.class.getName()+"$SeedEntry");

        //addMethod("onItemUse", "func_77648_a", true, Item.class, ItemStack.class, Player.class, World.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class);
        addMethod("dropFewItems", "func_70628_a", false, LivingEntity.class, boolean.class, int.class);
        addMethod("dropEquipment", "func_82160_b", false, LivingEntity.class, boolean.class, int.class);
        addMethod("dropRareDrop", "func_70600_l", false, LivingEntity.class, int.class);
        addMethod("jump", "func_70664_aZ", false, LivingEntity.class);
        addMethod("getHurtSound", "func_70621_aR", false, LivingEntity.class);
//        addMethod("canSpawnStructureAtCoords", "func_75047_a", false, MapGenStructure.class, int.class, int.class);
//        addMethod("getCoordList", "func_75052_o_", false, MapGenStructure.class);
        addMethod("tryExtend", "func_150079_i", false, PistonBaseBlock.class, Level.class, int.class, int.class, int.class, int.class);
        if (isClientSide()) {
            addMethod("getInputStreamByName", "func_110591_a", false, AbstractPackResources.class, String.class);
        }*//*
		if (isClientSide()) {
			addField("field_110859_k", "field_110859_k", false, RenderBiped.class); //armor texture map
			addField("scaleAmount", "field_77092_a", false, RenderSlime.class);
			addField("mainModel", "field_77045_g", false, RendererLivingEntity.class);
			addField("modelBipedMain", "field_77109_a", false, RenderPlayer.class);
		}
		addField("isJumping", "field_70703_bu", false, LivingEntity.class);
		addField("timeSinceIgnited", "field_70833_d", false, Creeper.class);*//*
        addField("potionTypes", "field_76425_a", true, Potion.class);*//*
		addField("biomeList", "field_76773_a", true, Biome.class);
		addField("weaponDamage", "field_77827_a", false, ItemSword.class);
		addField("isAggressive", "field_104003_g", false, EnderMan.class);
		addField("stareTimer", "field_70826_g", false, EnderMan.class);
		addField("theWorldGenerator", "field_82915_S", false, BiomeGenHills.class);
		addField("blockFlammability", "blockFlammability", false, Blocks.class);
		addField("blockFireSpreadSpeed", "blockFireSpreadSpeed", false, Blocks.class);
		addField("stringToIDMapping", "field_75622_f", false, EntityList.class);*//*
        addField("harvesters", "harvesters", false, Block.class);
        addField("seedList", "seedList", false, ForgeHooks.class);
        addField("seed", "seed", false, "SeedEntry");

        if (isClientSide()) {
            //addField("soundLibrary", "soundLibrary", false, SoundSystem.class);
            //addField("streamThread", "streamThread", false, Library.class);
        }
    }*/

    private static boolean isClientSide() {
        return FMLLoader.getDist() == Dist.CLIENT;
    }

    public static interface ReflectiveAccessExceptionHandler {

        /** Return false to silence the stacktrace at the end. */
        public boolean handleException(Exception e);

    }
}
