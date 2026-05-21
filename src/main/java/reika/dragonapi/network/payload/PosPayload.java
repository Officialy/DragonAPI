package reika.dragonapi.network.payload;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public record PosPayload() implements CustomPacketPayload {
    public static void register(PayloadRegistrar registrar, String modId) {}
    public static void registerClient(RegisterClientPayloadHandlersEvent event) {}
    @Override public Type<? extends CustomPacketPayload> type() { return RawBytesPayload.TYPE; }
}



