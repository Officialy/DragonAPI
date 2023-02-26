package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.DonatorController;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.trackers.PatreonController;

public class TestControlCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("debugtest").executes((context) -> {
            DragonAPI.debugtest = !DragonAPI.debugtest;
//            ReikaChatHelper.sendChatToAllOnServer("Debug Test Mode: "+DragonAPI.debugtest);
            ReikaChatHelper.sendChatToPlayer(context.getSource().getPlayerOrException(),"Debug Test Mode: " + DragonAPI.debugtest);
            return 1;
        }));
    }
}