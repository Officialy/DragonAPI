package reika.dragonapi.auxiliary;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

/** Warning service; GUI and cursor state belong exclusively to the client popup screen. */
@EventBusSubscriber(modid = DragonAPI.MODID)
public final class PopupWriter {
    private static final PopupWriter INSTANCE = new PopupWriter();
    private final ArrayDeque<Message> messages = new ArrayDeque<>();
    private final List<Warning> serverMessages = new ArrayList<>();
    private final Map<MinecraftServer, Map<UUID, Set<Warning>>> alreadySent = new HashMap<>();

    private PopupWriter() {}

    public static PopupWriter instance() { return INSTANCE; }

    public static void open() {
        if (FMLEnvironment.getDist() == Dist.CLIENT) reika.dragonapi.client.PopupClient.open();
    }

    public void addMessage(String text) { addMessage(new Warning(text)); }

    public void addMessage(Warning warning) {
        if (FMLEnvironment.getDist() == Dist.DEDICATED_SERVER || EffectiveSide.get() == LogicalSide.SERVER)
            addServerMessage(warning);
        else enqueue(new Warning(warning.text() + " Hold CTRL to be able to click this message.", warning.width()));
    }

    /** Enqueue an already formatted warning, including restoring a previously obtained snapshot. */
    public synchronized void enqueue(Warning warning) { messages.addLast(new Message(Objects.requireNonNull(warning))); }

    /** Each occurrence has its own identity, even when the same Warning is queued twice. */
    public static final class Message {
        private final Warning warning;
        private Message(Warning warning) { this.warning = warning; }
        public Warning warning() { return warning; }
    }

    public synchronized List<Warning> pendingMessages() { return messages.stream().map(Message::warning).toList(); }
    public synchronized Message currentMessage() { return messages.peekFirst(); }
    public synchronized boolean hasMessages() { return !messages.isEmpty(); }

    /** A click extracted for a removed popup must never dismiss its successor. */
    public synchronized boolean dismiss(Message expected) {
        if (expected == null || messages.peekFirst() != expected) return false;
        messages.removeFirst();
        return true;
    }

    public synchronized boolean dismissFirst() { return dismiss(messages.peekFirst()); }
    public synchronized void clearMessages() { messages.clear(); }
    public synchronized void addServerMessage(Warning warning) { serverMessages.add(Objects.requireNonNull(warning)); }

    public synchronized void sendServerMessages(ServerPlayer player) {
        var sent = alreadySent.computeIfAbsent(player.level().getServer(), key -> new HashMap<>())
                .computeIfAbsent(player.getUUID(), key -> new HashSet<>());
        for (Warning warning : serverMessages) {
            if (sent.contains(warning)) continue;
            ReikaPacketHelper.sendStringIntPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.POPUP.wireId(),
                    new PacketTarget.PlayerTarget(player), warning.text(), warning.width());
            sent.add(warning);
        }
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) INSTANCE.sendServerMessages(player);
    }

    @SubscribeEvent public static void stop(ServerStoppedEvent event) {
        synchronized (INSTANCE) { INSTANCE.alreadySent.remove(event.getServer()); }
    }

    public record Warning(String text, int width) {

            public Warning(String s) {
                this(s, Math.max(calcMinSizeForText(s), 192));
            }

            public Warning(String text, int width) {
                this.text = text;
                this.width = Math.clamp(width, 96, 300);
            }

            private static int calcMinSizeForText(String s) { //at w=192, 74 chars becomes 4 lines, or about 18 chars a line (1 char = 11px); ideally keep line count <= 6
                int w = 192;
                int c = 18;
                int lines = s.length() / c;
                while (lines > 6) {
                    w += 16;
                    c += 2;
                    lines = s.length() / c;
                }
                return w;
            }

            @Override
            public int hashCode() {
                return text.hashCode();
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof Warning && text.equals(((Warning) o).text);
            }

        }

}
