package reika.dragonapi.instantiable.event.client;

<<<<<<< Updated upstream:Instantiable/Event/Client/WinterColorsEvent.java
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.eventhandler.Event;
=======
import net.neoforged.common.NeoForge;
import net.neoforged.eventbus.api.Event;
>>>>>>> Stashed changes:src/main/java/reika/dragonapi/instantiable/event/client/WinterColorsEvent.java


public abstract class WinterColorsEvent extends Event {

	public final int defaultColor;
	public int chosenColor;

	public WinterColorsEvent(int c) {
		defaultColor = c;
		chosenColor = defaultColor;
	}

<<<<<<< Updated upstream:Instantiable/Event/Client/WinterColorsEvent.java
=======
	public static int getSkyColor() {
		WinterColorsEvent evt = new WinterSkyColorsEvent();
		NeoForge.EVENT_BUS.post(evt);
		return evt.chosenColor;
	}

	public static int getFogColor() {
		WinterColorsEvent evt = new WinterFogColorsEvent();
		NeoForge.EVENT_BUS.post(evt);
		return evt.chosenColor;
	}

>>>>>>> Stashed changes:src/main/java/reika/dragonapi/instantiable/event/client/WinterColorsEvent.java
	public static class WinterSkyColorsEvent extends WinterColorsEvent {

		public WinterSkyColorsEvent() {
			super(0x688499);
		}

	}

	public static class WinterFogColorsEvent extends WinterColorsEvent {

		public WinterFogColorsEvent() {
			super(0x425766);
		}

	}

}
