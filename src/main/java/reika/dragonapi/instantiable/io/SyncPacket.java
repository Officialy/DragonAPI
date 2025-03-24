package reika.dragonapi.instantiable.io;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.interfaces.DataSync;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Supplier;

public final class SyncPacket extends ReikaPacketHelper.PacketObj implements DataSync {

    private final HashMap<String, Tag> data = new HashMap<>();
    private final HashMap<String, Tag> oldData = new HashMap<>();
    private final HashMap<String, Tag> changes = new HashMap<>();

    private boolean dispatch;
    private boolean receive;
    private static final String ERROR_TAG = "erroredPacket";

    private BlockPos pos;
    private int blockEntityTypeId;

    // Constructor for sending
    public SyncPacket(BlockEntity te) {
        this.pos = te.getBlockPos();
        this.blockEntityTypeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(te.getType());
    }

    // Constructor for decoding
    public SyncPacket() {}

    public void setData(BlockEntity te, boolean force, CompoundTag NBT) {
        if (dispatch) {
            if (DragonOptions.LOGSYNCCME.getState()) {
                DragonAPI.LOGGER.info("Potential CME detected while setting data.");
            }
            return;
        }

        changes.clear();
        HashSet<String> unused = new HashSet<>(data.keySet());
        for (String name : NBT.getAllKeys()) {
            if (name == null) {
                DragonAPI.LOGGER.error("Null key in SyncPacket data from " + te);
            } else {
                unused.remove(name);
                Tag tag = NBT.get(name);
                addData(name, tag, force);
            }
        }
        for (String s : unused) {
            addData(s, null, force);
        }
    }

    @Override
    public boolean hasNoData() {
        return data.isEmpty();
    }

    private void addData(String key, Tag value, boolean force) {
        Tag prev = data.get(key);
        oldData.put(key, prev);
        data.put(key, value);
        if (force || !match(prev, value)) {
            changes.put(key, value);
        }
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    private void populateFromStream(CompoundTag received) {
        for (String name : received.getAllKeys()) {
            Tag tag = received.get(name);
            data.put(name, tag);
        }
    }

    public void readForSync(BlockEntity te, CompoundTag NBT) {
        if (dispatch) {
            if (DragonOptions.LOGSYNCCME.getState()) {
                DragonAPI.LOGGER.info("Potential CME detected while reading data.");
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

    public void encode(FriendlyByteBuf buf) {
        dispatch = true;
        buf.writeBlockPos(pos);
        buf.writeVarInt(blockEntityTypeId);
        CompoundTag toSend = new CompoundTag();
        try {
            saveChanges(toSend);
        } catch (Exception e) {
            toSend.putBoolean(ERROR_TAG, true);
            e.printStackTrace();
        }
        buf.writeNbt(toSend);
        dispatch = false;
    }

    @Override
    public DataInputStream getDataIn() {
        return new DataInputStream(new ByteArrayInputStream(new byte[0])); // legacy, unused now
    }

    @Override
    protected String getDataAsString() {
        return data.toString();
    }

    public static SyncPacket decode(FriendlyByteBuf buf) {
        SyncPacket pkt = new SyncPacket();
        pkt.pos = buf.readBlockPos();
        pkt.blockEntityTypeId = buf.readVarInt();
        CompoundTag received = buf.readNbt();
        if (received != null && !received.getBoolean(ERROR_TAG)) {
            pkt.populateFromStream(received);
        }
        return pkt;
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            BlockEntity te = Minecraft.getInstance().level.getBlockEntity(pos);
            if (te != null) {
                CompoundTag nbt = new CompoundTag();
                readForSync(te, nbt);
                te.load(nbt);
            }
        });
        ctx.get().setPacketHandled(true);
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
}

