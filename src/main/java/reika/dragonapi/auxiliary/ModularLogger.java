package reika.dragonapi.auxiliary;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraft.network.chat.Component;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.exception.RegistrationException;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

import java.util.HashMap;
import java.util.Locale;

public class ModularLogger {

    public static final ModularLogger instance = new ModularLogger();

    private final HashMap<String, LoggerElement> loggers = new HashMap<>();

    private ModularLogger() {

    }

    public void addLogger(DragonAPIMod mod, String label) {
        label = label.toLowerCase(Locale.ENGLISH);
        if (loggers.containsKey(label))
            throw new RegistrationException(mod, "Modular logger name '" + label + "' is already taken!");
        loggers.put(label, new LoggerElement(mod, label));
    }

    public void log(String logger, String msg) {
        LoggerElement e = loggers.get(logger.toLowerCase(Locale.ENGLISH));
        if (e == null) {
            DragonAPI.LOGGER.error("Tried to use an unregistered logger '" + logger + "'!");
        } else {
            if (e.enabled) {
                e.mod.getModLogger().info(msg);
            }
        }
    }

    public boolean isEnabled(String logger) {
        LoggerElement e = loggers.get(logger.toLowerCase(Locale.ENGLISH));
        return e != null && e.enabled;
    }

    private static final class LoggerElement {

        private final DragonAPIMod mod;
        private final String label;

        private boolean enabled;

        public LoggerElement(DragonAPIMod mod, String s) {
            this.mod = mod;
            label = s;
        }

    }

    public void setState(String logger, boolean enable) {
        String id = logger.toLowerCase(Locale.ENGLISH);
        LoggerElement e = instance.loggers.get(id);
        if (e != null) {
            e.enabled = enable;
        }
    }

    public static class ModularLoggerCommand {

        public static void register(CommandDispatcher<CommandSourceStack> dispatcher, String[] args) {
            dispatcher.register(Commands.literal("modularlog").requires((source) -> source.hasPermission(3)).executes((context) -> {
                if (args.length != 2) {
                    context.getSource().sendFailure(Component.literal(ChatFormatting.RED + "You must specify a logger ID and a status!"));
                }
                String id = args[0].toLowerCase(Locale.ENGLISH);
                LoggerElement e = instance.loggers.get(id);
                if (e == null) {
                    context.getSource().sendFailure(Component.literal(ChatFormatting.RED + "Unrecognized logger ID '" + args[0] + "'!"));
                }
                e.enabled = args[1].equalsIgnoreCase("yes") || args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("1") || Boolean.parseBoolean(args[1]);
                String status = e.enabled ? "enabled" : "disabled";
                context.getSource().sendSuccess(() -> Component.literal(ChatFormatting.GREEN + "Logger '" + args[0] + "' " + status + "."), false);
                ReikaPacketHelper.sendStringIntPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.MODULARLOGGER.ordinal(), PacketTarget.allPlayers, id, e.enabled ? 1 : 0);

                return 1;
            }));
        }

    }
}
