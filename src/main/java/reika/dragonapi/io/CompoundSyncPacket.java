package reika.dragonapi.io;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.auxiliary.trackers.TickRegistry;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.interfaces.DataSync;

import java.io.IOException;
import java.util.*;

public class CompoundSyncPacket implements DataSync, Packet {

    private static final String ERROR_TAG = "erroredPacket";

    public static final CompoundSyncPacket instance = new CompoundSyncPacket();

    private final HashMap<WorldLocation, HashMap<String, Tag>> data = new HashMap();
    private final HashMap<WorldLocation, HashMap<String, Tag>> oldData = new HashMap();
    private final HashMap<WorldLocation, HashMap<String, Tag>> changes = new HashMap();

    /** Is the packet currently being written by the network thread */
    private boolean dispatch;
    /** Is the packet currently being read by the network thread */
    private boolean receive;

    private CompoundSyncPacket() {

    }

    public void setData(BlockEntity te, boolean force, CompoundTag NBT) {
        if (dispatch) {
            if (DragonOptions.LOGSYNCCME.getState()) {
                DragonAPI.LOGGER.info("The compound sync packet for "+te+" would have just CME'd, as the");
                DragonAPI.LOGGER.info("Server-Thread data-writing code has overlapped with the Network-Thread byte[] dispatch.");
                DragonAPI.LOGGER.info("Seeing this message frequently could indicate a serious issue.\n");
            }
            return;
        }

        WorldLocation loc = new WorldLocation(te);

        this.createMaps(loc);
        changes.remove(loc);

        Collection<String> c = NBT.getAllKeys();
        Iterator<String> it = c.iterator();
        while (it.hasNext()) {
            String name = it.next();
            if (name == null) {
                DragonAPI.LOGGER.info("An NBT tag with a null key is being sent to the compound sync packet from "+te);
            }
            else {
                Tag tag = NBT.get(name);
                this.addData(loc, name, tag, force);
            }
        }
    }

    private void createMaps(WorldLocation loc) {
        if (data.get(loc) == null) {
            data.put(loc, new HashMap<>());
        }
        if (oldData.get(loc) == null) {
            oldData.put(loc, new HashMap<>());
        }
    }

    private void addData(WorldLocation loc, String key, Tag value, boolean force) {
        Tag prev = data.get(loc).get(key);
        oldData.get(loc).put(key, prev);
        data.get(loc).put(key, value);
        if (force || !this.match(prev, value)) {
            //DragonAPI.LOGGER.info("Changing '"+key+"' from "+prev+" to "+value+" @ "+loc.getBlockEntity());
            this.addChange(loc, key, value);
        }
    }

    private void addChange(WorldLocation loc, String key, Tag value) {
        HashMap<String, Tag> map = changes.get(loc);
        if (map == null) {
            map = new HashMap();
            changes.put(loc, map);
        }
        map.put(key, value);
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public boolean isEmpty(WorldLocation loc) {
        return changes.get(loc).isEmpty();
    }

    @OnlyIn(Dist.CLIENT)
    public void readForSync(BlockEntity te, CompoundTag NBT) {
        if (dispatch) {
            if (DragonOptions.LOGSYNCCME.getState()) {
                DragonAPI.LOGGER.info("The compound sync packet for "+te+" would have just CME'd, as the");
                DragonAPI.LOGGER.info("Client-Thread data-reading code has overlapped with the Network-Thread byte[] reading.");
                DragonAPI.LOGGER.info("Seeing this message frequently could indicate a serious issue.\n");
            }
            return;
        }

        WorldLocation loc = new WorldLocation(te);

        this.createMaps(loc);

        for (String key : data.get(loc).keySet()) {
            NBT.put(key, data.get(loc).get(key));
        }
    }

    @Override
    public String toString() {
        return changes.isEmpty() ? "[Empty]" : changes.toString();
    }

    private boolean match(Tag old, Tag cur) {
        if (old == cur)
            return true;
        if (old == null || cur == null)
            return false;
        return cur.equals(old);
    }

    @Override
    public void write(FriendlyByteBuf out) {
        dispatch = true;

        out.writeInt(changes.size());
        for (WorldLocation loc : changes.keySet()) {
            out.writeResourceKey(loc.getDimension()); //write resourceKey for 1.19 - future max says thank you to past max
            out.writeInt(loc.pos.getX());
            out.writeShort(loc.pos.getY());
            out.writeInt(loc.pos.getZ());
        }

        CompoundTag toSend = new CompoundTag();

        for (WorldLocation loc : changes.keySet()) {
            HashMap<String, Tag> map = changes.get(loc);
            try {
                CompoundTag local = new CompoundTag();
                this.saveChanges(map, local);
                toSend.put(loc.toSerialString(), local);
            }
            catch (Exception e) {
                toSend.putBoolean(ERROR_TAG, true);
                e.printStackTrace();
                //out.clear();
            }
        }

        try {
            out.writeNbt(toSend);
        }
        catch (Exception e) {
            DragonAPI.LOGGER.error("Error writing Compound Sync Tag!");
            out.clear();
            e.printStackTrace();
        }

        DragonAPI.LOGGER.info("Wrote "+changes.size()+" locations, data="+toSend);

        dispatch = false;
    }

    private void saveChanges(HashMap<String, Tag> changes, CompoundTag loc) {
        for (String key : changes.keySet()) {
            Tag val = changes.get(key);
            loc.put(key, val);
        }
    }
    /*
    private void trim() {
        HashSet<WorldLocation> remove = new HashSet();
        for (WorldLocation loc : changes.keySet()) {
            if (changes.get(loc).isEmpty()) {
                remove.add(loc);
            }
        }
        for (WorldLocation loc : remove)
            changes.remove(loc);
    }
     */

    public void readPacketData(FriendlyByteBuf in) {
        receive = true;
        try {
            int num = in.readInt();
            for (int i = 0; i < num; i++) {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, in.readResourceLocation()); //todo check if it breaks or not
                int x = in.readInt();
                int y = in.readShort();
                int z = in.readInt();
                WorldLocation loc = new WorldLocation(dim, x, y, z);
                this.createMaps(loc);
            }

            CompoundTag received = in.readNbt();
            if (!received.getBoolean(ERROR_TAG)) {
                Collection c = received.getAllKeys();
                for (String name : (Iterable<String>) c) {
                    CompoundTag local = received.getCompound(name);
                    WorldLocation loc = WorldLocation.fromSerialString(name);
                    //try {
                    this.populateFromStream(loc, local);
                    //}
                    //catch (Exception e) {
                    //	e.printStackTrace();
                    //	data.clear(); //discard packet
                    //}
                }
            }
        }
        catch (Exception e) {
            DragonAPI.LOGGER.error("Error reading Compound Sync Tag!");
            e.printStackTrace();
            data.clear();
        }
        receive = false;
    }

    private void populateFromStream(WorldLocation loc, CompoundTag local) {
        Collection c = local.getAllKeys();
        Iterator<String> it = c.iterator();
        while (it.hasNext()) {
            String name = it.next();
            Tag tag = local.get(name);
            data.get(loc).put(name, tag);
        }
        DragonAPI.LOGGER.info("Reading "+data.get(loc)+" from "+local+" @ "+loc.getBlockEntity());
    }

    @Override
    public boolean hasNoData() {
        return data.isEmpty();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(PacketListener inh) { //Ignore default handling
        Level world = Minecraft.getInstance().level;
        for (WorldLocation loc : data.keySet()) {
            if (loc.getDimension() == world.dimension()) {
                if (world.getBlockState(new BlockPos(loc.pos.getX(), loc.pos.getY(), loc.pos.getZ())) != null) {
                    BlockEntity te = loc.getBlockEntity(world);
                    if (te instanceof CompoundSyncPacketHandler) {
                        ((CompoundSyncPacketHandler)te).handleCompoundSyncPacket(this);
                    }
                }
            }
        }
    }

    public interface CompoundSyncPacketHandler {

        void handleCompoundSyncPacket(CompoundSyncPacket packet);

    }

    public static class CompoundSyncPacketTracker implements TickRegistry.TickHandler {

        public static final CompoundSyncPacketTracker instance = new CompoundSyncPacketTracker();

        private int tickcount;
        private static final int MAXTICK = DragonOptions.SLOWSYNC.getState() ? 20 : 4;

        private CompoundSyncPacketTracker() {

        }

        @Override
        public void tick(TickRegistry.TickType type, Object... tickData) {
            tickcount++;
            if (tickcount >= MAXTICK) {
                if (!CompoundSyncPacket.instance.isEmpty())
                    this.dispatchPacket((Level)tickData[0]);
                tickcount = 0;
            }
        }

        private void dispatchPacket(Level world) {
            for (ServerPlayer ep : ((List<ServerPlayer>)world.players()))  {
                ep.connection.send(CompoundSyncPacket.instance);
            }
        }

        @Override
        public EnumSet<TickRegistry.TickType> getType() {
            return EnumSet.of(TickRegistry.TickType.WORLD);
        }

        @Override
        public boolean canFire(TickEvent.Phase p) {
            return p == TickEvent.Phase.END;
        }

        @Override
        public String getLabel() {
            return "Compound Sync Packet";
        }

    }
}
