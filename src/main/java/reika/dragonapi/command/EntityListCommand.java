package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

import java.util.ArrayList;
import java.util.Locale;
public class EntityListCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("entitylist")
                .then(Commands.argument("side", StringArgumentType.word()).executes(context -> {
            Dist side;
            try {
                side = Dist.valueOf(StringArgumentType.getString(context, "side").toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                String sides = java.util.Arrays.stream(Dist.values())
                        .map(value -> value.name().toLowerCase(Locale.ROOT))
                        .collect(java.util.stream.Collectors.joining(", "));
                context.getSource().sendFailure(Component.translatable("command.dragonapi.entitylist.invalid_side", sides));
                return 0;
            }

            CommandSourceStack source = context.getSource();
            if (side == Dist.CLIENT) {
                if (!(source.getEntity() instanceof ServerPlayer ep)) {
                    source.sendFailure(Component.translatable("command.dragonapi.entitylist.player_required"));
                    return 0;
                }
                ep.sendSystemMessage(Component.translatable("command.dragonapi.entitylist.header"));
                sendPacket(ep);
            } else {
                source.sendSuccess(() -> Component.translatable("command.dragonapi.entitylist.header"), false);
                for (String line : getData(side))
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
        ArrayList<String> data = getData(Dist.CLIENT);
        for (String s : data) {
            ReikaChatHelper.writeString(s);
        }
    }

    private static void sendPacket(ServerPlayer ep) {
        ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.ENTITYDUMP.wireId(), ep);
    }

    private static ArrayList<String> getData(Dist side) {
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
