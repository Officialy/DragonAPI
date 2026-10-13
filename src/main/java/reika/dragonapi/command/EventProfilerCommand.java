package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.EventProfiler;
import reika.dragonapi.libraries.java.ReikaStringParser;

import java.util.Locale;
public class EventProfilerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("profileevent")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("disable").executes(context -> processCommand(context.getSource(), new String[]{"disable"})))
                .then(Commands.literal("display").executes(context -> processCommand(context.getSource(), new String[]{"display"})))
                .then(Commands.literal("enable").then(Commands.argument("event", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(context -> processCommand(context.getSource(), new String[]{"enable", com.mojang.brigadier.arguments.StringArgumentType.getString(context, "event")})))));
    }

    private static void feedback(CommandSourceStack source, String text) {
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(text), false);
    }

    public static int processCommand(CommandSourceStack sourceStack, String[] args) throws CommandSyntaxException {
        if (args.length < 1) {
            feedback(sourceStack, ChatFormatting.RED + "Wrong number of arguments. Specify 'disable', 'enable', or 'display'.");
            return 0;
        }
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "disable" -> {
                EventProfiler.finishProfiling();
                feedback(sourceStack, ChatFormatting.GREEN + "Profiling finished.");
            }
            case "enable" -> {
                if (args.length < 2) {
                    feedback(sourceStack, ChatFormatting.RED + "You must specify an event type (class)!");
                    return 0;
                }
                EventProfiler.ProfileStartStatus st = EventProfiler.startProfiling(args[1]);
                switch (st) {
                    case SUCCESS ->
                            feedback(sourceStack, ChatFormatting.GREEN + "Profiling started for events of type " + args[1]);
                    case ALREADYRUNNING ->
                            feedback(sourceStack, ChatFormatting.RED + "Profiling already running!");
                    case NOSUCHCLASS ->
                            feedback(sourceStack, ChatFormatting.RED + "No such class '" + args[1] + "'!");
                    case AMBIGUOUS -> feedback(sourceStack, ChatFormatting.RED + "Ambiguous shortcut; use an enclosing-class name such as EntityTickEvent.Pre or the full class name.");
                    case NOTPOSTABLE -> feedback(sourceStack, ChatFormatting.RED + "Choose a concrete event accepted by the game bus.");
                    case UNAVAILABLE -> feedback(sourceStack, ChatFormatting.RED + "Event profiling instrumentation or this event class is unavailable; check the startup log.");
                    case NOTANEVENT ->
                            feedback(sourceStack, ChatFormatting.RED + "Class '" + args[1] + "' does not extend Event!");
                }
            }
            case "display" -> {
                var snapshot = EventProfiler.snapshot();
                String type = snapshot.eventType();
                var li = snapshot.handlers();
                long fires = snapshot.eventFires();
                long total = snapshot.totalHandlerTime();
                String totalt = String.format("%.6f", total / 1000000D);
                String desc = "Profiling data for event type " + type + " (" + snapshot.scope() + ") contains " + li.size() + " handlers across " + fires + " event fires, total time " + totalt + " ms (inclusive handler time):";
                feedback(sourceStack, desc);
                DragonAPI.LOGGER.info(desc);
                for (EventProfiler.EventProfile g : li) {
                    long time = g.getAverageTime();
                    String s = ReikaStringParser.padToLength("'" + g.identifier + "'", 60, " ");
                    double percent = total == 0 ? 0 : g.getTotalTime() * 100D / total;
                    String sg = String.format("Handler %s - Average Time Per Fire: %7.3f microseconds (%2.3f%s)", s, time / 1000D, percent, "%");
                    feedback(sourceStack, sg + " [calls=" + g.getFireCount() + ", failures=" + g.getFailureCount() + "]");
                    DragonAPI.LOGGER.info(sg);
                }
            }
            default ->
                    feedback(sourceStack, ChatFormatting.RED + "Invalid argument. Specify 'disable', 'enable', or 'display'.");
        }
        return 1;

    }
}
