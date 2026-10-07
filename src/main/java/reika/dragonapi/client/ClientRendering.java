package reika.dragonapi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import reika.dragonapi.auxiliary.trackers.PlayerSpecificRenderer;
import reika.dragonapi.instantiable.effects.StringParticleFX;
import reika.dragonapi.instantiable.rendering.GlowLayer;

/** Physical-client entry point for DragonAPI's additional render submissions. */
public final class ClientRendering {
    private ClientRendering() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((net.neoforged.neoforge.client.event.AddClientReloadListenersEvent event) ->
                event.addListener(net.minecraft.resources.Identifier.parse("dragonapi:direct_assets"), reika.dragonapi.io.DirectResourceManager.getInstance()));
        modBus.addListener((RegisterParticleGroupsEvent event) -> event.register(StringParticleFX.GROUP, TextParticleGroup::new));
        modBus.addListener((EntityRenderersEvent.AddLayers event) -> {
            for (var skin : event.getSkins()) {
                var renderer = event.getPlayerRenderer(skin);
                if (renderer != null) renderer.addLayer(new GlowLayer(renderer));
            }
            PlayerSpecificRenderer.instance.loadGlowFiles();
        });
        NeoForge.EVENT_BUS.addListener(ClientRendering::renderPlayer);
    }

    private static void renderPlayer(RenderPlayerEvent.Post<?> event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (minecraft.level.getEntity(event.getRenderState().id) instanceof Player player
                && !player.isInvisibleTo(minecraft.player))
            PlayerSpecificRenderer.instance.renderAdditionalObjects(event.getPoseStack(), player,
                    event.getPartialTick(), event.getSubmitNodeCollector());
    }
}
