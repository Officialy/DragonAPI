package reika.dragonapi.network.payload;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterClientPayloadHandlersEvent;

public record StringPayload() implements CustomPacketPayload {
    public static void register(RegisterPayloadHandlersEvent.PayloadRegistrar registrar, String modId) {}
    public static void registerClient(RegisterClientPayloadHandlersEvent event) {}
    @Override public Type<? extends CustomPacketPayload> type() { return RawBytesPayload.TYPE; }
}



