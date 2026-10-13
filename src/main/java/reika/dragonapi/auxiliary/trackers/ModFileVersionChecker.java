package reika.dragonapi.auxiliary.trackers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.instantiable.event.client.ClientLoginEvent;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = DragonAPI.MODID)
public class ModFileVersionChecker {

    public static final ModFileVersionChecker instance = new ModFileVersionChecker();

    private static final ConcurrentHashMap<String, String> data = new ConcurrentHashMap<>();

    private ModFileVersionChecker() {
    }

    public void addMod(DragonAPIMod mod) {
        data.put(mod.getModContainer().getModId(), mod.getFileHash());
    }

    @SubscribeEvent
    public static void dispatch(ClientLoginEvent evt) {
        if (!reika.dragonapi.DragonOptions.FILEHASH.getState()) return;
        for (String mod : data.keySet()) {
            String s = mod+":"+data.get(mod);
            ReikaPacketHelper.sendStringIntPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.FILEMATCH.wireId(), PacketTarget.server, s);
        }
    }

	public void checkFiles(ServerPlayer ep, String s) {
		String[] parts = s != null ? s.split(":", 2) : new String[0];
        String mod = parts.length > 0 ? parts[0] : "<unknown>";
        String client = parts.length == 2 ? parts[1] : "<malformed>";
        String hash = data.get(mod);
        if (!reika.dragonapi.DragonOptions.FILEHASH.getState()) {
            report(ep, mod, client, hash, HashDecision.DISABLED); return;
        }
		if (parts.length != 2) {
			this.kick(ep, mod, client, null);
			return;
		}
        if (hash != null && !hash.equals(client)) {
            this.kick(ep, mod, client, hash);
        }
        else {
            DragonAPI.LOGGER.info("Player "+ep.getName()+" passed hash check for "+mod+". Hash: "+hash);
            report(ep,mod,client,hash,hash == null ? HashDecision.CLIENT_ONLY : HashDecision.MATCH);
        }
    }

    private void kick(ServerPlayer ep, String mod, String client, String server) {
        HashKickEvent evt = new HashKickEvent(ep, mod, client, server);
        if (!NeoForge.EVENT_BUS.post(evt).isCanceled()) {
            String msg = mod+" jarfile mismatch. Client Hash: "+client+"; Expected (Server) Hash: "+server;
            ReikaPlayerAPI.kickPlayer(ep, msg);
            DragonAPI.LOGGER.info("Player "+ep.getName()+" kicked due to "+msg);
            report(ep,mod,client,server,HashDecision.KICKED);
        }
        else {
            DragonAPI.LOGGER.info("Player "+ep.getName()+" not kicked for "+mod+" hash mismatch; kick cancelled");
            report(ep,mod,client,server,HashDecision.CANCELLED);
        }
    }

    private static void report(ServerPlayer player,String mod,String client,String server,HashDecision decision) {
        NeoForge.EVENT_BUS.post(new HashCheckEvent(player,mod,client,server,decision));
    }
    public enum HashDecision { MATCH, CLIENT_ONLY, DISABLED, KICKED, CANCELLED }
    /** Read-only observation after enforcement; cancellation stays on the original HashKickEvent. */
    public static final class HashCheckEvent extends PlayerEvent {
        public final String mod, clientHash, serverHash;
        public final HashDecision decision;
        public HashCheckEvent(ServerPlayer player,String mod,String client,String server,HashDecision decision) {
            super(player); this.mod=mod; clientHash=client; serverHash=server; this.decision=decision;
        }
    }

    public static class HashKickEvent extends PlayerEvent implements ICancellableEvent {

        public final String serverHash;
        public final String clientHash;
        public final String mod;

        public HashKickEvent(Player player, String mod, String client, String server) {
            super(player);
            this.mod = mod;
            serverHash = server;
            clientHash = client;
        }

    }

}

