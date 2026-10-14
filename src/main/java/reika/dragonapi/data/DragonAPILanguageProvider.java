package reika.dragonapi.data;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import reika.dragonapi.DragonAPI;

/** Client resources for shared commands; translated components survive server delivery. */
public final class DragonAPILanguageProvider extends LanguageProvider {
    public DragonAPILanguageProvider(PackOutput output) {
        super(output, DragonAPI.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("dragonapi.donatorcommand", "Thank you to all these people whose donations made the following mods possible:\n");
        add("dragonapi.checkram", "Memory Usage Info: \n");
        add("dragonapi.checkram1", "Used Memory: ");
        add("dragonapi.checkram2", "Allocated Memory: ");
        add("dragonapi.uuidcommand", "UUID is: ");
        add("config.jade.plugin_dragonapi.temperature", "Temperature");
        add("item.dragonapi.guide.name", "Reika's Mods Guide");
        add("command.dragonapi.entitylist.invalid_side", "Invalid side. Use one of: %s.");
        add("command.dragonapi.entitylist.player_required", "Client entity data can only be requested by a player.");
        add("command.dragonapi.entitylist.header", "Found entities:");
        add("command.dragonapi.modularlog.unknown", "Unrecognized logger ID '%s'!");
        add("command.dragonapi.modularlog.enabled", "Logger '%s' enabled.");
        add("command.dragonapi.modularlog.disabled", "Logger '%s' disabled.");
        add("command.dragonapi.biomepng.player_missing", "If you specify a player, they must exist.");
        add("command.dragonapi.biomepng.error", "Cannot generate biome map: %s");
        add("command.dragonapi.biomepng.created", "File created in %s ms: %s");
        add("command.dragonapi.biomepng.failed", "Failed to create file: %s");
    }
}
