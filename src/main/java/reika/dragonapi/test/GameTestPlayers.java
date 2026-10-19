package reika.dragonapi.test;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Joined test players with NeoForge's supported mock negotiation, including real login events. */
public final class GameTestPlayers {
    private GameTestPlayers() {}
    /** Preserve vanilla's creative-returning mock while joining through a fully compatible mock connection. */
    public static ServerPlayer joinedCreative(GameTestHelper helper) {
        var cookie=cookie();
        var player=new ServerPlayer(helper.getLevel().getServer(),helper.getLevel(),cookie.gameProfile(),cookie.clientInformation()) {
            @Override public GameType gameMode() { return GameType.CREATIVE; }
        };
        return join(helper,player,cookie);
    }
    /** A changeable real game mode; subclassing retains NeoForge's test-player config-sync handling. */
    public static ServerPlayer joined(GameTestHelper helper,GameType mode) {
        Objects.requireNonNull(mode,"mode");
        var cookie=cookie();
        var player=new ServerPlayer(helper.getLevel().getServer(),helper.getLevel(),cookie.gameProfile(),cookie.clientInformation()) {};
        join(helper,player,cookie);
        player.setGameMode(mode);
        return player;
    }
    private static CommonListenerCookie cookie() {
        return CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),"test-mock-player"),false);
    }
    private static ServerPlayer join(GameTestHelper helper,ServerPlayer player,CommonListenerCookie cookie) {
        var connection=new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
        return player;
    }
}
