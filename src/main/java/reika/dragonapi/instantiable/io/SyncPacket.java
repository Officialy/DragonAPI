package reika.dragonapi.instantiable.io;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.interfaces.DataSync;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public final class SyncPacket extends ClientboundBlockEntityDataPacket implements DataSync {

    private final HashMap<String, Tag> data = new HashMap<>();
    private final HashMap<String, Tag> oldData = new HashMap<>();
    private final HashMap<String, Tag> changes = new HashMap<>();

    /** Is the packet currently being written by the network thread */
    private boolean dispatch;
    /** Is the packet currently being read by the network thread */
    private boolean receive;

    private static final String ERROR_TAG = "erroredPacket";

    public SyncPacket(BlockEntity te) {
        super(new FriendlyByteBuf(Unpooled.buffer())
                .writeBlockPos(te.getBlockPos())
                .writeVarInt(BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(te.getType()))
                .writeNbt(null /*todo te.serializeNBT()*/));
    }

    public void setData(BlockEntity te, boolean force, CompoundTag NBT) {
        if (dispatch) {
            if (DragonOptions.LOGSYNCCME.getState()) {
                DragonAPI.LOGGER.info("The sync packet for "+te+" would have just CME'd, as the");
                DragonAPI.LOGGER.info("Server-Thread data-writing code has overlapped with the Network-Thread byte[] dispatch.");
                DragonAPI.LOGGER.info("Seeing this message frequently could indicate a serious issue.\n");
            }
            return;
        }

//     todo   this.getPos().getX() = te.getBlockPos().getX();
//        this.getPos().getY() = te.getBlockPos().getY();
//        this.getPos().getZ() = te.getBlockPos().getZ();

        changes.clear();
        Collection<String> c = NBT.getAllKeys();
        Iterator<String> it = c.iterator();
        HashSet<String> unused = new HashSet<>(data.keySet());
        while (it.hasNext()) {
            String name = it.next();
            if (name == null) {
                DragonAPI.LOGGER.error("An NBT tag with a null key is being sent to the sync packet from "+te);
            }
            else {
                unused.remove(name);
                Tag tag = NBT.get(name);
                this.addData(name, tag, force);
            }
        }
        for (String s : unused) {
            this.addData(s, null, force);
        }
    }

    private void addData(String key, Tag value, boolean force) {
        Tag prev = data.get(key);
        oldData.put(key, prev);
        data.put(key, value);
        if (force || !this.match(prev, value)) {
            changes.put(key, value);
        }
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

//    @Override
    public void readPacketData(FriendlyByteBuf in) {
        receive = true;
        try {
//todo            this.getPos().getX() = in.readInt();
//            this.getPos().getY() = in.readShort();
//            this.getPos().getZ() = in.readInt();
//
            CompoundTag received = in.readNbt();
            if (!received.getBoolean(ERROR_TAG)) {
                //try {
                this.populateFromStream(received);
                //}
                //catch (Exception e) {
                //	e.printStackTrace();
                //	data.clear(); //discard packet
                //}
            }
        }
        catch (Exception e) {
            DragonAPI.LOGGER.error("Error reading Sync Tag!");
            e.printStackTrace();
            data.clear();
        }
        receive = false;
    }

    private void populateFromStream(CompoundTag received) {
        Collection<String> c = received.getAllKeys();
        for (String name : c) {
            Tag tag = received.get(name);
            data.put(name, tag);
        }
    }

//    @SideOnly(Dist.CLIENT)
    public void readForSync(BlockEntity te, CompoundTag NBT) {
        if (dispatch) {
            if (DragonOptions.LOGSYNCCME.getState()) {
                DragonAPI.LOGGER.info("The sync packet for "+te+" would have just CME'd, as the");
                DragonAPI.LOGGER.info("Client-Thread data-reading code has overlapped with the Network-Thread byte[] reading.");
                DragonAPI.LOGGER.info("Seeing this message frequently could indicate a serious issue.\n");
            }
            return;
        }

        for (String key : data.keySet()) {
            Tag base = data.get(key);
            if (base == null)
                NBT.remove(key);
            else
                NBT.put(key, base);
        }
    }

    @Override
    public void write(FriendlyByteBuf out) {
        super.write(out);
        dispatch = true;
        out.writeInt(this.getPos().getX());
        out.writeShort(this.getPos().getY());
        out.writeInt(this.getPos().getZ());

        CompoundTag toSend = new CompoundTag();
        try {
            this.saveChanges(toSend);
        }
        catch (Exception e) {
            toSend.putBoolean(ERROR_TAG, true);
            e.printStackTrace();
            //out.clear();
        }
        try {
            out.writeNbt(toSend);
        }
        catch (Exception e) {
            DragonAPI.LOGGER.error("Error writing Sync Tag!");
            out.clear();
            e.printStackTrace();
        }
        dispatch = false;
    }

    private void saveChanges(CompoundTag toSend) {
        for (String key : changes.keySet()) {
            Tag val = changes.get(key);
            if (val == null)
                toSend.remove(key);
            else
                toSend.put(key, val);
        }
    }

    private boolean match(Tag old, Tag cur) {
        if (old == cur)
            return true;
        if (old == null || cur == null)
            return false;
        return cur.equals(old);
    }

    @Override
    public String toString() {
        return changes.isEmpty() ? "[Empty]" : changes.toString();
    }

    public boolean hasNoData() {
        return data.isEmpty();
    }
}
