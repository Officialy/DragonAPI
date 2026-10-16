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
        add("command.dragonapi.profile.usage", "Specify 'disable', 'enable', or 'display'.");
        add("command.dragonapi.profile.finished", "Profiling finished.");
        add("command.dragonapi.profile.missing_type", "You must specify an event type (class)!");
        add("command.dragonapi.profile.started", "Profiling started for events of type %s");
        add("command.dragonapi.profile.alreadyrunning", "Profiling already running!");
        add("command.dragonapi.profile.nosuchclass", "No such class '%s'!");
        add("command.dragonapi.profile.ambiguous", "Ambiguous shortcut; use an enclosing-class name such as EntityTickEvent.Pre or the full class name.");
        add("command.dragonapi.profile.notpostable", "Choose a concrete event accepted by the game bus.");
        add("command.dragonapi.profile.unavailable", "Event profiling instrumentation or this event class is unavailable; check the startup log.");
        add("command.dragonapi.profile.notanevent", "Class '%s' does not extend Event!");
        add("command.dragonapi.profile.summary", "Profiling data for event type %s (%s) contains %s handlers across %s event fires, total time %s ms (inclusive handler time):");
        add("command.dragonapi.profile.handler", "Handler %s - Average Time Per Fire: %s microseconds (%s%%) [calls=%s, failures=%s]");
        add("command.dragonapi.pregen.empty", "No bounded pregeneration sessions in this dimension.");
        add("command.dragonapi.pregen.started", "Population #%s started for %s origin chunks. Finishes queued structures; use a terrain pregenerator to fill missing terrain.");
        add("command.dragonapi.pregen.status", "#%s %s %s: %s origin chunks, %s pending jobs");
        add("command.dragonapi.pregen.state.cancelled", "cancelled");
        add("command.dragonapi.pregen.state.complete", "complete");
        add("command.dragonapi.pregen.state.paused", "paused");
        add("command.dragonapi.pregen.state.terrain_running", "terrain running");
        add("command.dragonapi.pregen.state.finishing_structures", "finishing structures");
        add("command.dragonapi.pregen.pause", "Bounded population paused in this dimension. The terrain pregenerator has separate controls.");
        add("command.dragonapi.pregen.resume", "Bounded population resumed in this dimension. The terrain pregenerator has separate controls.");
        add("command.dragonapi.pregen.cancel", "Bounded population cancelled in this dimension. The terrain pregenerator has separate controls.");
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
