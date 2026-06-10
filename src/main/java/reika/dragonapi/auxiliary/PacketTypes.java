package reika.dragonapi.auxiliary;

public enum PacketTypes {

    DATA(),
    SOUND(),
    STRING(),
    UPDATE(),
    FLOAT(),
    SYNC(),       // legacy per-field reflection sync (see ReikaPacketHelper#sendSyncPacket); the
                  // SYNC case in APIPacketHandler reads readUTF + 3×int + PacketableData; all known
                  // callers are commented out but the type is preserved for backwards compatibility
    TANK(),
    RAW(),
    NBT(),
    STRINGINT(),
    STRINGINTLOC(),
    UUID(),
    PREFIXED(),
    POS(),
    FULLSOUND(),
    // 26.1: dedicated type for {@link reika.dragonapi.instantiable.io.SyncPacket} (BlockEntityBase's
    // periodic full-BE-NBT sync, post-tick-20). Wire format: BlockPos (long) + varInt typeId + NBT.
    // Previously SyncPacket co-opted {@link #SYNC}, but the SYNC handler reads UTF-name + 3×int,
    // which silently corrupted every periodic sync — client tanks never updated, fluid never
    // rendered. New handler in APIPacketHandler decodes via FriendlyByteBuf and applies the
    // tag via {@code BlockEntityBase#applySyncTag(CompoundTag)}.
    BE_NBT_SYNC();

    public static PacketTypes getPacketType(int type) {
        return PacketTypes.values()[type];
    }

    public boolean hasCoordinates() {
        return this != RAW && this != NBT && this != STRINGINT && this != PREFIXED && this != POS && this != FULLSOUND && this != BE_NBT_SYNC;
    }

}
