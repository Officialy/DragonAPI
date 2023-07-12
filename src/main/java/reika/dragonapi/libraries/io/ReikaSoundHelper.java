package reika.dragonapi.libraries.io;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.data.maps.MultiMap;
import reika.dragonapi.instantiable.io.EnumSound;
import reika.dragonapi.instantiable.io.SoundVariant;
import reika.dragonapi.interfaces.registry.SoundEnum;
import reika.dragonapi.interfaces.registry.VariableSound;
import reika.dragonapi.libraries.java.ReikaArrayHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;

import java.util.*;

public class ReikaSoundHelper {

    private static final MultiMap<SoundEnum, SoundPlay> plays = new MultiMap<>();
    private static final HashMap<Class<?>, SoundEnumSet> soundSets = new HashMap<>();
    private static final HashMap<Integer, Class<?>> soundSetIDs = new HashMap<>();

    public static void playBreakSound(Level world, BlockPos pos, Block b) {
        playBreakSound(world, pos.getX(), pos.getY(), pos.getZ(), b);
    }

    public static void playBreakSound(Level world, int x, int y, int z, Block b) {
        SoundType s = b.defaultBlockState().getSoundType();
        SoundEvent f = s.getBreakSound();
        world.playLocalSound(x + 0.5, y + 0.5, z + 0.5, f, SoundSource.BLOCKS, s.getVolume(), s.getPitch(), false);
    }

    public static void playStepSound(Level world, int x, int y, int z, Block b) {
        SoundEvent s = b.defaultBlockState().getSoundType().getStepSound();
        world.playLocalSound(x + 0.5, y + 0.5, z + 0.5, s, SoundSource.BLOCKS, 1, 1, false);
    }

    public static void playPlaceSound(Level world, int x, int y, int z, Block b) {
        SoundEvent s = b.defaultBlockState().getSoundType().getPlaceSound();
        world.playLocalSound(x + 0.5, y + 0.5, z + 0.5, s, SoundSource.BLOCKS, 1, 1, false);
    }

    public static void playBreakSound(Level world, int x, int y, int z, Block b, float vol, float pitch) {
        SoundEvent s = b.defaultBlockState().getSoundType().getBreakSound();
        world.playLocalSound(x + 0.5, y + 0.5, z + 0.5, s, SoundSource.BLOCKS, 1 * vol, 1 * pitch, false);
    }

    public static void playStepSound(Level world, int x, int y, int z, Block b, float vol, float pitch) {
        SoundEvent s = b.defaultBlockState().getSoundType().getStepSound();
        world.playLocalSound(x + 0.5, y + 0.5, z + 0.5, s, SoundSource.BLOCKS, 1 * vol, 1 * pitch, false);
    }

    public static void playPlaceSound(Level world, int x, int y, int z, Block b, float vol, float pitch) {
        SoundEvent s = b.defaultBlockState().getSoundType().getPlaceSound();
        world.playLocalSound(x + 0.5, y + 0.5, z + 0.5, s, SoundSource.BLOCKS, 1 * vol, 1 * pitch, false);
    }

    public static void playSoundAtBlock(Level world, int x, int y, int z, SoundEvent snd, float vol, float pit) {
        world.playLocalSound(x + 0.5, y + 0.5, z + 0.5, snd, SoundSource.BLOCKS, vol, pit, false);
    }

    public static void playSoundAtBlock(Level world, BlockPos pos, SoundEvent snd, float vol, float pit) {
        world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, snd, SoundSource.BLOCKS, vol, pit, false);
    }

    public static void playSoundAtBlock(Level world, int x, int y, int z, SoundEvent snd) {
        world.playLocalSound(x + 0.5, y + 0.5, z + 0.5, snd, SoundSource.BLOCKS, 1, 1, false);
    }

    public static void playSoundAtBlock(Level world, BlockPos pos, SoundEvent snd) {
        world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, snd, SoundSource.BLOCKS, 1, 1, false);
    }

    public static void playSound(SoundEnum s, Level world, Entity e, float vol, float pitch) {
        playSound(s, world, e.getX(), e.getY(), e.getZ(), vol, pitch);
    }

    public static void playSound(SoundEnum s, Level world, double x, double y, double z, float vol, float pitch) {
        playSound(s, world, x, y, z, vol, pitch, s.attenuate());
    }

    public static void playSound(SoundEnum s, Level world, double x, double y, double z, float vol, float pitch, boolean atten) {
        long time = world.getGameTime();
        if (!s.canOverlap()) {
            Collection<SoundPlay> c = plays.get(s);
            Iterator<SoundPlay> it = c.iterator();
            while (it.hasNext()) {
                SoundPlay p = it.next();
                if (time - p.time < 20) { //1s for now
                    if (p.loc.getDistanceTo(x, y, z) < 12 && !p.loc.sharesBlock(x, y, z))
                        return;
                } else {
                    it.remove();
                }
            }
            plays.addValue(s, new SoundPlay(time, x, y, z));
        }
        sendSound(s, world, x, y, z, vol, pitch, atten);
    }

    private static void sendSound(SoundEnum s, Level world, double x, double y, double z, float vol, float pitch, boolean atten) {
        ReikaPacketHelper.sendSoundPacket(s, world, x, y, z, vol, pitch, atten);
    }


    public static SoundInstance playClientSound(SoundEnum s, double x, double y, double z, float vol, float pitch) {
        return playClientSound(s, x, y, z, vol, pitch, true);
    }


    public static SoundInstance playClientSound(SoundEnum s, double x, double y, double z, float vol, float pitch, boolean att) {
        DragonAPI.LOGGER.info("Playing sound "+s+" at "+x+", "+y+", "+z);
        float v = vol * s.getModulatedVolume();
        if (v <= 0)
            return null;
        EnumSound es = new EnumSound(s, x, y, z, v, pitch, att);
        try {
            Minecraft.getInstance().getSoundManager().play(es);
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }
        return es;
    }

    public static SoundInstance playClientSound(SoundEnum s, Entity e, float vol, float pitch, boolean att) {
        return playClientSound(s, e.getX(), e.getY(), e.getZ(), vol, pitch, att);
    }

    public static void playClientSound(SoundEvent snd, double x, double y, double z, float vol, float pitch, boolean atten) {
        Minecraft.getInstance().level.playLocalSound(x, y, z, snd, SoundSource.AMBIENT, vol, pitch, atten);
    }


    public static void playNormalClientSound(Level world, double x, double y, double z, SoundEvent name, float vol, float pitch, boolean flag) {
        world.playLocalSound(x, y, z, name, SoundSource.AMBIENT, vol, pitch, flag);
    }

    public static void broadcastSound(SoundEnum s, float vol, float pitch) {
        if (FMLLoader.getDist() == Dist.CLIENT) //todo getdist wont work in this context
            throw new MisuseException("You cannot call this from the client!");
//   todo Level[] worlds = DimensionManager.getLevels();
//        for (Level world : worlds) {
//            for (Player ep : world.players()) {
//                playSound(s, world, ep, vol, pitch);
//            }
//        }

    }

    public static void playSoundAtEntity(Level world, Entity e, SoundEvent snd) {
        playSoundAtEntity(world, e, snd, 1, 1);
    }

    public static void playSoundAtEntity(Level world, Entity e, SoundEvent snd, float vol, float p) {
        world.playLocalSound(e.getX(), e.getY(), e.getZ(), snd, SoundSource.AMBIENT, vol, p, false);
    }

    public static void playSoundFromServer(Level world, double x, double y, double z, ResourceLocation loc, float vol, float pitch, boolean scale) {
        ReikaPacketHelper.writeDirectSound(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.SERVERSOUND.ordinal(), world, x, y, z, loc, vol, pitch, scale);
    }

    public static void playSoundFromServerAtBlock(Level world, int x, int y, int z, ResourceLocation loc, float vol, float pitch, boolean scale) {
        playSoundFromServer(world, x + 0.5, y + 0.5, z + 0.5, loc, vol, pitch, scale);
    }

    static {
        soundSetIDs.put(0, SingleSound.class);
        SingleSoundSet set = new SingleSoundSet();
        soundSets.put(SingleSound.class, set);
    }

    public static SoundEnumSet getSoundLibrary(SoundEnum s) {
        SoundEnumSet set = soundSets.get(s.getClass());
        if (set == null) {
            DragonAPI.LOGGER.error("Tried to play an unregistered sound '" + s.getClass() + " " + s + "'!");
        }
        return set;
    }

    public static SoundEnum lookupSound(int lib, int idx) {
        Class<?> type = soundSetIDs.get(lib);
        SoundEnumSet set = soundSets.get(type);
        if (type == null || set == null) {
            DragonAPI.LOGGER.error("Tried to play an unregistered sound!");
            return null;
        }
        return set.getSound(idx);
    }

    public static void registerSoundSet(Class<? extends SoundEnum> c) {
        if (c == SingleSound.class) {
            throw new IllegalArgumentException("You cannot register single sounds as a set!");
        } else {
            SoundEnumSet set = soundSets.get(c);
            if (set != null) {
                //throw new IllegalArgumentException("Sound set "+c+" already registered!");
            }
            int idx = set != null ? set.index : soundSets.size();
            soundSetIDs.put(idx, c);
            if (set == null) {
                if (VariableSound.class.isAssignableFrom(c))
                    set = new SoundEnumSetWithVariants((Class<? extends VariableSound>) c, idx);
                else
                    set = new SoundEnumSet(c, idx);
            }
            soundSets.put(c, set);
            if (set instanceof SoundEnumSetWithVariants) {
                for (Class<?> c2 : ((SoundEnumSetWithVariants) set).variantClasses) {
                    soundSets.put(c2, set);
                }
            }
            ReikaJavaLibrary.pConsole("Registered sound set of type " + c + " with values " + Arrays.toString(set.sounds));
        }
    }

    public static void registerSingleSound(SingleSound s) {
        SingleSoundSet set = (SingleSoundSet) soundSets.get(SingleSound.class);
        set.addSound(s);
    }

    private static class SoundPlay {

        private final long time;
        private final DecimalPosition loc;

        private SoundPlay(long t, double x, double y, double z) {
            time = t;
            loc = new DecimalPosition(x, y, z);
        }

    }

    private static class SingleSoundSet extends SoundEnumSet {

        private final ArrayList<SingleSound> soundList = new ArrayList<>();
        private final HashSet<String> nameSet = new HashSet<>();
        private final Comparator<SingleSound> sorter = (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name);

        private SingleSoundSet() {
            super(SingleSound.class, 0);
        }

        private void addSound(SingleSound s) {
            if (!nameSet.add(s.name))
                throw new MisuseException("Sound name '" + s.name + "' already occupied!");
            soundList.add(s);
            soundList.sort(sorter);
        }

        @Override
        protected SoundEnum getSound(int idx) {
            return soundList.get(idx);
        }

        @Override
        public int getSoundIndex(SoundEnum s) {
            return soundList.indexOf(s);
        }

    }

    private static class SoundEnumSetWithVariants extends SoundEnumSet {

        private static final Comparator<SoundVariant<?>> sorter = (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());

        private final HashMap<SoundEnum, SoundVariant<?>[]> variants = new HashMap<>();
        private final HashSet<Class<?>> variantClasses = new HashSet<>();

        private SoundEnumSetWithVariants(Class<? extends VariableSound> c, int idx) {
            super(c, idx);

            for (SoundEnum e : sounds) {
                VariableSound v = (VariableSound) e;
                Collection<SoundVariant<?>> cv = v.getVariants();
                if (cv != null && !cv.isEmpty()) {
                    SoundVariant<?>[] arr = cv.toArray(new SoundVariant[cv.size()]);
                    Arrays.sort(arr, sorter);
                    variants.put(e, arr);
                    for (SoundVariant<?> sv : arr) {
                        variantClasses.add(sv.getClass());
                    }
                }
            }
        }

        @Override
        protected SoundEnum getSound(int idx) {
            int base = idx & 32767;
            int variant = (idx >> 16) & 32767;
            SoundEnum e = super.getSound(base);
            if (variant > 0) {
                SoundVariant<?>[] arr = variants.get(e);
                e = arr[variant - 1];
            }
            return e;
        }

        @Override
        public int getSoundIndex(SoundEnum s) {
            SoundEnum parent = s;
            boolean var = s instanceof SoundVariant;
            if (var) {
                parent = ((SoundVariant<?>) s).root;
            }
            int val = super.getSoundIndex(parent);
            if (var) {
                SoundVariant<?>[] arr = variants.get(parent);
                int offset = arr == null ? 0 : 1 + ReikaArrayHelper.indexOf(arr, s);
                if (offset == 0) {
                    DragonAPI.LOGGER.error("Could not find variant index for " + s + " in " + Arrays.toString(arr) + " from " + parent);
                }
                val |= (offset << 16);
            }
            return val;
        }

    }

    public static class SoundEnumSet {

        public final int index;
        public final Class<? extends SoundEnum> enumClass;
        protected final SoundEnum[] sounds;

        private SoundEnumSet(Class<? extends SoundEnum> c, int idx) {
            enumClass = c;
            sounds = c.getEnumConstants();
            index = idx;
        }

        protected SoundEnum getSound(int idx) {
            return sounds[idx];
        }

        public int getSoundIndex(SoundEnum s) {
            return s.ordinal();
        }

    }
}
