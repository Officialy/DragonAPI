package reika.dragonapi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import reika.dragonapi.auxiliary.ModularLogger;
import reika.dragonapi.auxiliary.PacketTypes;
import reika.dragonapi.auxiliary.PopupWriter;
import reika.dragonapi.auxiliary.trackers.CommandableUpdateChecker;
import reika.dragonapi.auxiliary.trackers.KeyWatcher;
import reika.dragonapi.auxiliary.trackers.ModFileVersionChecker;
import reika.dragonapi.auxiliary.trackers.SettingInterferenceTracker;
import reika.dragonapi.base.BlockEntityBase;
import reika.dragonapi.command.BiomeMapCommand;
import reika.dragonapi.command.EntityListCommand;
import reika.dragonapi.instantiable.effects.StringParticleFX;
import reika.dragonapi.instantiable.event.RawKeyPressEvent;
import reika.dragonapi.instantiable.event.client.ClientLoginEvent;
import reika.dragonapi.instantiable.event.client.ClientLogoutEvent;
import reika.dragonapi.instantiable.event.client.PlayerInteractEventClient;
import reika.dragonapi.interfaces.PacketHandler;
import reika.dragonapi.interfaces.registry.SoundEnum;
import reika.dragonapi.libraries.ReikaEntityHelper;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.io.ReikaSoundHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.registry.ReikaParticleHelper;
import reika.dragonapi.libraries.rendering.ReikaRenderHelper;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Random;

public class APIPacketHandler implements PacketHandler {

    private final Random rand = new Random();

    protected PacketIDs pack;

    public void handleData(ReikaPacketHelper.PacketObj packet, Level world, Player ep) {
        DataInputStream inputStream = packet.getDataIn();

        int control;
        int len;
        int[] data = new int[0];
        long longdata = 0;
        float floatdata = 0;
        int x = 0;
        int y = 0;
        int z = 0;
        boolean readinglong;
        CompoundTag NBT = null;
        String stringdata = null;
        PacketTypes packetType = packet.getType();
        // DragonAPI.LOGGER.info("Packet: {}{}", packet, inputStream.toString());
        try {
            switch (packetType) {
                case SOUND -> {
                    // Check if there's enough data for a complete sound packet
                    // A complete sound packet needs: 2 ints + 3 doubles + 2 floats + 1 boolean
                    // = 8 + 24 + 8 + 1 = 41 bytes minimum
                    int availableBytes = inputStream.available();
                    DragonAPI.LOGGER.debug("SOUND packet received with " + availableBytes + " bytes available");
                    
                    if (availableBytes < 41) {
                        DragonAPI.LOGGER.warn("Received an incomplete SOUND packet (only " + availableBytes + " bytes available, need 41). Packet details: " + packet.toString());
                        return;
                    }
                    try {
                        int lib = inputStream.readInt();
                        control = inputStream.readInt();
                        DragonAPI.LOGGER.debug("SOUND packet: lib=" + lib + ", control=" + control);
                        
                        SoundEnum s = ReikaSoundHelper.lookupSound(lib, control);
                        if (s == null) {
                            DragonAPI.LOGGER.warn("Received SOUND packet with invalid sound enum (lib=" + lib + ", control=" + control + "). Skipping.");
                            return;
                        }
                        double sx = inputStream.readDouble();
                        double sy = inputStream.readDouble();
                        double sz = inputStream.readDouble();
                        float v = inputStream.readFloat();
                        float p = inputStream.readFloat();
                        boolean att = inputStream.readBoolean();
                        
                        DragonAPI.LOGGER.debug("SOUND packet data: pos=(" + sx + "," + sy + "," + sz + "), vol=" + v + ", pitch=" + p + ", atten=" + att);
                        
                        ReikaSoundHelper.playClientSound(s, sx, sy, sz, v, p, att);
                    } catch (EOFException e) {
                        DragonAPI.LOGGER.warn("Received truncated SOUND packet. Available bytes: " + inputStream.available() + ". Error: " + e.getMessage());
                    }
                    return;
                }
                case FULLSOUND, UPDATE -> {
                    control = inputStream.readInt();
                    pack = PacketIDs.getEnum(control);
                }
                case STRING -> {
                    stringdata = packet.toString();
                    control = inputStream.readInt();
                    pack = PacketIDs.getEnum(control);
                }
                case DATA, RAW -> {
                    try {
                        control = inputStream.readInt();
                        pack = PacketIDs.getEnum(control);
                        len = pack.getNumberDataInts();
                        data = new int[len];
                        readinglong = pack.isLongPacket();
                        
                        // Check if we have enough data available
                        int bytesNeeded = readinglong ? 8 : (len * 4);
                        if (inputStream.available() < bytesNeeded) {
                            DragonAPI.LOGGER.warn("Received incomplete DATA packet for " + pack + " (needed " + bytesNeeded + " bytes, only " + inputStream.available() + " available). Skipping.");
                            return;
                        }
                        
                        if (!readinglong) {
                            for (int i = 0; i < len; i++) {
                                data[i] = inputStream.readInt();
                            }
                        } else {
                            longdata = inputStream.readLong();
                        }
                    } catch (EOFException e) {
                        DragonAPI.LOGGER.warn("Received truncated DATA packet for " + (pack != null ? pack : "unknown") + ". Skipping.");
                        return;
                    }
                }
                case FLOAT -> {
                    control = inputStream.readInt();
                    pack = PacketIDs.getEnum(control);
                    floatdata = inputStream.readFloat();
                }
                case SYNC -> {
                    String name = packet.toString();
                    x = inputStream.readInt();
                    y = inputStream.readInt();
                    z = inputStream.readInt();
                    ReikaPacketHelper.updateBlockEntityData(world, x, y, z, name, inputStream);
                    return;
                }
                case TANK -> {
                    String tank = packet.toString();
                    x = inputStream.readInt();
                    y = inputStream.readInt();
                    z = inputStream.readInt();
                    int level = inputStream.readInt();
                    ReikaPacketHelper.updateBlockEntityTankData(world, x, y, z, tank, level);
                    return;
                }
                case NBT -> {
                    control = inputStream.readInt();
                    pack = PacketIDs.getEnum(control);
                    NBT = ((ReikaPacketHelper.DataPacket) packet).asNBT();
                }
                case STRINGINT -> {
                    stringdata = packet.toString();
                    control = inputStream.readInt();
                    pack = PacketIDs.getEnum(control);
                    data = new int[pack.getNumberDataInts()];
                    for (int i = 0; i < data.length; i++)
                        data[i] = inputStream.readInt();
                }
                case BE_NBT_SYNC -> {
                    // 26.1: routed via the new dedicated BE-NBT-sync type (see PacketTypes / SyncPacket).
                    // Wire format inside the DataPacket's payload bytes — written by
                    // {@link reika.dragonapi.instantiable.io.SyncPacket#encode}:
                    //   - BlockPos        (FriendlyByteBuf#writeBlockPos = packed long, 8 bytes)
                    //   - varInt typeId   (BlockEntityType registry id, currently unused on the receive
                    //                      side but kept in case we want to add a type-mismatch guard)
                    //   - NBT compound    (FriendlyByteBuf#writeNbt)
                    // We can't decode that off the plain DataInputStream — varInt and writeNbt are
                    // FriendlyByteBuf-format — so wrap the DataPacket's raw bytes into a
                    // FriendlyByteBuf and decode through that. The previous routing used the SYNC
                    // case below which reads readUTF + 3×readInt + reflection field-write, which
                    // for SyncPacket data was reading bits of the packed BlockPos as a UTF length
                    // and immediately falling into "unknown field" territory — silently dropping
                    // every periodic BE sync after tick 20. With this case, runtime BE NBT updates
                    // now reach the client correctly: reservoir tanks fill on the client, pipe
                    // fluid renders, etc.
                    if (packet instanceof ReikaPacketHelper.DataPacket dp) {
                        byte[] body = dp.getBytes();
                        net.minecraft.network.FriendlyByteBuf bbuf =
                                new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(body));
                        try {
                            BlockPos pos = bbuf.readBlockPos();
                            bbuf.readVarInt(); // typeId — unused for now, kept for forward compat
                            CompoundTag nbt = bbuf.readNbt();
                            if (nbt != null) {
                                BlockEntity te = world.getBlockEntity(pos);
                                if (te instanceof BlockEntityBase beb) {
                                    beb.applySyncTag(nbt);
                                } else if (te == null) {
                                    // Common case during chunk-edge resync; not worth a log line.
                                } else {
                                    DragonAPI.LOGGER.debug(
                                            "BE_NBT_SYNC for {} dispatched but BE at {} is {} (not BlockEntityBase)",
                                            world, pos, te.getClass().getSimpleName());
                                }
                            }
                        } catch (Exception decodeErr) {
                            DragonAPI.LOGGER.error("Failed to decode BE_NBT_SYNC payload", decodeErr);
                        }
                    } else {
                        DragonAPI.LOGGER.error("BE_NBT_SYNC dispatched on non-DataPacket {}", packet);
                    }
                    return;
                }
                default -> {
                }
            }
            if (packetType.hasCoordinates()) {
                x = inputStream.readInt();
                y = inputStream.readInt();
                z = inputStream.readInt();
            }
        } catch (IOException e) {
            DragonAPI.LOGGER.error("Error when handling " + packet.getType() + " packet [" + packet + "]: " + e);
            e.printStackTrace();
            return;
        }
        try {
            switch (pack) {
                case BLOCKUPDATE:
                    //ReikaJavaLibrary.pConsole(x+", "+y+", "+z, Dist.CLIENT);
                    world.sendBlockUpdated(new BlockPos(x, y, z), world.getBlockState(new BlockPos(x, y, z)), world.getBlockState(new BlockPos(x, y, z)), 3); //todo check if this works
//             todo figure out this func       world.func_147479_m(x, y, z);
                    break;
                case PARTICLE:
                case PARTICLEWITHPOS:
                case PARTICLEWITHPOSVEL:
                    if (data[0] >= 0 && data[0] < ReikaParticleHelper.particleList.length) {
                        double px;
                        double py;
                        double pz;
                        if (pack == PacketIDs.PARTICLE) {
                            px = x + rand.nextDouble();
                            py = y + rand.nextDouble();
                            pz = z + rand.nextDouble();
                        } else {
                            px = ReikaJavaLibrary.buildDoubleFromInts(data[2], data[3]);
                            py = ReikaJavaLibrary.buildDoubleFromInts(data[4], data[5]);
                            pz = ReikaJavaLibrary.buildDoubleFromInts(data[6], data[7]);
                        }
                        double vx = 0;
                        double vy = 0;
                        double vz = 0;
                        if (pack == PacketIDs.PARTICLEWITHPOSVEL) {
                            vx = ReikaJavaLibrary.buildDoubleFromInts(data[8], data[9]);
                            vy = ReikaJavaLibrary.buildDoubleFromInts(data[10], data[11]);
                            vz = ReikaJavaLibrary.buildDoubleFromInts(data[12], data[13]);
                        }
                        for (int i = 0; i < data[1]; i++) {
                            ReikaParticleHelper p = ReikaParticleHelper.particleList[data[0]];
                            world.addParticle(p.particle, px, py, pz, vx, vy, vz);
                        }
                    }
                    break;
                case BIOMECHANGE:
                    var biomeList = world.registryAccess().lookupOrThrow(Registries.BIOME).listElements().toList();
                    if (data[0] >= 0 && data[0] < biomeList.size()) {
                        ReikaWorldHelper.setBiomeForXZ(world, x, z, biomeList.get(data[0]).value());
                    }
//                    world.markBlockRangeForRenderUpdate(x, 0, z, x, world.getHeight(), z);
                    world.sendBlockUpdated(new BlockPos(x, 0, z), world.getBlockState(new BlockPos(x, world.getHeight(), z)), world.getBlockState(new BlockPos(x, world.getHeight(), z)), 3);
                    break;
                case KEYUPDATE:
                    if (data.length < 2) {
                        DragonAPI.LOGGER.error("Caught key packet missing data (len=" + data.length + ")! Packet=" + packet);
                        break;
                    }
                    int ordinal = data[0];
                    boolean used = data[1] > 0;
                    if (ordinal < 0 || ordinal > KeyWatcher.Key.keyList.length) {
                        DragonAPI.LOGGER.error("Caught key packet for key #" + ordinal + " (use=" + used + "), yet no such key exists. Packet=" + packet);
                        break;
                    }
                    KeyWatcher.Key key = KeyWatcher.Key.keyList[ordinal];
                    KeyWatcher.instance.setKey(ep, key, used);
                    NeoForge.EVENT_BUS.post(new RawKeyPressEvent(key, ep));
                    break;
                case TILESYNC:
                    BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));
                    if (te instanceof BlockEntityBase && !world.isClientSide()) {
                        BlockEntityBase tile = (BlockEntityBase) te;
                        tile.syncAllData(data[0] > 0);
                    }
                    break;
                case VTILESYNC:
                    int tx = NBT.getIntOr("x", 0);
                    int ty = NBT.getIntOr("y", 0);
                    int tz = NBT.getIntOr("z", 0);
                    BlockEntity tile = world.getBlockEntity(new BlockPos(tx, ty, tz));
                    //ReikaJavaLibrary.pConsole(((Container)tile).getStackInSlot(0));
                    if (tile != null) {
                        try (var scopedCollector = new ProblemReporter.ScopedCollector(tile.problemPath(), LoggerFactory.getLogger(DragonAPI.class))) {
                            tile.loadWithComponents(TagValueInput.create(scopedCollector, world.registryAccess(), NBT));
                        }
                    }
                    break;
                case TILEDELETE:
                    world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    break;
                case PLAYERDATSYNC:
                case PLAYERDATSYNC_CLIENT:
                    // Player NBT sync - use ValueInput API directly
                    try (var scopedCollector = new ProblemReporter.ScopedCollector(ep.problemPath(), LoggerFactory.getLogger(DragonAPI.class))) {
                        ep.load(TagValueInput.create(scopedCollector, world.registryAccess(), NBT));
                    }
                    break;/*=
			case PLAYERATTRSYNC:
				for (Object o : NBT.func_150296_c()) { //Double tags
					String name = (String)o;
					NBTBase tag = NBT.getTag(name);
					BaseAttributeMap map = ep.getAttributeMap();
				}
				break;*/
                case PLAYERDATSYNCREQ_CLIENT:
                    ReikaPlayerAPI.syncCustomData((ServerPlayer) ep);
                    break;
                case RERENDER:
                    ReikaRenderHelper.rerenderAllChunksLazily(); //todo was rerenderAllChunks
                    break;
                case COLOREDPARTICLE:
                    ReikaParticleHelper.spawnColoredParticlesWithOutset(world, new BlockPos(x, y, z), data[0], data[1], data[2], data[3], data[4] / 16D);
                    break;
                case NUMBERPARTICLE:
                case STRINGPARTICLE:
                case REDSTONECHANGE:
                case SENDLATENCY:
                case POPUP:
                case LOGOUT:
                case OREDUMP:
                case MODLOCK:
                case CLEARCHAT:
                case GUIRELOAD:
                case ITEMDROPPER:
                case BREAKPARTICLES:
                case LOGIN:
                case OLDMODS:
                case EXPLODE:
                case IDDUMP:
                case ENTITYDUMP:
                    break;
                case SERVERSOUND:
                    if (world.isClientSide()) {
                        double dx = inputStream.readDouble();
                        double dy = inputStream.readDouble();
                        double dz = inputStream.readDouble();
                        SoundEvent name = SoundEvent.createVariableRangeEvent(Identifier.tryParse(packet.readString())); //todo check if this works
                        float vol = inputStream.readFloat();
                        float pitch = inputStream.readFloat();
                        boolean flag = inputStream.readBoolean();
                        ReikaSoundHelper.playNormalClientSound(world, dx, dy, dz, name, vol, pitch, flag);
                    }
                    break;
                case PLAYERKICK:
                    ((ServerPlayer) ep).connection.disconnect(Component.literal(stringdata));
                    break;
                case ITEMDROPPERREQUEST: {
                    Entity e = world.getEntity(data[0]);
                    if (e instanceof ItemEntity) {
                        var entityOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, world.registryAccess());
                        e.saveWithoutId(entityOutput);
                        var entityNBT = entityOutput.buildResult();
                        if (entityNBT.contains("dropper")) {
                            String s = entityNBT.getStringOr("dropper", "");
                            //ReikaJavaLibrary.pConsole("Received request for Entity ID "+data[0]+"; response = '"+s+"'");
                            if (!s.isEmpty()) {
                                ReikaPacketHelper.sendStringIntPacket(DragonAPI.packetChannel, PacketIDs.ITEMDROPPER.ordinal(), (ServerPlayer) ep, s, data[0]);
                            }
                        }
                    }
                    break;
                }
                case PLAYERINTERACT:
                    NeoForge.EVENT_BUS.post(new PlayerInteractEventClient(ep, PlayerInteractEventClient.Result.values()[data[4]], data[0], data[1], data[2], data[3], world));
                    break;
                case BIOMEPNGSTART:
                    BiomeMapCommand.startCollecting(data[0], stringdata, world.dimension()/*todo old dimension id's data[1]*/, data[2], data[3], data[4], data[5], data[6], data[7] > 0);
                    break;
                case BIOMEPNGDAT:
                    int hash = data[0];
                    for (int i = 0; i < BiomeMapCommand.PACKET_COMPILE; i++) {
                        int a = 1 + i * 3;
                        BiomeMapCommand.addBiomePoint(hash, data[a], data[a + 1], data[a + 2]);
                    }
                    break;
                case BIOMEPNGEND:
                    BiomeMapCommand.finishCollectingAndMakeImage(data[0]);
                    break;
                case FILEMATCH:
                    ModFileVersionChecker.instance.checkFiles((ServerPlayer) ep, stringdata);
                    break;
                case ENTITYSYNC: {
                    int id = NBT.getIntOr("dispatchID", 0);
                    Entity e = world.getEntity(id);
                    if (e != null) {
                        try (var scopedCollector = new ProblemReporter.ScopedCollector(e.problemPath(), LoggerFactory.getLogger(DragonAPI.class))) {
                            e.load(TagValueInput.create(scopedCollector, world.registryAccess(), NBT));
                        }
                    } else {
                        DragonAPI.LOGGER.error("Entity does not exist clientside to be synced!");
                    }
                    break;
                }
                case MODULARLOGGER:
                    ModularLogger.instance.setState(stringdata, data[0] > 0);
                    break;
                case GETLATENCY:
                    int[] l = ReikaJavaLibrary.splitLong(System.currentTimeMillis());
                    ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, PacketIDs.SENDLATENCY.ordinal(), (ServerPlayer) ep, data[0], data[1], l[0], l[1]);
                    break;
                case ENTITYVERIFY:
                    ReikaEntityHelper.performEntityVerification((ServerPlayer) ep, data[0], world.dimension()/*data[1]*/, data[2]); //todo dimension is null for now as i need to figure out how to get it with the new system
                    break;
                case ENTITYVERIFYFAIL:
                    Entity e = world.getEntity(data[0]);
                    if (e != null && world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        e.kill(serverLevel);
                        DragonAPI.LOGGER.info("Removing client-only entity " + e);
                    } else if (e != null) {
                        // Client-side: use remove instead
                        e.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                        DragonAPI.LOGGER.info("Removing client-only entity " + e);
                    }
                    break;
            }
            if (world.isClientSide())
                this.clientHandle(Minecraft.getInstance().level, x, y, z, pack, data, stringdata, ep);
        } catch (Exception e) {
            DragonAPI.LOGGER.error("Error when handling " + pack + " packet [" + packet + "]: " + e);
            e.printStackTrace();
        }
    }

    private void clientHandle(ClientLevel world, int x, int y, int z, PacketIDs pack, int[] data, String sg, Player player) {
        switch (pack) {
            case NUMBERPARTICLE ->
                    Minecraft.getInstance().particleEngine.add(new StringParticleFX(world, x + 0.5, y + 0.5, z + 0.5, String.valueOf(data[0]), 0, 0, 0));
            case STRINGPARTICLE -> {
                StringParticleFX fx = new StringParticleFX(world, x + 0.5, y + 0.5, z + 0.5, sg, 0, 0, 0);
                fx.setLife(Math.max(15, 3 * sg.length()));
                fx.setScale(Math.max(0.01F, Math.min(1, 0.5F / sg.length())));
                Minecraft.getInstance().particleEngine.add(fx);
            }
            case ENTITYDUMP -> EntityListCommand.dumpClientside();
            case EXPLODE -> {
                ReikaSoundHelper.playSoundAtBlock(world, x, y, z, SoundEvents.GENERIC_EXPLODE.value());
                ReikaParticleHelper.EXPLODE.spawnAroundBlock(world, new BlockPos(x, y, z), 1);
            }
            case OLDMODS -> CommandableUpdateChecker.instance.onClientReceiveOldModID(sg);
            case LOGIN -> {
                NeoForge.EVENT_BUS.post(new ClientLoginEvent(player, data[0] > 0));
                SettingInterferenceTracker.instance.onLogin(player);
            }
            case LOGOUT -> NeoForge.EVENT_BUS.post(new ClientLogoutEvent(player));
            case BREAKPARTICLES -> {
                Block b = Block.stateById(data[0]).getBlock();
                ReikaRenderHelper.spawnDropParticles(world, x, y, z, b, data[1]);
            }
            case ITEMDROPPER -> {
                Entity e = world.getEntity(data[0]);
                if (e instanceof ItemEntity) {
                    e.getPersistentData().putString("dropper", sg);
                }
            }
            case GUIRELOAD -> {
//                if (Minecraft.getInstance().screen != null)
//                    Minecraft.getInstance().screen.initGui();
            }
            case POPUP -> PopupWriter.instance().addMessage(new PopupWriter.Warning(sg, data[0]));
            case SENDLATENCY -> {
                long t3 = System.currentTimeMillis();
                long t1 = ReikaJavaLibrary.buildLong(data[0], data[1]);
                long t2 = ReikaJavaLibrary.buildLong(data[2], data[3]);
                long toServerTime = t2 - t1;
                long toClientTime = t3 - t2;
                ReikaChatHelper.write("Total latency: " + toServerTime + "ms to server, " + toClientTime + "ms from server.");
            }
            case REDSTONECHANGE ->
                    ((BlockEntityBase) world.getBlockEntity(new BlockPos(x, y, z))).onRedstoneChangedClientside(data[0] > 0, data[1] > 0);
            case CLEARCHAT -> ReikaChatHelper.clearChat();

//            case MODLOCK:
//                ModLockController.instance.readSync(player, sg);
//                break;
//            case OREDUMP:
//                OreDumpCommand.dumpClientside(sg);
//                break;
            default -> {
            }
        }
    }

    public enum PacketIDs {
        BIOMECHANGE(),
        BLOCKUPDATE(),
        PARTICLE(),
        PARTICLEWITHPOS(),
        PARTICLEWITHPOSVEL(),
        KEYUPDATE(),
        TILESYNC(),
        VTILESYNC(),
        TILEDELETE(),
        PLAYERDATSYNC(),
        PLAYERDATSYNC_CLIENT(),
        PLAYERDATSYNCREQ_CLIENT(),
        RERENDER(),
        COLOREDPARTICLE(),
        NUMBERPARTICLE(),
        STRINGPARTICLE(),
        IDDUMP(),
        ENTITYDUMP(),
        EXPLODE(),
        OLDMODS(),
        LOGIN(),
        LOGOUT(),
        SERVERSOUND(),
        BREAKPARTICLES(),
        PLAYERKICK(),
        CONFIGSYNC(),
        CONFIGSYNCSTART(),
        CONFIGSYNCEND(),
        ITEMDROPPER(),
        ITEMDROPPERREQUEST(),
        PLAYERINTERACT(),
        GUIRELOAD(),
        BIOMEPNGSTART(),
        BIOMEPNGDAT(),
        BIOMEPNGEND(),
        FILEMATCH(),
        ENTITYSYNC(),
        MODULARLOGGER(),
        POPUP(),
        GETLATENCY(),
        SENDLATENCY(),
        REDSTONECHANGE(),
        ENTITYVERIFY(),
        ENTITYVERIFYFAIL(),
        CLEARCHAT(),
        MODLOCK(),
        OREDUMP();

        public static PacketIDs getEnum(int index) {
            return PacketIDs.values()[index];
        }

        public boolean isLongPacket() {
            return false;
        }

        public boolean hasLocation() {
            return this != KEYUPDATE && this != PLAYERKICK && this != CONFIGSYNC && this != CONFIGSYNCEND && this != FILEMATCH && this != MODULARLOGGER && this != POPUP;
        }

        public int getNumberDataInts() {
            return switch (this) {
                case PARTICLE, ENTITYVERIFYFAIL, REDSTONECHANGE, GETLATENCY, BREAKPARTICLES, KEYUPDATE -> 2;
                case PARTICLEWITHPOS -> 2 + 2 * 3;
                case PARTICLEWITHPOSVEL -> 2 + 2 * 6;
                case NUMBERPARTICLE, POPUP, MODULARLOGGER, LOGIN, BIOMEPNGEND, IDDUMP, CONFIGSYNC, ITEMDROPPER,
                     ITEMDROPPERREQUEST, TILESYNC, BIOMECHANGE -> 1;
                case COLOREDPARTICLE, PLAYERINTERACT -> 5;
                case BIOMEPNGSTART -> 8;
                case BIOMEPNGDAT -> 1 + 3 * BiomeMapCommand.PACKET_COMPILE;
                case SENDLATENCY -> 4;
                case ENTITYVERIFY -> 3;
                default -> 0;
            };
        }
    }
}
