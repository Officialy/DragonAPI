package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GuideCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ItemStack is = new ItemStack(Items.ENCHANTED_BOOK);
        CompoundTag data = new CompoundTag();
        ListTag data2 = new ListTag();
        data2.add(StringTag.valueOf("Reika's Mods Guide"));
        data.put("Lore", data2);

        is.getOrCreateTag().put("display", data);

        dispatcher.register(Commands.literal("guide").executes((context) -> {
            context.getSource().getPlayerOrException().getInventory().add(is);
            return 1;
        }));
    }

}
