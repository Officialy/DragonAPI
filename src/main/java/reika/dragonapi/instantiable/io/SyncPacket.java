package reika.dragonapi.instantiable.io;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
// Legacy import removed; now bridged via payloads
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.auxiliary.PacketTypes;
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
        bindToDragonAPIPipeline();
    }

    // Constructor for decoding
    public SyncPacket() {
        bindToDragonAPIPipeline();
    }

    /**
     * 1.21.5: every {@link ReikaPacketHelper.PacketObj} writes its handler id (a short) into
     * the outgoing payload via {@code data.writeShort(getHandlerID(this.handler))}. If
     * {@code handler} is still {@code null} at that point, {@link ReikaPacketHelper#getHandlerID}
     * returns the sentinel {@code -1}, which the receiver can't look up in its handlers map
     * and logs as "Drop unknown-handler packet id=-1" (a spammy DEBUG line per BE per tick).
     *
     * <p>SyncPackets are constructed directly by {@code BlockEntityBase} rather than going
     * through one of the {@code ReikaPacketHelper.send*Packet} helpers that would call
     * {@code init(PacketTypes.X, pipe)}, so we bind here. The channel is DragonAPI's; the
     * pipeline / handler are registered during mod init.
     */
    private void bindToDragonAPIPipeline() {
        var pipe = ReikaPacketHelper.getPipeline(DragonAPI.packetChannel);
        if (pipe != null) {
            // 26.1 fix: use the dedicated BE-NBT-sync type, not the legacy SYNC type which is for
            // per-field reflection sync ({@link reika.dragonapi.libraries.io.ReikaPacketHelper#sendSyncPacket})
            // and has a totally different wire format (UTF-name + 3×int vs our BlockPos + typeId + NBT).
            // Previously this co-opted SYNC and the handler silently misread our packets, so every
            // periodic BlockEntityBase sync after tick 20 was a no-op on the client.
            this.init(PacketTypes.BE_NBT_SYNC, pipe);
        }
        // If pipe is null we're being constructed before DragonAPI's mod-init handler-register
        // fired (e.g. very early static init). PacketObj#init logs the configuration bug itself
        // if it then tries to encode with a missing handler, so we don't double-log here.
    }

    public void setData(BlockEntity te, boolean force, CompoundTag NBT) {
        if (dispatch) {
            if (DragonOptions.LOGSYNCCME.getState()) {
                DragonAPI.LOGGER.info("Potential CME detected while setting data.");
            }
            return;
        }

        changes.clear();
        HashSet<String> unused = new HashSet<>(data.keySet());
        for (String name : NBT.keySet()) { // 1.21: method exists
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
        for (String name : received.keySet()) {
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

    @Override
    public void encode(FriendlyByteBuf buf) {
        // 1.21.5 fix: must write the {@link ReikaPacketHelper.PacketObj} header (short handler-id
        // + byte packet-type) AND the body inside a varInt-length frame, matching the
        // {@code DataPacket} wire format the receiver's {@link ReikaPacketHelper.DataPacket#decode}
        // expects. Without that, decode would interpret the first 2 bytes of {@link #pos}'s
        // packed long as the handler id (= 0xFFFF for negative coords) and fall into the
        // unknown-handler drop path, flooding the log on every per-tick BE sync. Body
        // payload-routing is still TODO (APIPacketHandler's SYNC case has a different on-the-wire
        // shape), but at minimum the bytes are now well-formed.
        super.encode(buf);
        dispatch = true;
        ByteBuf body = Unpooled.buffer();
        FriendlyByteBuf bodyBuf = new FriendlyByteBuf(body);
        try {
            bodyBuf.writeBlockPos(pos);
            bodyBuf.writeVarInt(blockEntityTypeId);
            CompoundTag toSend = new CompoundTag();
            try {
                saveChanges(toSend);
            } catch (Exception e) {
                toSend.putBoolean(ERROR_TAG, true);
                e.printStackTrace();
            }
            bodyBuf.writeNbt(toSend);
            int len = body.readableBytes();
            buf.writeVarInt(len);
            buf.writeBytes(body);
        } finally {
            body.release();
            dispatch = false;
        }
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
        if (received != null && !received.getBooleanOr(ERROR_TAG, false)) {
            pkt.populateFromStream(received);
        }
        return pkt;
    }

    // Handler migrated to payload version

    private void saveChanges(CompoundTag toSend) {
        for (String key : changes.keySet()) {
            Tag val = changes.get(key);
            if (val == null)
                toSend.remove(key);
            else
                toSend.put(key, val);
        }
    }

    // Accessors for payload bridge
    public BlockPos getPos() {
        return pos;
    }

    public int getBlockEntityTypeId() {
        return blockEntityTypeId;
    }

    public CompoundTag getChangesForSend() {
        CompoundTag tag = new CompoundTag();
        saveChanges(tag);
        return tag;
    }

    private boolean match(Tag old, Tag cur) {
        if (old == cur)
            return true;
        if (old == null || cur == null)
            return false;
        return cur.equals(old);
    }
}

