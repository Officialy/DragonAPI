package reika.dragonapi.auxiliary;

public enum PacketTypes implements reika.dragonapi.interfaces.PacketId {

    DATA(0),
    SOUND(1),
    STRING(2),
    UPDATE(3),
    FLOAT(4),
    SYNC(5),       // legacy per-field reflection sync (see ReikaPacketHelper#sendSyncPacket); the
                  // SYNC case in APIPacketHandler reads readUTF + 3×int + PacketableData; all known
                  // callers are commented out but the type is preserved for backwards compatibility
    TANK(6),
    RAW(7),
    NBT(8),
    STRINGINT(9),
    STRINGINTLOC(10),
    UUID(11),
    PREFIXED(12),
    POS(13),
    FULLSOUND(14),
    // 26.3: dedicated type for {@link reika.dragonapi.instantiable.io.SyncPacket} (BlockEntityBase's
    // periodic full-BE-NBT sync, post-tick-20). Wire format: BlockPos (long) + varInt typeId + NBT.
    // Previously SyncPacket co-opted {@link #SYNC}, but the SYNC handler reads UTF-name + 3×int,
    // which silently corrupted every periodic sync — client tanks never updated, fluid never
    // rendered. New handler in APIPacketHandler decodes via FriendlyByteBuf and applies the
    // tag via {@code BlockEntityBase#applySyncTag(CompoundTag)}.
    BE_NBT_SYNC(15);

    private final int wireId;
    private static final java.util.Map<Integer, PacketTypes> BY_ID = reika.dragonapi.interfaces.PacketId.index(values());
    PacketTypes(int id) {
        if (id < 0 || id > 255) throw new IllegalArgumentException("Packet type ID must fit one byte");
        wireId = id;
    }
    @Override public int wireId() { return wireId; }

    public static PacketTypes getPacketType(int type) {
        return reika.dragonapi.interfaces.PacketId.lookup(BY_ID, type);
    }

    public boolean isClientboundOnly() {
        return this == SYNC || this == TANK || this == BE_NBT_SYNC || this == SOUND || this == FULLSOUND;
    }

    public boolean hasCoordinates() {
        return this != RAW && this != NBT && this != STRINGINT && this != PREFIXED && this != POS && this != FULLSOUND && this != BE_NBT_SYNC;
    }

}
