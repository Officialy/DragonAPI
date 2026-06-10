package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

public class GuideCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 1.21.5: building the ItemStack eagerly here (during RegisterCommandsEvent → command tree
        // construction → ReloadableServerResources.<init>) hits Holder$Reference.components()
        // before the item holders' components are bound and throws
        // "java.lang.NullPointerException: Components not bound yet".
        // Build the stack lazily inside the executor instead — by the time /guide runs the
        // registries are long-since bound.
        dispatcher.register(Commands.literal("guide").executes((context) -> {
            ItemStack is = new ItemStack(Items.ENCHANTED_BOOK);
            is.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Reika's Mods Guide"));
            context.getSource().getPlayerOrException().getInventory().add(is);
            return 1;
        }));
    }

}
