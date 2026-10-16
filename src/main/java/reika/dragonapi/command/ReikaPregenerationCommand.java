package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import reika.dragonapi.auxiliary.trackers.BoundedPopulation;
import reika.dragonapi.instantiable.worldgen.PopulationScope;

public final class ReikaPregenerationCommand {
    private ReikaPregenerationCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("reikapregen").requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                .then(Commands.literal("start").then(Commands.argument("center", BlockPosArgument.blockPos())
                        .then(Commands.argument("chunkRadius", IntegerArgumentType.integer(0, 2500)).executes(ctx -> {
                            ChunkPos center = ChunkPos.containing(BlockPosArgument.getBlockPos(ctx, "center"));
                            int radius = IntegerArgumentType.getInteger(ctx, "chunkRadius");
                            return start(ctx, PopulationScope.rectangle(center.x() - radius, center.z() - radius,
                                    center.x() + radius, center.z() + radius));
                        }))))
                .then(Commands.literal("corners").then(Commands.argument("from", BlockPosArgument.blockPos())
                        .then(Commands.argument("to", BlockPosArgument.blockPos()).executes(ctx -> {
                            ChunkPos a = ChunkPos.containing(BlockPosArgument.getBlockPos(ctx, "from"));
                            ChunkPos b = ChunkPos.containing(BlockPosArgument.getBlockPos(ctx, "to"));
                            return start(ctx, PopulationScope.rectangle(Math.min(a.x(), b.x()), Math.min(a.z(), b.z()),
                                    Math.max(a.x(), b.x()), Math.max(a.z(), b.z())));
                        }))))
                .then(Commands.literal("status").executes(ctx -> {
                    var messages = BoundedPopulation.get(ctx.getSource().getLevel()).statusSnapshot();
                    if (messages.isEmpty()) ctx.getSource().sendSuccess(() -> Component.translatable("command.dragonapi.pregen.empty"), false);
                    messages.forEach(message -> ctx.getSource().sendSuccess(() -> Component.translatable("command.dragonapi.pregen.status",
                            message.id(), message.source(), Component.translatable("command.dragonapi.pregen.state." + message.state().name().toLowerCase(java.util.Locale.ROOT)),
                            message.originChunks(), message.pendingJobs()), false));
                    return messages.size();
                }))
                .then(Commands.literal("pause").executes(ctx -> control(ctx, "pause")))
                .then(Commands.literal("resume").executes(ctx -> control(ctx, "resume")))
                .then(Commands.literal("cancel").executes(ctx -> control(ctx, "cancel"))));
    }
    private static int start(CommandContext<CommandSourceStack> ctx, PopulationScope origins) {
        long id = BoundedPopulation.get(ctx.getSource().getLevel()).begin("manual", "manual/" + System.nanoTime(), origins, false);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.dragonapi.pregen.started", id, origins.size()), true);
        return 1;
    }
    private static int control(CommandContext<CommandSourceStack> ctx, String action) {
        var population = BoundedPopulation.get(ctx.getSource().getLevel());
        switch (action) {
            case "pause" -> population.pauseAll();
            case "resume" -> population.resumeAll();
            case "cancel" -> population.cancelAll();
            default -> throw new IllegalArgumentException(action);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.dragonapi.pregen." + action), true);
        return 1;
    }
}
