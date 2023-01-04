package reika.dragonapi.libraries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLLoader;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.interfaces.item.UnbreakableArmor;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;

import static reika.dragonapi.DragonAPI.rand;

public class ReikaEntityHelper {

    private static final HashMap<Class<?>, Boolean> hostilityMap = new HashMap<>();

    /**
     * Adds a small velocity in a random direction (akin to items' speeds when dropped)
     */
    public static void addRandomDirVelocity(Entity ent, double max) {
        ent.setDeltaMovement(-max + 2 * max * rand.nextFloat(), -max + 2 * max * rand.nextFloat(), 4 * max * rand.nextFloat());
    }

    public static boolean isHostile(LivingEntity mob) {
        return isHostile(mob.getClass());
    }

    public static boolean isHostile(Class<? extends LivingEntity> mob) {
        Boolean ret = hostilityMap.get(mob);
        if (ret == null) {
            ret = calcHostility(mob);
            hostilityMap.put(mob, ret);
        }
        return ret.booleanValue();
    }

    /**
     * Returns the mass (in kg) of the entity. Args: Entity
     */
    public static double getEntityMass(Entity ent) {
        if (ent instanceof ItemEntity || ent instanceof ExperienceOrb)
            return 0.25;
        if (ent instanceof Creeper)
            return 100; //220 lbs; TNT is heavy
        if (ent instanceof Skeleton)
            return 30;    //66 lbs
        if (ent instanceof Piglin)
            return 90;
        if (ent instanceof Zombie || ent instanceof Player || ent instanceof Villager || ent instanceof Witch)
            return 70; // 180 lbs
        if (ent instanceof CaveSpider)
            return 30;
        if (ent instanceof Spider)
            return 60;    //
        if (ent instanceof Pig)
            return 100;
        if (ent instanceof Cow || ent instanceof MushroomCow)
            return 350;
        if (ent instanceof Ghast)
            return 20; //spirit creature
        if (ent instanceof Blaze)
            return 300;
        if (ent instanceof MagmaCube) {
            MagmaCube cube = (MagmaCube) ent;
            return 400 * cube.getSize() * cube.getSize();
        }
        if (ent instanceof Slime) {
            Slime cube = (Slime) ent;
            return 200 * cube.getSize() * cube.getSize();
        }
        if (ent instanceof EnderMan)
            return 40;
        if (ent instanceof Silverfish)
            return 1;
        if (ent instanceof Chicken)
            return 2;
        if (ent instanceof EnderDragon)
            return 10000; //really conjectural
        if (ent instanceof WitherBoss)
            return 3000;  //even more conjectural
        if (ent instanceof Wolf)
            return 50;
        if (ent instanceof Ocelot)
            return 15;
        if (ent instanceof IronGolem)
            return 32000; //iron = 8g/cc, 4m^3 of it
        if (ent instanceof IronGolem)
            return 100;
        if (ent instanceof Sheep)
            return 150;
        if (ent instanceof Squid)
            return 120;
        if (ent instanceof Bat)
            return 0.5;
        if (ent instanceof Minecart)
            return 400;
        if (ent instanceof Boat)
            return 70;
        if (ent instanceof PrimedTnt)
            return 2700; //2.7 g/cc
        if (ent instanceof FallingBlockEntity)
            return ReikaPhysicsHelper.getBlockDensity(((FallingBlockEntity) ent).getBlockState().getBlock());//.func_145805_f()); //2 g/cc
        return 100;
    }
    
    /** Gets a direction from an entity's look direction. Args: Entity, allow vertical yes/no */
    public static Direction getDirectionFromEntityLook(LivingEntity e, boolean vertical) {
        if (Mth.abs(e.getXRot()) < 60 || !vertical) {
            int i = Mth.floor((e.getYRot() * 4F) / 360F + 0.5D);
            while (i > 3)
                i -= 4;
            while (i < 0)
                i += 4;
            switch (i) {
                case 0 -> {
                    return Direction.SOUTH;
                }
                case 1 -> {
                    return Direction.WEST;
                }
                case 2 -> {
                    return Direction.NORTH;
                }
                case 3 -> {
                    return Direction.EAST;
                }
            }
        }
        else { //Looking up/down
            if (e.getXRot() > 0)
                return Direction.DOWN; //set to up
            else
                return Direction.UP; //set to down
        }
        return Direction.NORTH; //todo default to north?
    }
    
    public static boolean isSolidEntity(Entity e) {
//        if (e instanceof EtherealEntity)
//            return false;
        String name = e.getClass().getSimpleName();
        if (name.equalsIgnoreCase("EntityTFMobileFirefly"))
            return false;
        return !name.equalsIgnoreCase("EntityWisp");
    }

    private static boolean calcHostility(Class<? extends LivingEntity> mob) {
//        if (TameHostile.class.isAssignableFrom(mob))
//            return false;
        if (Mob.class.isAssignableFrom(mob))
            return true;
        if (Ghast.class.isAssignableFrom(mob))
            return true;
        if (Slime.class.isAssignableFrom(mob))
            return true;
        if (Witch.class.isAssignableFrom(mob))
            return true;
        if (Witch.class.isAssignableFrom(mob))
            return true;
        if (EnderDragon.class.isAssignableFrom(mob))
            return true;
        if (WitherBoss.class.isAssignableFrom(mob))
            return true;
        String n = mob.getName().toLowerCase(Locale.ENGLISH);
        if (n.contains("wisp"))
            return true;
        if (n.contains("pech"))
            return true;
        if (n.contains("botania") && n.contains("doppleganger"))
            return true;
        return n.contains("tconstruct") && n.contains("blueslime");
    }

    public static boolean isEntityWearingFullSuitOf(LivingEntity e, ArmorMaterial type) {
        return isEntityWearingFullSuitOf(e, (ItemStack is) -> is.getItem() instanceof ArmorItem && ((ArmorItem)is.getItem()).getMaterial() == type);
    }

    public static boolean isEntityWearingFullSuitOf(LivingEntity e, Function<ItemStack, Boolean> func) {
        for (int i = 1; i <= 4; i++) {
            ItemStack is = e.getItemBySlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, i));
            if (is == null || !func.apply(is))
                return false;
        }
        return true;
    }
    public static boolean burnsInSun(LivingEntity e) {
        return e.getMobType() == MobType.UNDEAD;
    }

    public static int damageArmor(LivingEntity e, int amt) {
        return damageArmor(e, amt, null);
    }

    public static int damageArmor(LivingEntity e, int amt, BiFunction<ItemStack, Integer, Integer> handle) {
        return damageArmor(e, amt, (s -> true), handle);
    }

    public static int damageArmor(LivingEntity e, int amt, int slot) {
        return damageArmor(e, amt, (s -> s == slot), null);
    }

    public static int damageArmor(LivingEntity e, int amt, Function<Integer, Boolean> apply, BiFunction<ItemStack, Integer, Integer> handle) {
        int ret = 0;
        for (int i = 0; i <= 3; i++) {
            ret += damageArmorItem(e, i, amt, handle);
        }
        return ret;
    }

    public static void performEntityVerification(ServerPlayer ep, int entityID, ResourceKey<Level> dim, int classHash) {
//        ReikaJavaLibrary.pConsole("Verifying existence of "+entityID+" on side "+ FMLLoader.getDist());
        Level world = ep.getLevel();//todo DimensionManager.getWorld(dim);
        if (world != null) {
            Entity e = world.getEntity(entityID);
            if (e != null) {
                if (e.getClass().getName().hashCode() == classHash) {
                    ReikaJavaLibrary.pConsole("Verified existence of "+e+" on side "+FMLLoader.getDist());
                    return;
                }
            }
        }
        ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.ENTITYVERIFYFAIL.ordinal(), ep, entityID, dim.hashCode()); //todo dimension id stuff, old code so i just put hashcode for now so it doesnt hate me
        //ReikaJavaLibrary.pConsole("Verified NON-existence of "+entityID+" on side "+FMLCommonHandler.instance.getEffectiveSide());
    }

    private static int damageArmorItem(LivingEntity e, int slot, int amt, BiFunction<ItemStack, Integer, Integer> handle) {
        ItemStack arm = e.getItemBySlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, slot));
        if (arm != null && canDamageArmorOf(e)) {
            ItemStack pre = arm.copy();
            int ret = 0;
            if (handle != null) {
                Integer get = handle.apply(arm, amt);
                if (get != null) {
                    ret += get.intValue();
                }
            }
            Item item = arm.getItem();
//            if (InterfaceCache.MUSEELECTRICITEM.instanceOf(item)) {
//                MuseElectricItem ms = (MuseElectricItem)item;
//                ret += ms.extractEnergy(arm, amt*300, false);
//            }
//            else if (InterfaceCache.RFENERGYITEM.instanceOf(item)) {
//                IEnergyContainerItem ie = (IEnergyContainerItem)item;
//                ret += ie.extractEnergy(arm, amt*300, false);
//            }
//            else if (InterfaceCache.IELECTRICITEM.instanceOf(item)) {
//                ret += ElectricItem.manager.discharge(arm, amt*250, Integer.MAX_VALUE, true, false, false);
//            }
//            else if (InterfaceCache.GASITEM.instanceOf(item)) {
//                IGasItem ie = (IGasItem)item;
//                GasStack gas = ie.getGas(arm);
//                if (gas != null && gas.amount > 0)
//                    gas = ie.removeGas(arm, Math.max(amt, gas.amount*amt/400));
//                ret += gas != null ? gas.amount : 0;
//            }
            if (item instanceof UnbreakableArmor && !((UnbreakableArmor) item).canBeDamaged()) {
                //do nothing
            } else {
                arm.setDamageValue(amt);// damageItem(amt, e);
                if (arm.getDamageValue() > arm.getMaxDamage() || arm.getCount() <= 0) {
                    arm = null;
                    e.setItemSlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, slot), ItemStack.EMPTY);
                }
                e.playSound(SoundEvents.ITEM_BREAK, 0.1F, 0.8F);
                ret += amt;
            }
            ItemStack post = e.getItemBySlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, slot));
            return ItemStack.matches(pre, post) ? 0 : ret;
        }
        return 0;
    }


    /**
     * Returns true if all LivingEntity within the list are dead. Args: List
     * [The list MUST be of LivingEntity (or subclass) - any other type WILL cause
     * a classcast exception!], test isDead only yes/no
     */
    public static boolean allAreDead(List<LivingEntity> mobs, boolean isDeadOnly) {
        for (LivingEntity ent : mobs) {
            if ((!ent.isDeadOrDying() && ent.getHealth() > 0) || (!ent.isDeadOrDying() && isDeadOnly))
                return false;
        }
        return true;
    }

    /**
     * Drop an entity's head. Args: LivingEntity
     */
    public static void dropHead(LivingEntity e) {
        if (e == null)
            return;
        ItemStack is = null;
        if (e instanceof Skeleton) {
            is = new ItemStack(Items.SKELETON_SKULL, 1);
        }
        if (e instanceof WitherSkeleton) {
            is = new ItemStack(Items.WITHER_SKELETON_SKULL, 1);
        }
        if (e instanceof Zombie || e instanceof ZombieVillager) {
            is = new ItemStack(Items.ZOMBIE_HEAD, 1);
        }
        if (e instanceof Player)
            is = new ItemStack(Items.PLAYER_HEAD, 1);
        if (e instanceof Creeper)
            is = new ItemStack(Items.CREEPER_HEAD, 1);
        if (is == null)
            return;
        ReikaItemHelper.dropItem(e.getLevel(), new BlockPos(e.getX(), e.getY() + 0.2, e.getZ()), is);
    }

    private static boolean canDamageArmorOf(LivingEntity target) {
        MinecraftServer ms = target.getServer();
        return !(target instanceof Player) || ms != null && ms.isPvpAllowed();
    }

}
