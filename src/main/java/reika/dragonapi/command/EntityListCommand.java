package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
        String[] args = {"pig"};

        dispatcher.register(Commands.literal("entitylist").executes((context) -> {
            if (args.length != 1) {
                context.getSource().sendFailure(Component.literal(ChatFormatting.RED + "Invalid arguments. Use /" + "entitylist" + " <side>."));
            }

            Dist side = null;
            if(context.getSource().getPlayerOrException() != null) {
                side = Dist.CLIENT;
            } else{
                side = Dist.DEDICATED_SERVER;
            }

            try {
//                side = Dist.valueOf(args[0].toUpperCase());
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
                context.getSource().sendSuccess(Component.literal(sb.toString()), true);
                return 1;
            }

            ServerPlayer ep = context.getSource().getPlayerOrException();
            ReikaChatHelper.sendChatToPlayer(ep, "Found entities:");
            perform(side, ep);
            return 1;
        }));
    }


    public static void dumpClientside() {
        ArrayList<String> data = getData(Minecraft.getInstance().player, Dist.CLIENT);
        for (String s : data) {
            ReikaChatHelper.writeString(s);
        }
    }

    private static void perform(Dist side, ServerPlayer ep) {
        switch (side) {
            case CLIENT -> sendPacket(ep);
            case DEDICATED_SERVER -> {
                ArrayList<String> data = getData(ep, side);
                for (String s : data) {
                    ReikaChatHelper.sendChatToPlayer(ep, s);
                }
            }
        }
    }

    private static void sendPacket(ServerPlayer ep) {
        ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.ENTITYDUMP.ordinal(), ep);
    }

    private static ArrayList<String> getData(Player ep, Dist side) {
        ArrayList<String> li = new ArrayList<>();
        String sd = ReikaStringParser.capFirstChar(side.name());
//    todo    for (Class c : ((Map<Class, String>)EntityList.classToStringMapping).keySet()) {
//            String s = (String)EntityList.classToStringMapping.get(c);
//            if (s == null)
//                s = "[NO NAME]";
//            else if (s.isEmpty())
//                s = "[EMPTY STRING]";
//            Integer id = (Integer)EntityList.stringToIDMapping.get(s);
//            String sid = id != null ? String.valueOf(id) : "[NO ID]";
//            String loc = ReikaEntityHelper.getEntityDisplayName(s);
//            li.add(String.format("%s - '%s': Class = %s; ID = %s; Name = '%s'", sd, s, c.getName(), sid, loc));
//        }
        return li;
    }
}
