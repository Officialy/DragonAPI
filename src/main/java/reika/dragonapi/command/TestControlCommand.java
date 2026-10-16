package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import reika.dragonapi.DragonAPI;

public final class TestControlCommand {
    private TestControlCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("debugtest")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .executes(context -> {
                    DragonAPI.debugtest = !DragonAPI.debugtest;
                    String key = DragonAPI.debugtest ? "command.dragonapi.debugtest.enabled" : "command.dragonapi.debugtest.disabled";
                    context.getSource().sendSuccess(() -> Component.translatable(key), false);
                    return 1;
                }));
    }
}
