package reika.dragonapi.libraries;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Server-only bridge for gameplay milestones whose criteria are awarded by mod logic. */
public final class AdvancementHelper {
    private AdvancementHelper() {}

    public static void grant(Player player, Identifier id) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        var advancement = serverPlayer.level().getServer().getAdvancements().get(id);
        if (advancement == null)
            return; // Datapacks may remove an optional advancement.
        var progress = serverPlayer.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : java.util.stream.StreamSupport.stream(
                progress.getRemainingCriteria().spliterator(), false).toList())
            serverPlayer.getAdvancements().award(advancement, criterion);
    }

    public static void revoke(Player player, Identifier id) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        var advancement = serverPlayer.level().getServer().getAdvancements().get(id);
        if (advancement == null)
            return;
        var progress = serverPlayer.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : java.util.stream.StreamSupport.stream(
                progress.getCompletedCriteria().spliterator(), false).toList())
            serverPlayer.getAdvancements().revoke(advancement, criterion);
    }
}
