package reika.dragonapi.libraries.io;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.Unpooled;
import java.io.DataInputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.junit.jupiter.api.Test;
import reika.dragonapi.auxiliary.PacketTypes;
import reika.dragonapi.interfaces.PacketHandler;
import reika.dragonapi.libraries.io.CustomNetworkBridge.DataPacketPayload;
import reika.dragonapi.libraries.io.ReikaPacketHelper.DataPacket;
import reika.dragonapi.libraries.io.ReikaPacketHelper.PacketObj;

class PacketPipelineTest {
    private final PacketHandler handler = (packet, world, player) -> {};
    private final PacketPipeline pipe = new PacketPipeline("pipeline_test", "channel", handler);
    private final Identifier customId = Identifier.fromNamespaceAndPath("pipeline_test", "custom");

    private DataPacket packet(PacketTypes type) {
        DataPacket packet = new DataPacket(new byte[]{12, 34, 56, 78});
        packet.init(type, pipe);
        return packet;
    }

    private static IPayloadContext context(PacketFlow incoming, List<CustomPacketPayload> replies) {
        return (IPayloadContext)Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(), new Class[]{IPayloadContext.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "flow" -> incoming;
                    case "reply" -> { replies.add((CustomPacketPayload)arguments[0]); yield null; }
                    default -> throw new AssertionError("Unexpected context access: " + method.getName());
                });
    }

    @Test void nativePacketsPreserveExistingBridgeBytes() {
        DataPacket packet = packet(PacketTypes.RAW);
        var client = (ClientboundCustomPayloadPacket)pipe.getMinecraftPacket(packet, PacketFlow.CLIENTBOUND);
        var server = (ServerboundCustomPayloadPacket)pipe.getMinecraftPacket(packet, PacketFlow.SERVERBOUND);
        var payload = (DataPacketPayload)client.payload();
        assertEquals(Identifier.fromNamespaceAndPath("pipeline_test", "channel"), payload.type().id());
        assertArrayEquals(new byte[]{(byte)PacketTypes.RAW.ordinal(), 4, 12, 34, 56, 78}, payload.data());
        assertArrayEquals(payload.data(), ((DataPacketPayload)server.payload()).data());
        assertArrayEquals(packet.getBytes(), ((DataPacket)pipe.bridge().decode(payload)).getBytes());
    }

    @Test void registeredCodecsActuallyEncodeDecodeAndBindTheirHandler() {
        AtomicInteger encoded = new AtomicInteger(), decoded = new AtomicInteger();
        pipe.registerPacket(customId, CustomPacket.class, (packet, buf) -> { encoded.incrementAndGet(); buf.writeInt(packet.value); },
                buf -> { decoded.incrementAndGet(); return new CustomPacket(buf.readInt()); });
        CustomPacket packet = new CustomPacket(1234567); packet.init(PacketTypes.RAW, pipe);
        var payload = (DataPacketPayload)((ClientboundCustomPayloadPacket)pipe.getMinecraftPacket(packet, PacketFlow.CLIENTBOUND)).payload();
        assertEquals(customId, payload.type().id());
        FriendlyByteBuf wire = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var codec = pipe.bridge().codec(customId);
            codec.encode(wire, payload);
            var detached = codec.decode(wire);
            assertFalse(wire.isReadable());
            CustomPacket received = (CustomPacket)pipe.bridge().decode(detached);
            assertEquals(1234567, received.value);
            assertSame(handler, received.getHandler());
            assertEquals(1, encoded.get()); assertEquals(1, decoded.get());
        } finally { wire.release(); }
    }

    @Test void invalidRegistrationsAndForeignChannelPacketsFailClearly() {
        pipe.registerPacket(customId, CustomPacket.class, (packet, buf) -> buf.writeInt(packet.value), buf -> new CustomPacket(buf.readInt()));
        assertThrows(IllegalArgumentException.class, () -> pipe.registerPacket(customId, CustomPacket.class, (p,b) -> {}, b -> new CustomPacket(0)));
        assertThrows(IllegalArgumentException.class, () -> pipe.registerPacket(Identifier.fromNamespaceAndPath("foreign", "custom"), OtherPacket.class, (p,b) -> {}, b -> new OtherPacket(0)));
        assertThrows(IllegalArgumentException.class, () -> pipe.registerPacket(Identifier.fromNamespaceAndPath("pipeline_test", "channel"), OtherPacket.class, (p,b) -> {}, b -> new OtherPacket(0)));
        pipe.registerPacket(OtherPacket.class, (p,b) -> b.writeInt(p.value), b -> new OtherPacket(b.readInt()));
        OtherPacket otherPacket = new OtherPacket(77); otherPacket.init(PacketTypes.RAW, pipe);
        var derived = (DataPacketPayload)((ClientboundCustomPayloadPacket)pipe.getMinecraftPacket(otherPacket, PacketFlow.CLIENTBOUND)).payload();
        assertTrue(derived.type().id().getPath().startsWith("channel/") && derived.type().id().getPath().endsWith("/otherpacket"));
        assertEquals(77, ((OtherPacket)pipe.bridge().decode(derived)).value);
        var other = new PacketPipeline("pipeline_test", "other", (p,w,e) -> {});
        assertThrows(IllegalArgumentException.class, () -> other.getMinecraftPacket(packet(PacketTypes.RAW), PacketFlow.CLIENTBOUND));
        assertThrows(IllegalArgumentException.class, () -> pipe.bridge().toPayload("foreign", packet(PacketTypes.RAW)));
    }

    @Test void decodedPacketsRejectUnknownIdentitiesAndTrailingData() {
        var foreign = new DataPacketPayload(new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("foreign", "channel")), new byte[]{0});
        assertThrows(IllegalArgumentException.class, () -> pipe.bridge().decode(foreign));
        pipe.registerPacket(customId, CustomPacket.class, (p,b) -> b.writeInt(p.value), b -> new CustomPacket(b.readInt()));
        var trailing = new DataPacketPayload(new CustomPacketPayload.Type<>(customId), new byte[]{0, 0, 0, 1, 2});
        assertThrows(IllegalArgumentException.class, () -> pipe.bridge().decode(trailing));
        var wrongType = new DataPacketPayload(new CustomPacketPayload.Type<>(customId), new byte[]{0});
        FriendlyByteBuf wire = new FriendlyByteBuf(Unpooled.buffer());
        try { assertThrows(IllegalArgumentException.class, () -> pipe.bridge().codec(Identifier.fromNamespaceAndPath("pipeline_test", "channel")).encode(wire, wrongType)); }
        finally { wire.release(); }
    }

    @Test void repliesAreConnectionScopedAndNestedDispatchRestoresTheOuterConnection() {
        List<CustomPacketPayload> outer = new ArrayList<>(), inner = new ArrayList<>();
        var first = context(PacketFlow.SERVERBOUND, outer); var second = context(PacketFlow.SERVERBOUND, inner);
        DataPacket packet = packet(PacketTypes.RAW);
        assertThrows(IllegalStateException.class, () -> pipe.replyToPacket(packet));
        PacketPipeline.handleWithReplyContext(Identifier.fromNamespaceAndPath("pipeline_test", "channel"), first, () -> {
            pipe.replyToPacket(packet);
            var foreignNamespace = new PacketPipeline("foreign", "channel", handler);
            assertThrows(IllegalStateException.class, () -> foreignNamespace.replyToPacket(packet));
            PacketPipeline.handleWithReplyContext(Identifier.fromNamespaceAndPath("pipeline_test", "channel"), second, () -> pipe.replyToPacket(packet));
            pipe.replyToPacket(packet);
            assertInstanceOf(ClientboundCustomPayloadPacket.class, pipe.getMinecraftPacket(packet));
        });
        assertEquals(2, outer.size()); assertEquals(1, inner.size());
        assertThrows(IllegalStateException.class, () -> pipe.replyToPacket(packet));
    }

    @Test void clientRepliesUseTheOppositeFlowAndExplicitContextsSupportDeferredReplies() {
        List<CustomPacketPayload> replies = new ArrayList<>();
        var context = context(PacketFlow.CLIENTBOUND, replies);
        DataPacket packet = packet(PacketTypes.RAW);
        PacketPipeline.handleWithReplyContext(Identifier.fromNamespaceAndPath("pipeline_test", "channel"), context, () -> {
            assertInstanceOf(ServerboundCustomPayloadPacket.class, pipe.getMinecraftPacket(packet));
            pipe.replyToPacket(packet);
        });
        pipe.replyToPacket(packet, context);
        assertEquals(2, replies.size());
        assertThrows(IllegalArgumentException.class, () -> pipe.replyToPacket(packet(PacketTypes.SOUND), context));
    }

    @Test void handlerFailuresAndWrongChannelRepliesCannotLeakAmbientContext() {
        List<CustomPacketPayload> replies = new ArrayList<>();
        var context = context(PacketFlow.SERVERBOUND, replies);
        DataPacket packet = packet(PacketTypes.RAW);
        assertThrows(IllegalStateException.class, () -> PacketPipeline.handleWithReplyContext(Identifier.fromNamespaceAndPath("pipeline_test", "channel"), context, () -> { throw new IllegalStateException("failure"); }));
        assertThrows(IllegalStateException.class, () -> pipe.replyToPacket(packet));
        PacketPipeline.handleWithReplyContext(Identifier.fromNamespaceAndPath("pipeline_test", "other"), context, () -> assertThrows(IllegalStateException.class, () -> pipe.replyToPacket(packet)));
        assertTrue(replies.isEmpty());
    }

    @Test void serverboundConstructionRejectsClientOnlyPackets() {
        for (PacketTypes type : PacketTypes.values()) {
            if (type.isClientboundOnly()) assertThrows(IllegalArgumentException.class, () -> pipe.getMinecraftPacket(packet(type), PacketFlow.SERVERBOUND));
        }
        assertInstanceOf(ClientboundCustomPayloadPacket.class, pipe.getMinecraftPacket(packet(PacketTypes.SOUND), PacketFlow.CLIENTBOUND));
    }

    private static class CustomPacket extends PacketObj {
        final int value;
        CustomPacket(int value) { this.value = value; type = PacketTypes.RAW; }
        @Override public void encode(FriendlyByteBuf buf) { throw new AssertionError("Registered encoder must be used"); }
        @Override public DataInputStream getDataIn() { return new DataInputStream(new java.io.ByteArrayInputStream(new byte[0])); }
        @Override protected String getDataAsString() { return Integer.toString(value); }
    }
    private static final class OtherPacket extends CustomPacket { OtherPacket(int value) { super(value); } }
}
