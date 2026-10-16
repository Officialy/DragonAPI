package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.EventProfiler;

import java.util.Locale;

public final class EventProfilerCommand {
    private EventProfilerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("profileevent")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("disable").executes(context -> processCommand(context.getSource(), new String[]{"disable"})))
                .then(Commands.literal("display").executes(context -> processCommand(context.getSource(), new String[]{"display"})))
                .then(Commands.literal("enable").then(Commands.argument("event", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(context -> processCommand(context.getSource(), new String[]{"enable", com.mojang.brigadier.arguments.StringArgumentType.getString(context, "event")})))));
    }

    private static int failure(CommandSourceStack source, String key, Object... arguments) {
        source.sendFailure(Component.translatable("command.dragonapi.profile." + key, arguments));
        return 0;
    }

    public static int processCommand(CommandSourceStack source, String[] args) throws CommandSyntaxException {
        if (args.length == 0) return failure(source, "usage");
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "disable" -> {
                EventProfiler.finishProfiling();
                source.sendSuccess(() -> Component.translatable("command.dragonapi.profile.finished").withStyle(ChatFormatting.GREEN), false);
            }
            case "enable" -> {
                if (args.length < 2) return failure(source, "missing_type");
                var status = EventProfiler.startProfiling(args[1]);
                if (status != EventProfiler.ProfileStartStatus.SUCCESS)
                    return failure(source, status.name().toLowerCase(Locale.ROOT), args[1]);
                source.sendSuccess(() -> Component.translatable("command.dragonapi.profile.started", args[1]).withStyle(ChatFormatting.GREEN), false);
            }
            case "display" -> {
                var snapshot = EventProfiler.snapshot();
                long total = snapshot.totalHandlerTime();
                String milliseconds = String.format(Locale.ROOT, "%.6f", total / 1_000_000D);
                source.sendSuccess(() -> Component.translatable("command.dragonapi.profile.summary", snapshot.eventType(), snapshot.scope(),
                        snapshot.handlers().size(), snapshot.eventFires(), milliseconds), false);
                DragonAPI.LOGGER.info("Profiling data for event type {} ({}) contains {} handlers across {} event fires, total time {} ms (inclusive handler time)",
                        snapshot.eventType(), snapshot.scope(), snapshot.handlers().size(), snapshot.eventFires(), milliseconds);
                for (var handler : snapshot.handlers()) {
                    String average = String.format(Locale.ROOT, "%.3f", handler.getAverageTime() / 1000D);
                    String percent = String.format(Locale.ROOT, "%.3f", total == 0 ? 0 : handler.getTotalTime() * 100D / total);
                    source.sendSuccess(() -> Component.translatable("command.dragonapi.profile.handler", handler.identifier,
                            average, percent, handler.getFireCount(), handler.getFailureCount()), false);
                    DragonAPI.LOGGER.info("Handler '{}' - Average Time Per Fire: {} microseconds ({}%) [calls={}, failures={}]",
                            handler.identifier, average, percent, handler.getFireCount(), handler.getFailureCount());
                }
            }
            default -> { return failure(source, "usage"); }
        }
        return 1;
    }
}
