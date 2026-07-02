package reika.dragonapi.auxiliary;

import java.util.HashMap;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

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

        public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
            dispatcher.register(Commands.literal("modularlog")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.argument("id", StringArgumentType.string())
                    .then(Commands.argument("state", StringArgumentType.string())
                        .executes((context) -> {
                            String id = StringArgumentType.getString(context, "id").toLowerCase(Locale.ENGLISH);
                            String stateStr = StringArgumentType.getString(context, "state");
                            
                            LoggerElement e = instance.loggers.get(id);
                            if (e == null) {
                                context.getSource().sendFailure(Component.literal(ChatFormatting.RED + "Unrecognized logger ID '" + id + "'!"));
                                return 0;
                            }
                            
                            e.enabled = stateStr.equalsIgnoreCase("yes") || stateStr.equalsIgnoreCase("enable") || stateStr.equalsIgnoreCase("1") || Boolean.parseBoolean(stateStr);
                            String status = e.enabled ? "enabled" : "disabled";
                            context.getSource().sendSuccess(() -> Component.literal(ChatFormatting.GREEN + "Logger '" + id + "' " + status + "."), false);
                            ReikaPacketHelper.sendStringIntPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.MODULARLOGGER.ordinal(), PacketTarget.allPlayers, id, e.enabled ? 1 : 0);

                            return 1;
                        })
                    )
                )
            );
        }

    }
}
