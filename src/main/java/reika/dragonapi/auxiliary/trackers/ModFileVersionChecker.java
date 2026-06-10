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

import java.util.HashMap;

@EventBusSubscriber(modid = DragonAPI.MODID)
public class ModFileVersionChecker {

    public static final ModFileVersionChecker instance = new ModFileVersionChecker();

    private static final HashMap<String, String> data = new HashMap<>();

    private ModFileVersionChecker() {
    }

    public void addMod(DragonAPIMod mod) {
        data.put(mod.getModContainer().getModId(), mod.getFileHash());
    }

    @SubscribeEvent
    public static void dispatch(ClientLoginEvent evt) {
        for (String mod : data.keySet()) {
            String s = mod+":"+data.get(mod);
            ReikaPacketHelper.sendStringIntPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.FILEMATCH.ordinal(), PacketTarget.server, s);
        }
    }

    public void checkFiles(ServerPlayer ep, String s) {
        boolean flag = false;
        String[] parts = s.split(":");
        if (parts.length != 2)
            flag = true;
        String mod = parts[0];
        String hash = data.get(mod);
        if (!flag) {
            if (hash != null) { //Client-only mods will be ignored
                flag = !hash.equals(parts[1]);
            }
        }
        if (flag) {
            this.kick(ep, mod, parts[1], hash);
        }
        else {
            DragonAPI.LOGGER.info("Player "+ep.getName()+" passed hash check for "+mod+". Hash: "+hash);
        }
    }

    private void kick(ServerPlayer ep, String mod, String client, String server) {
        HashKickEvent evt = new HashKickEvent(ep, mod, client, server);
        if (!NeoForge.EVENT_BUS.post(evt).isCanceled()) {
            String msg = mod+" jarfile mismatch. Client Hash: "+client+"; Expected (Server) Hash: "+server;
            ReikaPlayerAPI.kickPlayer(ep, msg);
            DragonAPI.LOGGER.info("Player "+ep.getName()+" kicked due to "+msg);
        }
        else {
            DragonAPI.LOGGER.info("Player "+ep.getName()+" not kicked for "+mod+" hash mismatch; kick cancelled");
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


