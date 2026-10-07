package reika.dragonapi.client;

import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only holder for full recipes sent by NeoForge; cleared on disconnect. */
public final class SyncedRecipeLookup {
    private static RecipeMap recipes;
    private SyncedRecipeLookup() {}
    public static void register() {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RecipesReceivedEvent event) -> { recipes = event.getRecipeMap(); reika.dragonapi.libraries.ReikaIngredientHelper.clearCache(); });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) -> {
            recipes = null;
            ClientKeyWatcher.reset();
            reika.dragonapi.libraries.ReikaIngredientHelper.clearCache();
            reika.dragonapi.command.BiomeMapCommand.clearClientMaps();
            reika.dragonapi.auxiliary.PopupWriter.list.clear();
        });
    }
    public static RecipeMap recipes() { return recipes; }
}
