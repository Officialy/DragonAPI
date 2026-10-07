package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.ReikaEntityHelper;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.java.ReikaStringParser;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
public class EntityListCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("entitylist")
                .then(Commands.argument("side", StringArgumentType.word()).executes(context -> {
            Dist side;
            try {
                side = Dist.valueOf(StringArgumentType.getString(context, "side").toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                StringBuilder sb = new StringBuilder();
                sb.append(ChatFormatting.RED + "Invalid side. Use one of the following: ");
                for (int i = 0; i < Dist.values().length; i++) {
                    sb.append("'");
                    sb.append(Dist.values()[i].name().toLowerCase(Locale.ENGLISH));
                    sb.append("'");
                    if (i < Dist.values().length - 1)
                        sb.append(", ");
                }
                sb.append(".");
                context.getSource().sendSuccess(() -> Component.literal(sb.toString()), true);
                return 1;
            }

            CommandSourceStack source = context.getSource();
            if (side == Dist.CLIENT) {
                if (!(source.getEntity() instanceof ServerPlayer ep)) {
                    source.sendFailure(Component.literal("Client entity data can only be requested by a player."));
                    return 0;
                }
                ReikaChatHelper.sendChatToPlayer(ep, "Found entities:");
                sendPacket(ep);
            } else {
                source.sendSuccess(() -> Component.literal("Found entities:"), false);
                Player player = source.getEntity() instanceof Player p ? p : null;
                for (String line : getData(player, side))
                    source.sendSuccess(() -> Component.literal(line), false);
            }
            return 1;
        })));
    }


    public static void dumpClientside() {
        // Via the client holder: naming Minecraft.player here puts a LocalPlayer descriptor in this
        // command class, which the dedicated server loads when it registers commands.
        if (!FMLEnvironment.getDist().isClient())
            return;
        ArrayList<String> data = getData(reika.dragonapi.client.ClientEnvironment.player(), Dist.CLIENT);
        for (String s : data) {
            ReikaChatHelper.writeString(s);
        }
    }

    private static void sendPacket(ServerPlayer ep) {
        ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.ENTITYDUMP.ordinal(), ep);
    }

    private static ArrayList<String> getData(Player ep, Dist side) {
        ArrayList<String> li = new ArrayList<>();
        var registry = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE;
        for (var type : registry) {
            li.add(String.format("%s - '%s': ID = %d; Name = '%s'; Category = %s",
                    side.name(), registry.getKey(type), registry.getId(type), type.getDescription().getString(), type.getCategory()));
        }
        li.sort(String::compareTo);
        return li;
    }
}
