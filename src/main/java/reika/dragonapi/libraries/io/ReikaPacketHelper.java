package reika.dragonapi.libraries.io;

import com.google.common.collect.HashBiMap;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.PacketTypes;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.HybridTank;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.interfaces.PacketHandler;
import reika.dragonapi.interfaces.registry.CustomDistanceSound;
import reika.dragonapi.interfaces.registry.SoundEnum;
import reika.dragonapi.libraries.ReikaAABBHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.java.ReikaReflectionHelper;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ReikaPacketHelper {

    private static final HashMap<String, PacketPipeline> pipelines = new HashMap<>();
    private static final HashBiMap<Short, PacketHandler> handlers = HashBiMap.create();
    private static short handlerID = 0;

    public static void registerPacketHandler(DragonAPIMod mod, String channel, PacketHandler handler) {
        SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(mod.getModId(), channel.toLowerCase()), () -> DragonAPI.last_API_Version, DragonAPI.last_API_Version::equals, DragonAPI.last_API_Version::equals);
        PacketPipeline p = new PacketPipeline(mod, channel, handler, INSTANCE);
        p.registerPacket(DataPacket.class);
//        p.registerPacket(NBTPacket.class);
        handlers.put(handlerID, handler);
        pipelines.put(channel, p);
        handlerID++;
    }

    public static void registerPacketClass(String channel, Class<? extends PacketObj> c) {
        PacketPipeline pipe = pipelines.get(channel);
        if (pipe == null)
            throw new MisuseException("Cannot register a packet class to a null pipeline!");
        pipe.registerPacket(c);
    }

    private static short getHandlerID(PacketHandler handler) {
        return handlers.containsValue(handler) ? handlers.inverse().get(handler) : -1;
    }

    private static PacketHandler getHandlerFromID(short id) {
        return handlers.get(id);
    }

    /*
    public static void initPipelines() {
        for (PacketPipeline p : pipelines.values()) {
            p.initialize();
        }
    }
    public static void postInitPipelines() {
        for (PacketPipeline p : pipelines.values()) {
            p.postInitialize();
        }
    }*/

    public static void sendNIntPacket(String ch, int id, PacketTarget p, List<Integer> data) {
        int npars = 1 + data.size(); //+1 for the size

        ByteArrayOutputStream bos = new ByteArrayOutputStream(npars * 4); //4 bytes an int
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);
            outputStream.writeInt(data.size());
            if (data != null) {
                for (int i : data) {
                    outputStream.writeInt(i);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.PREFIXED, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        p.dispatch(pipe, pack);
    }

    public static void sendNIntPacket(String ch, int id, PacketTarget p, int... data) {
        sendNIntPacket(ch, id, p, ReikaJavaLibrary.makeIntListFromArray(data));
    }

    public static void sendRawPacket(String ch, ByteArrayOutputStream bos) {
        DataOutputStream outputStream = new DataOutputStream(bos);
        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.RAW, pipe);
        pack.setData(dat);
        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);

        } else if (side == Dist.CLIENT) {
            //PacketDispatcher.sendPacketToServer(packet);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendDataPacket(String ch, ByteArrayOutputStream bos) {
        DataOutputStream outputStream = new DataOutputStream(bos);
        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.DATA, pipe);
        pack.setData(dat);
        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);

        } else if (side == Dist.CLIENT) {
            //PacketDispatcher.sendPacketToServer(packet);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendDataPacket(String ch, int id, ServerPlayer ep, int... data) {
        ArrayList<Integer> li = new ArrayList<>();
        for (int datum : data) {
            li.add(datum);
        }
        sendDataPacket(ch, id, ep, li);
    }

    public static void sendDataPacket(String ch, int id, ServerPlayer ep, List<Integer> data) {
        int npars;
        if (data == null)
            npars = 4;
        else
            npars = data.size() + 4;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(npars * 4); //4 bytes an int
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);
            if (data != null)
                for (Integer datum : data) {
                    outputStream.writeInt(datum);
                }

            outputStream.writeInt(0);
            outputStream.writeInt(0); //xyz
            outputStream.writeInt(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.DATA, pipe);
        pack.setData(dat);

        //PacketDispatcher.sendPacketToPlayer(packet, (Player)ep);
        pipe.sendToPlayer(pack, ep);
    }

    public static void sendDataPacket(String ch, int id, BlockEntity te, ServerPlayer ep, int... data) {
        int npars;
        if (data == null)
            npars = 4;
        else
            npars = data.length + 4;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(npars * 4); //4 bytes an int
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);
            if (data != null) {
                for (int datum : data) {
                    outputStream.writeInt(datum);
                }
            }
            outputStream.writeInt(te.getBlockPos().getX());
            outputStream.writeInt(te.getBlockPos().getY());
            outputStream.writeInt(te.getBlockPos().getZ());

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.DATA, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();

        if (side == Dist.DEDICATED_SERVER) {
            pipe.sendToPlayer(pack, ep);
        } else if (side == Dist.CLIENT) {

        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendDataPacket(String ch, int id, Level world, int x, int y, int z, int radius, List<Integer> data) {
        int npars;
        if (data == null)
            npars = 4;
        else
            npars = data.size() + 4;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(npars * 4); //4 bytes an int
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);
            if (data != null)
                for (Integer datum : data) {
                    outputStream.writeInt(datum);
                }
            outputStream.writeInt(x);
            outputStream.writeInt(y);
            outputStream.writeInt(z);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.DATA, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();

        if (side == Dist.DEDICATED_SERVER) {
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);

            PacketTarget pt = new PacketTarget.RadiusTarget(world, x + 0.5, y + 0.5, z + 0.5, radius);
            pt.dispatch(pipe, pack);
        } else if (side == Dist.CLIENT) {
            //PacketDispatcher.sendPacketToServer(packet);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendDataPacket(String ch, int id, PacketTarget pt, List<Integer> data) {
        int npars;
        if (data == null)
            npars = 4;
        else
            npars = data.size() + 4;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(npars * 4); //4 bytes an int
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);
            if (data != null)
                for (Integer datum : data) {
                    outputStream.writeInt(datum);
                }
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.DATA, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();

        if (side == Dist.DEDICATED_SERVER) {
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);
            pt.dispatch(pipe, pack);
        } else if (side == Dist.CLIENT) {
            //PacketDispatcher.sendPacketToServer(packet);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendDataPacketToEntireServer(String ch, int id, List<Integer> data) {

        int npars;
        if (data == null)
            npars = 4;
        else
            npars = data.size() + 4;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(npars * 4); //4 bytes an int
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);
            if (data != null)
                for (Integer datum : data) {
                    outputStream.writeInt(datum);
                }
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.DATA, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();

        if (side == Dist.DEDICATED_SERVER) {
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);
            pipe.sendToAllOnServer(pack);
        } else if (side == Dist.CLIENT) {
            //PacketDispatcher.sendPacketToServer(packet);
            //pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendDataPacket(String ch, int id, Level world, int x, int y, int z, PacketTarget pt, List<Integer> data) {

        int npars;
        if (data == null)
            npars = 4;
        else
            npars = data.size() + 4;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(npars * 4); //4 bytes an int
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);
            if (data != null)
                for (Integer datum : data) {
                    outputStream.writeInt(datum);
                }
            outputStream.writeInt(x);
            outputStream.writeInt(y);
            outputStream.writeInt(z);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.DATA, pipe);
        pack.setData(dat);

        if (!world.isClientSide()) {
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);
            pt.dispatch(pipe, pack);
        } else if (world.isClientSide()) {
            //PacketDispatcher.sendPacketToServer(packet);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendLongDataPacket(String ch, int id, Level world, int x, int y, int z, List<Long> data) {
        int npars;
        if (data == null)
            npars = 4;
        else
            npars = data.size() + 4;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(((npars - 4) * 8) + 2 * 4); //4 bytes an int + 8 bytes a long
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);
            if (data != null) {
                for (Long datum : data) {
                    outputStream.writeLong(datum);
                }
            }
            outputStream.writeInt(x);
            outputStream.writeInt(y);
            outputStream.writeInt(z);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.DATA, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            // We are on the server side.
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);
            pipe.sendToDimension(pack, world);
        } else if (side == Dist.CLIENT) {
            // We are on the client side.
            //PacketDispatcher.sendPacketToServer(packet);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendUUIDPacket(String ch, int id, Level world, int x, int y, int z, UUID data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(2 * 8 + 3 * 4); //4 bytes an int + 8 bytes a long
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);
            outputStream.writeLong(data.getMostSignificantBits());
            outputStream.writeLong(data.getLeastSignificantBits());
            outputStream.writeInt(x);
            outputStream.writeInt(y);
            outputStream.writeInt(z);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.DATA, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            // We are on the server side.
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);
            pipe.sendToDimension(pack, world);
        } else if (side == Dist.CLIENT) {
            // We are on the client side.
            //PacketDispatcher.sendPacketToServer(packet);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendDataPacketToEntireServer(String ch, int id, int... data) {
        sendDataPacketToEntireServer(ch, id, ReikaJavaLibrary.makeIntListFromArray(data));
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendPacketToServer(String ch, int id, int... data) {
        sendDataPacket(ch, id, Minecraft.getInstance().level, 0, 0, 0, PacketTarget.server, ReikaJavaLibrary.makeIntListFromArray(data));
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendPacketToServer(String ch, int id, BlockEntity te, int... data) {
        sendDataPacket(ch, id, te.getLevel(), te.getBlockPos().getX(), te.getBlockPos().getY(), te.getBlockPos().getZ(), PacketTarget.server, ReikaJavaLibrary.makeIntListFromArray(data));
    }

    public static void sendDataPacketWithRadius(String ch, int id, Level world, BlockPos c, int radius, int... data) {
        sendDataPacket(ch, id, world, c.getX(), c.getY(), c.getZ(), radius, ReikaJavaLibrary.makeIntListFromArray(data));
    }

    public static void sendDataPacketWithRadius(String ch, int id, WorldLocation c, int radius, int... data) {
        sendDataPacket(ch, id, c.getWorld(), c.pos.getX(), c.pos.getY(), c.pos.getZ(), radius, ReikaJavaLibrary.makeIntListFromArray(data));
    }

    public static void sendDataPacketWithRadius(String ch, int id, BlockEntity te, int radius, int... data) {
        sendDataPacket(ch, id, te.getLevel(), te.getBlockPos().getX(), te.getBlockPos().getY(), te.getBlockPos().getZ(), radius, ReikaJavaLibrary.makeIntListFromArray(data));
    }

    public static void sendDataPacketWithRadius(String ch, int id, Entity e, int radius, int... data) {
        sendDataPacketWithRadius(ch, id, e.getLevel(), Mth.floor(e.getX()), Mth.floor(e.getY()), Mth.floor(e.getZ()), radius, data);
    }

    public static void sendDataPacket(String ch, int id, PacketTarget pt, int... data) {
        sendDataPacket(ch, id, pt, ReikaJavaLibrary.makeIntListFromArray(data));
    }

    public static void sendDataPacketWithRadius(String ch, int id, Level world, int x, int y, int z, int radius, int... data) {
        sendDataPacket(ch, id, world, x, y, z, new PacketTarget.RadiusTarget(world, x + 0.5, y + 0.5, z + 0.5, radius), ReikaJavaLibrary.makeIntListFromArray(data));
    }

    public static void sendDataPacket(String ch, int id, Level world, int x, int y, int z, PacketTarget pt, int... data) {
        sendDataPacket(ch, id, world, x, y, z, pt, ReikaJavaLibrary.makeIntListFromArray(data));
    }

    public static void sendLongDataPacket(String ch, int id, BlockEntity te, long data) {
        sendLongDataPacket(ch, id, te.getLevel(), te.getBlockPos().getX(), te.getBlockPos().getY(), te.getBlockPos().getZ(), ReikaJavaLibrary.makeListFrom(data));
    }

    public static void writeDirectSound(String ch, int id, Level world, double x, double y, double z, ResourceLocation name, float vol, float pitch, boolean scale) {
        int length = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);

            outputStream.writeDouble(x);
            outputStream.writeDouble(y);
            outputStream.writeDouble(z);

            writeResourceLocation(name, outputStream);

            outputStream.writeFloat(vol);
            outputStream.writeFloat(pitch);

            outputStream.writeBoolean(scale);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Sound Packet for sound '" + name + "' @ " + x + ", " + y + ", " + z + " threw a packet exception!");
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.FULLSOUND, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            // We are on the server side.
            //ServerPlayer player2 = (ServerPlayer) player;
            //PacketDispatcher.sendPacketToAllAround(x, y, z, 20, world.provider.dimensionId, packet);
            if (scale)
                pipe.sendToAllAround(pack, world, x, y, z, 20);
            else
                pipe.sendToAllOnServer(pack);
        } else if (side == Dist.CLIENT) {

        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendSoundPacket(SoundEnum s, Level world, double x, double y, double z, float vol, float pitch, boolean atten) {
        sendSoundPacket(s, world, x, y, z, vol, pitch, atten, getSoundDistance(atten, s));
    }

    private static int getSoundDistance(boolean atten, SoundEnum s) {
        if (atten) {
            float d = s instanceof CustomDistanceSound ? ((CustomDistanceSound) s).getAudibleDistance() : 16;
            return (int) Math.max(10, Math.max(d + 3, Math.min(d + 8, d * 1.25)));
        } else {
            return Integer.MAX_VALUE;
        }
    }

    public static void sendSoundPacket(SoundEnum s, Level world, double x, double y, double z, float vol, float pitch, boolean atten, int range) {
//        DragonAPI.LOGGER.info("Sending sound packet for "+s+" @ "+x+", "+y+", "+z);
        int length = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        DataOutputStream outputStream = new DataOutputStream(bos);
        ReikaSoundHelper.SoundEnumSet lib = ReikaSoundHelper.getSoundLibrary(s);
        if (lib == null) {
            DragonAPI.LOGGER.error("Could not find a sound library for " + s + "!");
            ReikaJavaLibrary.dumpStack();
            return;
        }
        try {
            outputStream.writeInt(lib.index);
            outputStream.writeInt(lib.getSoundIndex(s));
            outputStream.writeDouble(x);
            outputStream.writeDouble(y);
            outputStream.writeDouble(z);

            outputStream.writeFloat(vol);
            outputStream.writeFloat(pitch);

            outputStream.writeBoolean(atten);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(DragonAPI.packetChannel);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.SOUND, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            // We are on the server side.
            pipe.sendToAllAround(pack, world, x, y, z, range);
        } else if (side == Dist.CLIENT) {
            // We are on the client side.
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendStringPacket(String ch, int id, String sg, PacketTarget pt) {
        int length = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            writeString(sg, outputStream);
            outputStream.writeInt(id);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("String Packet for " + sg + " threw a packet exception!");
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.STRING, pipe);
        pack.setData(dat);

        pt.dispatch(pipe, pack);
    }

    public static void sendStringPacket(String ch, int id, String sg, BlockEntity te) {
        int x = te.getBlockPos().getX();
        int y = te.getBlockPos().getY();
        int z = te.getBlockPos().getZ();
        int length = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            writeString(sg, outputStream);
            outputStream.writeInt(id);
            outputStream.writeInt(x);
            outputStream.writeInt(y);
            outputStream.writeInt(z);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("String Packet for " + sg + " threw a packet exception!");
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.STRING, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            // We are on the server side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllInDimension(packet, te.level.provider.dimensionId);
            pipe.sendToDimension(pack, te.getLevel());
        } else if (side == Dist.CLIENT) {
            // We are on the client side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllInDimension(packet, te.level.provider.dimensionId);
            pipe.sendToServer(pack);
            //pipe.sendToDimension(pack, te.level); //SERVER ONLY
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendStringPacket(String ch, int id, String sg, Level world, int x, int y, int z) {
        int length = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            writeString(sg, outputStream);
            outputStream.writeInt(id);
            outputStream.writeInt(x);
            outputStream.writeInt(y);
            outputStream.writeInt(z);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("String Packet for " + sg + " threw a packet exception!");
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.STRING, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            // We are on the server side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);
            pipe.sendToDimension(pack, world);
        } else if (side == Dist.CLIENT) {
            // We are on the client side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendStringIntPacket(String ch, int id, ServerPlayer ep, String sg, int... data) {
        int length = data.length * 4;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            writeString(sg, outputStream);
            outputStream.writeInt(id);
            if (data != null) {
                for (int datum : data) {
                    outputStream.writeInt(datum);
                }
            }
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            //throw new RuntimeException("String Packet for "+sg+" threw a packet exception!");
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.STRINGINT, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            // We are on the server side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllPlayers(packet);
            pipe.sendToPlayer(pack, ep);
        } else if (side == Dist.CLIENT) {
            // We are on the client side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllPlayers(packet);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendStringIntPacket(String ch, int id, BlockEntity te, String sg, int... data) {
        int length = data.length * 4;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            writeString(sg, outputStream);
            outputStream.writeInt(id);
            if (data != null) {
                for (int datum : data) {
                    outputStream.writeInt(datum);
                }
            }
            outputStream.writeInt(te.getBlockPos().getX());
            outputStream.writeInt(te.getBlockPos().getY());
            outputStream.writeInt(te.getBlockPos().getZ());
        } catch (Exception ex) {
            ex.printStackTrace();
            //throw new RuntimeException("String Packet for "+sg+" threw a packet exception!");
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.STRINGINTLOC, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            // We are on the server side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllPlayers(packet);
            pipe.sendToAllAround(pack, te, 32);
        } else if (side == Dist.CLIENT) {
            // We are on the client side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllPlayers(packet);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendStringIntPacket(String ch, int id, PacketTarget p, String sg, int... data) {
        int length = data.length * 4;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            writeString(sg, outputStream);
            outputStream.writeInt(id);
            if (data != null) {
                for (int datum : data) {
                    outputStream.writeInt(datum);
                }
            }
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            //throw new RuntimeException("String Packet for "+sg+" threw a packet exception!");
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.STRINGINT, pipe);
        pack.setData(dat);

        p.dispatch(pipe, pack);
    }

    public static void sendStringPacket(String ch, int id, String sg) {
        int length = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            writeString(sg, outputStream);
            outputStream.writeInt(id);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            //throw new RuntimeException("String Packet for "+sg+" threw a packet exception!");
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.STRING, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            // We are on the server side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllPlayers(packet);
            pipe.sendToAllOnServer(pack);
        } else if (side == Dist.CLIENT) {
            // We are on the client side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllPlayers(packet);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendStringPacketWithRadius(String ch, int id, BlockEntity te, int radius, String sg) {
        int length = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            writeString(sg, outputStream);
            outputStream.writeInt(id);
            outputStream.writeInt(te.getBlockPos().getX());
            outputStream.writeInt(te.getBlockPos().getY());
            outputStream.writeInt(te.getBlockPos().getZ());
        } catch (Exception ex) {
            ex.printStackTrace();
            //throw new RuntimeException("String Packet for "+sg+" threw a packet exception!");
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.STRING, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            // We are on the server side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllPlayers(packet);
            pipe.sendToAllAround(pack, te, radius);
        } else if (side == Dist.CLIENT) {
            // We are on the client side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllPlayers(packet);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendUpdatePacket(String ch, int id, BlockEntity te, PacketTarget pt) {
        sendUpdatePacket(ch, id, te.getBlockPos().getX(), te.getBlockPos().getY(), te.getBlockPos().getZ(), pt);
    }

    public static void sendUpdatePacket(String ch, int id, int x, int y, int z, PacketTarget pt) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(20);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);
            outputStream.writeInt(x);
            outputStream.writeInt(y);
            outputStream.writeInt(z);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Packet " + ch + "/" + id + " @ " + x + "," + y + "," + z + " threw an update packet exception!");
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.UPDATE, pipe);
        pack.setData(dat);

        pt.dispatch(pipe, pack);
    }

    public static void sendFloatPacket(String ch, int id, BlockEntity te, float data) {
        sendFloatPacket(ch, id, te.getLevel(), te.getBlockPos().getX(), te.getBlockPos().getY(), te.getBlockPos().getZ(), data);
    }

    public static void sendFloatPacket(String ch, int id, Level world, int x, int y, int z, float data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(20);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);
            outputStream.writeFloat(data);
            outputStream.writeInt(x);
            outputStream.writeInt(y);
            outputStream.writeInt(z);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.FLOAT, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            // We are on the server side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);
            pipe.sendToDimension(pack, world);
        } else if (side == Dist.CLIENT) {
            // We are on the client side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendPositionPacket(String ch, int id, Entity e, PacketTarget pt, int... data) {
        sendPositionPacket(ch, id, e.getLevel(), e.getX(), e.getY(), e.getZ(), pt, data);
    }

    public static void sendPositionPacket(String ch, int id, Level world, double x, double y, double z, double r, int... data) {
        sendPositionPacket(ch, id, world, x, y, z, new PacketTarget.RadiusTarget(world, x, y, z, r), data);
    }

    public static void sendPositionPacket(String ch, int id, Level world, double x, double y, double z, PacketTarget pt, int... data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(16 + 4 * data.length);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            outputStream.writeInt(id);
            outputStream.writeDouble(x);
            outputStream.writeDouble(y);
            outputStream.writeDouble(z);
            for (int datum : data) outputStream.writeInt(datum);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.POS, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            // We are on the server side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);
            pt.dispatch(pipe, pack);
        } else if (side == Dist.CLIENT) {
            // We are on the client side.
            //PacketDispatcher.sendPacketToServer(packet);
            //PacketDispatcher.sendPacketToAllInDimension(packet, world.provider.dimensionId);
            pipe.sendToServer(pack);
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendSyncPacket(String ch, BlockEntity te, String field) {
        sendSyncPacket(ch, te, field, false);
    }

    public static void sendSyncPacket(String ch, BlockEntity te, String field, boolean forceClient) {
        int x = te.getBlockPos().getX();
        int y = te.getBlockPos().getY();
        int z = te.getBlockPos().getZ();
        int length = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        DataOutputStream outputStream = new DataOutputStream(bos);
        try {
            Field f = ReikaReflectionHelper.getProtectedInheritedField(te, field);
            f.setAccessible(true);
            Object obj = f.get(te);
            writeString(field, outputStream);
            outputStream.writeInt(x);
            outputStream.writeInt(y);
            outputStream.writeInt(z);
            PacketableData type = PacketableData.getType(obj);
            outputStream.writeInt(type.ordinal());
            type.write(outputStream, obj);
        } catch (IllegalAccessException | IOException ex) {
            ex.printStackTrace();
        }

        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        byte[] dat = bos.toByteArray();
        DataPacket pack = new DataPacket();
        pack.init(PacketTypes.SYNC, pipe);
        pack.setData(dat);

        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            //PacketDispatcher.sendPacketToAllInDimension(packet, te.level.provider.dimensionId);
            new PacketTarget.RadiusTarget(te, 24).dispatch(pipe, pack);
        } else if (side == Dist.CLIENT) {
            if (forceClient)
                PacketTarget.server.dispatch(pipe, pack);
            else
                DragonAPI.LOGGER.error(te + " sent a sync packet from the client! This is not allowed!");
        } else {
            // We are on the Bukkit server.
        }
    }

    private static enum PacketableData {
        INT("I", Integer.class),
        BOOLEAN("B", Boolean.class),
        DOUBLE("D", Double.class),
        FLOAT("F", Float.class),
        STRING("S", String.class);

        private final String id;
        private final Class type;

        private static final HashMap<String, PacketableData> typeMap = new HashMap();
        private static final HashMap<Class, PacketableData> classMap = new HashMap();
        private static final PacketableData[] list = values();

        PacketableData(String s, Class c) {
            id = s;
            type = c;
        }

        private void write(DataOutputStream out, Object obj) throws IOException {
            switch (this) {
                case BOOLEAN -> out.writeBoolean((boolean) obj);
                case DOUBLE -> out.writeDouble((double) obj);
                case FLOAT -> out.writeFloat((float) obj);
                case INT -> out.writeInt((int) obj);
                case STRING -> writeString((String) obj, out);
            }
        }

        private Object read(DataInputStream in) throws IOException {
            return switch (this) {
                case BOOLEAN -> in.readBoolean();
                case DOUBLE -> in.readDouble();
                case FLOAT -> in.readFloat();
                case INT -> in.readInt();
                case STRING -> readString(in);
            };
        }

        private static PacketableData getType(Object o) {
            return classMap.get(o.getClass());
        }

        private static PacketableData getType(String id) {
            return typeMap.get(id);
        }

        static {
            for (PacketableData packetableData : list) {
                typeMap.put(packetableData.id, packetableData);
                classMap.put(packetableData.type, packetableData);
            }
        }
    }

    public static void sendTankSyncPacket(String ch, BlockEntity te, String tankField) {
        var x = te.getBlockPos().getX();
        var y = te.getBlockPos().getY();
        var z = te.getBlockPos().getZ();
        var length = 0;
        var bos = new ByteArrayOutputStream(length);
        var outputStream = new DataOutputStream(bos);
        try {
            Field f = ReikaReflectionHelper.getProtectedInheritedField(te, tankField);
            f.setAccessible(true);
            HybridTank tank = (HybridTank) f.get(te);
            writeString(tankField, outputStream);
            outputStream.writeInt(x);
            outputStream.writeInt(y);
            outputStream.writeInt(z);
            outputStream.writeInt(tank.getFluidLevel());
        } catch (ClassCastException ex) {
//            ex.printStackTrace();
            DragonAPI.LOGGER.error(te + " tried to sync its tank, but it is not a HybridTank instance!");
        } catch (IllegalAccessException | IOException ex) {
            ex.printStackTrace();
        }

        var pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }

        var dat = bos.toByteArray();
        var pack = new DataPacket();
        pack.init(PacketTypes.TANK, pipe);
        pack.setData(dat);

        var side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            DragonAPI.LOGGER.info(te + " sent a tank sync packet from the server, this is good.");
            new PacketTarget.RadiusTarget(te, 24).dispatch(pipe, pack);
        } else if (side == Dist.CLIENT) {
            DragonAPI.LOGGER.error(te + " sent a sync packet from the client! This is not allowed!");
        } else {
            // We are on the Bukkit server.
        }
    }

    public static void sendNBTPacket(String ch, int id, CompoundTag nbt, PacketTarget pt) {
        DataPacket pack = getNBTPacket(id, nbt);
        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }
        pack.init(PacketTypes.NBT, pipe);
        Dist side = FMLLoader.getDist();
        pt.dispatch(pipe, pack);
    }

    public static void sendEntitySyncPacket(String ch, Entity e, double range) {
        CompoundTag nbt = new CompoundTag();
        e.save(nbt);
        nbt.putInt("dispatchID", e.getId());
        DataPacket pack = getNBTPacket(APIPacketHandler.PacketIDs.ENTITYSYNC.ordinal(), nbt);
        PacketPipeline pipe = pipelines.get(ch);
        if (pipe == null) {
            DragonAPI.LOGGER.error("Attempted to send a packet from an unbound channel!");
            ReikaJavaLibrary.dumpStack();
            return;
        }
        pack.init(PacketTypes.NBT, pipe);
        Dist side = FMLLoader.getDist();
        if (side == Dist.DEDICATED_SERVER) {
            pipe.sendToAllAround(pack, e, range);
        } else if (side == Dist.CLIENT) {

        } else {
            // We are on the Bukkit server.
        }
    }

    private static DataPacket getNBTPacket(int id, CompoundTag nbt) {
        DataPacket pack = new DataPacket();
        pack.setData(id, nbt);
        return pack;
    }

    public static void updateBlockEntityData(Level world, int x, int y, int z, String name, DataInputStream in) {
        if (world.hasChunksAt(x, y, z, x, y, z)) {
            BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));
            if (te == null) {
                DragonAPI.LOGGER.error("Null BlockEntity for syncing field " + name);
                return;
            }
            try {
                Field f = ReikaReflectionHelper.getProtectedInheritedField(te, name);
                if (f == null) {
                    //DragonAPI.LOGGER.info("Null field for syncing tank field "+name);
                    return;
                }
                Object data = PacketableData.list[in.readInt()].read(in);
                f.setAccessible(true);
                f.set(te, data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void updateBlockEntityTankData(Level world, int x, int y, int z, String name, int level) {
        if (world.hasChunksAt(x, y, z, x, y, z)) {
            BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));
            if (te == null) {
                DragonAPI.LOGGER.error("Null BlockEntity for syncing tank field " + name);
                return;
            }
            try {
                Field f = ReikaReflectionHelper.getProtectedInheritedField(te, name);
                if (f == null) {
                    //DragonAPI.LOGGER.info("Null field for syncing tank field "+name);
                    return;
                }
                f.setAccessible(true);
                HybridTank tank = (HybridTank) f.get(te);
                if (level <= 0) {
                    tank.empty();
                } else if (level > tank.getCapacity())
                    level = tank.getCapacity();

                if (tank.isEmpty()) {

                } else {
                    Fluid fluid = tank.getActualFluid().getFluid();
                    tank.setContents(level, fluid);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void writeString(String par0Str, DataOutput par1DataOutput) throws IOException {
        if (par0Str.length() > 32767) {
            throw new IOException("String too big");
        } else {
            par1DataOutput.writeShort(par0Str.length());
            par1DataOutput.writeChars(par0Str);
        }
    }

    private static void writeResourceLocation(ResourceLocation loc, DataOutput par1DataOutput) throws IOException {
        if (loc.toString().length() > 32767) {
            throw new IOException("String too big");
        } else {
            par1DataOutput.writeShort(loc.toString().length());
            par1DataOutput.writeChars(loc.toString());
        }
    }

    private static String readString(DataInput par0DataInput) throws IOException {
        short short1 = par0DataInput.readShort();

        if (short1 > Short.MAX_VALUE) {
            throw new IOException("Received string length longer than maximum allowed!");
        } else if (short1 < 0) {
            throw new IOException("Received string length is less than zero!");
        } else {
            StringBuilder stringbuilder = new StringBuilder();

            for (int j = 0; j < short1; ++j) {
                stringbuilder.append(par0DataInput.readChar());
            }

            return stringbuilder.toString();
        }
    }

    public static void writeString(FriendlyByteBuf data, String s) {
        data.writeInt(s.length());
        for (int i = 0; i < s.length(); i++) {
            data.writeChar(s.charAt(i));
        }
    }

    public static String readString(FriendlyByteBuf data) {
        int n = data.readInt();
        char[] dat = new char[n];
        for (int i = 0; i < n; i++) {
            dat[i] = data.readChar();
        }
        return new String(dat);
    }

    public static Packet<?> getPacket(String channel, PacketObj p) {
        PacketPipeline pipe = pipelines.get(channel);
        return pipe != null ? pipe.getMinecraftPacket(p) : null;
    }

    public static class DataPacket extends PacketObj {
        protected byte[] bytes;
        private DataInputStream in;

        public DataPacket() {
            super();
        }

        public DataPacket(FriendlyByteBuf data) {
//            super.decode(data);
            byte[] dat = data.array();
            bytes = new byte[dat.length - byteIndex - 1];
            System.arraycopy(dat, byteIndex + 1, bytes, 0, bytes.length);
            DragonAPI.LOGGER.info("received " + this);
        }

        private void setData(byte[] data) {
            bytes = new byte[data.length];
            System.arraycopy(data, 0, bytes, 0, bytes.length);
        }

        private void setData(int id, CompoundTag tag) {
            try {
                byte[] most = this.writeCompoundTagToBytes(tag);
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeInt(id);
                out.write(most);
                bytes = out.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void encode(FriendlyByteBuf data) {
            super.encode(data);
            data.writeBytes(bytes);
//            DragonAPI.LOGGER.info("sent " + this);
        }

        public CompoundTag asNBT() {
            try {
                byte[] abyte = new byte[bytes.length - 4]; //remove control int
                System.arraycopy(bytes, 4, abyte, 0, abyte.length);
                return this.readCompoundTagFromBuffer(abyte);
            } catch (IOException e) {
                return null;
            }
        }

        public int getSize() {
            return bytes.length;
        }

        public boolean isEmpty() {
            return this.getSize() == 0;
        }

        @Override
        public DataInputStream getDataIn() {
            if (in == null)
                in = new DataInputStream(new ByteArrayInputStream(bytes));
            return in;
        }

        @Override
        protected String getDataAsString() {
            return Arrays.toString(bytes);
        }
    }

    public static abstract class PacketObj {

        protected PacketHandler handler;
        protected PacketTypes type;
        protected int byteIndex = 0;

        protected PacketObj() {

        }

        protected PacketObj(FriendlyByteBuf data) {
        }

        public final void fromBytes(FriendlyByteBuf buf) {
            this.readData(buf);
        }

        public void readData(FriendlyByteBuf data) {
            short id = this.readShort(data);
            handler = getHandlerFromID(id);
            byte type = this.readByte(data);
            this.type = PacketTypes.getPacketType(type);
        }

        public final void toBytes(FriendlyByteBuf buf) {
            this.encode(buf);
        }

        public void init(PacketTypes p, PacketPipeline l) {
            type = p;
            handler = l.getHandler();
        }

        protected int readInt(FriendlyByteBuf data) {
            byteIndex += 4;
            return data.readInt();
        }

        protected short readShort(FriendlyByteBuf data) {
            byteIndex += 2;
            return data.readShort();
        }

        protected byte readByte(FriendlyByteBuf data) {
            byteIndex += 1;
            return data.readByte();
        }

        public void encode(FriendlyByteBuf data) {
            data.writeShort(getHandlerID(handler));
            data.writeByte(type.ordinal());
        }

        public boolean handleClient(Supplier<NetworkEvent.Context> ctx) {
            final var success = new AtomicBoolean(false);
            try {
                this.handler.handleData(this, Minecraft.getInstance().level, Minecraft.getInstance().player);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ctx.get().setPacketHandled(true);
            this.close();
            return success.get();
        }

        public boolean handleServer(Supplier<NetworkEvent.Context> ctx) {
            final var success = new AtomicBoolean(false);
            try {
                this.handler.handleData(this, ctx.get().getSender().getLevel(), ctx.get().getSender());
            } catch (Exception e) {
                e.printStackTrace();
            }
            ctx.get().setPacketHandled(true);
            this.close();
            return success.get();
        }

        @Override
        public String toString() {
            String hd = handler.getClass().getCanonicalName() + " (ID " + this.handlerID() + ")";
            return "type " + this.getType() + "; Data: " + this.getDataAsString() + " from " + hd;
        }

        protected abstract String getDataAsString();

        private void close() {
            try {
                this.getDataIn().close();
            } catch (IOException e) {
                DragonAPI.LOGGER.error("Error closing packet " + this + ". Memory may leak.");
                e.printStackTrace();
            }
        }

        public abstract DataInputStream getDataIn();

        public final String readString() {
            try {
                return ReikaPacketHelper.readString(this.getDataIn());
            } catch (IOException e) {
                e.printStackTrace();
                return "ERROR";
            }
        }

        protected final byte[] writeCompoundTagToBytes(CompoundTag tag) throws IOException {
            ByteArrayDataOutput buf = ByteStreams.newDataOutput();
            if (tag == null)
                buf.writeInt(-1);
            else {
                byte[] abyte = compress(tag);
                buf.writeInt(abyte.length);
                buf.write(abyte); //todo potentially broken
            }
            return buf.toByteArray();
        }

        public static byte[] compress(CompoundTag tag) throws IOException {
            ByteArrayOutputStream var1 = new ByteArrayOutputStream();
            try (DataOutputStream var2 = new DataOutputStream(new GZIPOutputStream(var1))) {
                NbtIo.write(tag, var2);
            }
            return var1.toByteArray();
        }

        protected final CompoundTag readCompoundTagFromBuffer(byte[] bytes) throws IOException {
            ByteArrayDataInput buf = ByteStreams.newDataInput(bytes);
            int short1 = buf.readInt();
            if (short1 < 0)
                return null;
            else {
                byte[] abyte = new byte[short1];
                buf.readFully(abyte);
                return read(abyte, NbtAccounter.UNLIMITED); //todo potentially broken
            }
        }

        public static CompoundTag read(byte[] p_152457_0_, NbtAccounter p_152457_1_) throws IOException {
            DataInputStream var2 = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(p_152457_0_))));
            CompoundTag var3;
            try {
                var3 = NbtIo.read(var2, p_152457_1_);
            } finally {
                var2.close();
            }
            return var3;
        }

        public final PacketTypes getType() {
            return type;
        }

        protected final int handlerID() {
            return handlers.inverse().get(handler);
        }
    }

//    public static void registerVanillaPacketType(DragonAPIMod mod, int id, Class<? extends Packet> c, Dist s, EnumConnectionState state) {
//        switch(s) {
//            case CLIENT:
//                if (state.func_150753_a().containsKey(id))
//                    throw new IDConflictException(mod, "Packet "+c+" ID "+id+" is already occupied by "+state.func_150753_a().get(id)+"!");
//                state.func_150753_a().put(Integer.valueOf(id), c);
//                break;
//            case DEDICATED_SERVER:
//                if (state.func_150755_b().containsKey(id))
//                    throw new IDConflictException(mod, "Packet "+c+" ID "+id+" is already occupied by "+state.func_150755_b().get(id)+"!");
//                state.func_150755_b().put(Integer.valueOf(id), c);
//                break;
//        }
//        EnumConnectionState.field_150761_f.put(c, state);
//        mod.getModLogger().log("Registering vanilla-type packet "+c+" with ID "+id+" on side "+s);
//    }

    public static void syncBlockEntity(BlockEntity tile) {
        CompoundTag NBT = new CompoundTag();
        tile.load(NBT); //todo was save
        List<ServerPlayer> li = tile.getLevel().getEntitiesOfClass(ServerPlayer.class, ReikaAABBHelper.getBlockAABB(tile.getBlockPos().getX(), tile.getBlockPos().getY(), tile.getBlockPos().getZ()).expandTowards(4, 4, 4)); //todo inflate or expandtowards
        for (ServerPlayer ep : li)
            sendNBTPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.VTILESYNC.ordinal(), NBT, new PacketTarget.PlayerTarget(ep));
    }
}
