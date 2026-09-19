package reika.dragonapi.mixin;

import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import reika.dragonapi.test.GameTestRuntimeMonitor;

@Mixin(PlayerList.class)
public abstract class MixinPlayerList {

    @Shadow
    @Final
    private MinecraftServer server;

    @Invoker("save")
    protected abstract void dragonapi$invokeSave(ServerPlayer player);

    /** Test players have random UUIDs and no durable identity; saving each one leaks files to run/. */
    @Redirect(
            method = "remove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;save(Lnet/minecraft/server/level/ServerPlayer;)V"
            )
    )
    private void dragonapi$skipSyntheticPlayerSave(PlayerList owner, ServerPlayer player) {
        if (!(this.server instanceof GameTestServer) || !GameTestRuntimeMonitor.isSyntheticPlayer(player))
            this.dragonapi$invokeSave(player);
    }
}
