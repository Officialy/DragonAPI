package reika.dragonapi.instantiable.event.client;

import net.minecraft.client.Options;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;


public abstract class SettingsEvent extends Event {

    public final Options settings;

    public SettingsEvent(Options gs) {
        settings = gs;
    }

    public static void fireLoad(Options gs) {
        MinecraftForge.EVENT_BUS.post(new SettingsEvent.Load(gs));
    }

    public static void fireSave(Options gs) {
        MinecraftForge.EVENT_BUS.post(new SettingsEvent.Save(gs));
    }

    public static class Load extends SettingsEvent {

        public Load(Options gs) {
            super(gs);
        }

    }

    public static class Save extends SettingsEvent {

        public Save(Options gs) {
            super(gs);
        }

    }

}
