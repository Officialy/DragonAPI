/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.trackers;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.maps.CountMap;
import reika.dragonapi.io.ReikaFileReader;
import reika.dragonapi.libraries.java.ReikaStringParser;
import net.minecraft.ChatFormatting;
import reika.dragonapi.auxiliary.trackers.DonatorController;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public final class PatreonController {

	public static final PatreonController instance = new PatreonController();

	public static final String reikaURL = "http://server.techjargaming.com/Reika/Donator/patreon_";

	private final HashMap<String, Patrons> data = new HashMap();

	private PatreonController() {

	}

	public void registerMod(String dev, String root) {
		String url = root + ReikaStringParser.stripSpaces(dev) + ".txt";
		URL file = this.getURL(url);
		if (file == null) {
			DragonAPI.LOGGER.debug(DragonAPI.MODID + "Could not create URL to patreon file. Donators will not be loaded.");
			return;
		}
		DonatorFile f = new DonatorFile(dev);
		ArrayList<String> lines = ReikaFileReader.getFileAsLines(file, 10000, false, f);
		if (lines != null) {
			DragonAPI.LOGGER.debug("Loading " + lines.size() + " patrons for " + dev);
			this.addPatrons(dev, lines);
		}
	}

	private void addPatrons(String dev, ArrayList<String> lines) {
		for (String s : lines) {
			s = ReikaStringParser.stripSpaces(s);
			try {
				this.tryLoadingPatron(dev, s);
			} catch (Exception e) {
				DragonAPI.LOGGER.debug("Invalid patreon line: " + s + " for " + dev + ": " + e);
			}
		}
	}

	private void tryLoadingPatron(String dev, String s) {
		String[] parts = s.split(":");
		parts[parts.length - 1] = ReikaStringParser.clipStringBefore(parts[parts.length - 1], "//");
		if (parts.length == 3) {
			this.addPatron(dev, parts[0], parts[1], Integer.parseInt(parts[2]));
		} else if (parts.length == 2) {
			this.addPatron(dev, parts[0], Integer.parseInt(parts[1]));
		} else {
			throw new IllegalArgumentException("Too few arguments!");
		}
	}

	private URL getURL(String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void addPatron(String dev, String name, int amt) {
		this.addPatron(dev, name, null, amt);
	}

	private void addPatron(String dev, String name, String ingame, int amt) {
		Patrons p = this.getOrCreate(dev);
		p.addPatron(name, ingame != null ? UUID.fromString(ingame) : null, amt);
		DragonAPI.LOGGER.debug("Adding patron to " + dev + ": " + name + " / " + ingame + " @ $" + amt);
	}

	private Patrons getOrCreate(String dev) {
		Patrons p = data.get(dev);
		if (p == null) {
			p = new Patrons();
			data.put(dev, p);
		}
		return p;
	}

	public Collection<DonatorController.Donator> getModPatrons(String dev) {
		Patrons p = data.get(dev);
		return p != null ? Collections.unmodifiableCollection(p.data.keySet()) : new ArrayList<>();
	}

	public int getAmount(String dev, String name, UUID id) {
		return data.get(dev).getAmount(name, id);
	}

	public Collection<DonatorController.Donator> getPatronsOver(String dev, int amount) {
		return data.get(dev).getPatronsOver(amount);
	}

	public boolean isPatronAtLeast(String dev, String name, UUID id, int amount) {
		return data.get(dev).isPatronAtLeast(name, id, amount);
	}

	public int getTotal(String dev) {
		return data.get(dev).getTotal();
	}

	@Override
	public String toString() {
		return data.toString();
	}

	public String getDisplayList() {
		StringBuilder sb = new StringBuilder();
		for (String dev : data.keySet()) {
			Patrons li = data.get(dev);
			sb.append(ChatFormatting.BLUE);
			sb.append("Patreon for ");
			sb.append(dev);
			sb.append(":\n");
			sb.append(li.toString());
			//sb.append("\n");
		}
		return sb.toString();
	}

	private static class DonatorFile implements ReikaFileReader.ConnectionErrorHandler {

		private final String dev;

		private DonatorFile(String dev) {
			this.dev = dev;
		}

		@Override
		public void onServerRedirected() {
			DragonAPI.LOGGER.debug("Donator server not found!");
		}

		@Override
		public void onNoInternet() {
			DragonAPI.LOGGER.debug("Error accessing online file: Is your internet disconnected?");
		}

		@Override
		public void onServerNotFound() {
			DragonAPI.LOGGER.debug("Donator server not found!");
		}

		@Override
		public void onTimedOut() {
			DragonAPI.LOGGER.debug("Error accessing online file: Timed Out");
		}

	}

	private static class Patrons {

		private final CountMap<DonatorController.Donator> data = new CountMap();

		private int total;

		private void addPatron(String name, UUID id, int amt) {
			DonatorController.Donator d = new DonatorController.Donator(name, id);
			if (data.containsKey(d)) {
				throw new MisuseException("You cannot have two copies of the same patron!");
			} else {
				data.increment(d, amt);
				total += amt;
			}
		}

		public int getTotal() {
			return total;
		}

		private int getAmount(String name, UUID id) {
			DonatorController.Donator d = new DonatorController.Donator(name, id);
			return data.get(d);
		}

		private Collection<DonatorController.Donator> getPatronsOver(int amount) {
			ArrayList<DonatorController.Donator> li = new ArrayList<>();
			for (DonatorController.Donator d : data.keySet()) {
				int f = data.get(d);
				if (f >= amount)
					li.add(d);
			}
			return li;
		}

		private boolean isPatronAtLeast(String name, UUID id, int amount) {
			return this.getAmount(name, id) >= amount;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (DonatorController.Donator d : data.keySet()) {
				int amt = data.get(d);
				sb.append(String.format("  %s%s%s: %s%d%s\n", this.getDisplayColor(amt).toString(), this.getFormatting(amt), d.toString(), "$", amt, "/mo"));
			}
			return sb.toString();
		}

		public String getFormatting(int amt) {
			return /*amt >= 50 ? EnumChatFormatting.BOLD.toString() : */"";
		}

		public ChatFormatting getDisplayColor(int amt) {
			if (amt >= 30) {
				return ChatFormatting.GOLD;
			} else if (amt >= 20) {
				return ChatFormatting.LIGHT_PURPLE;
			} else if (amt >= 10) {
				return ChatFormatting.GREEN;
			} else {
				return ChatFormatting.WHITE;
			}
		}

	}

}
