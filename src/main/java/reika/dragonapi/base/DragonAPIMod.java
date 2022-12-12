/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.base;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.Logger;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.auxiliary.trackers.CommandableUpdateChecker;
import reika.dragonapi.auxiliary.trackers.ModFileVersionChecker;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.extras.ModVersion;
import reika.dragonapi.io.ReikaFileReader;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.mathsci.ReikaDateHelper;
import net.minecraftforge.common.MinecraftForge;

import java.io.File;
import java.net.URL;
import java.util.*;

import static net.minecraftforge.fml.loading.FMLLoader.isProduction;

public abstract class DragonAPIMod {
	private static final HashMap<String, ModVersion> modVersions = new HashMap<>();
	private static final HashMap<String, DragonAPIMod> mods = new HashMap<>();
	private static final HashSet<DragonAPIMod> preInitSet = new HashSet<>();

	private static ModVersion apiVersion;
	protected final boolean isDeObf;
	private final ModVersion version;
	private final LoadProfiler profiler;

	public abstract URL getDocumentationSite();
	public abstract URL getBugSite();
	public abstract File getConfigFolder();

	private String fileHash;

	protected DragonAPIMod() {
		profiler = new LoadProfiler(this);
		profiler.startTiming(LoadProfiler.LoadPhase.CONSTRUCT);
		isDeObf = !isProduction();
		if (isDeObf) {
			ReikaJavaLibrary.pConsole(this.getDisplayName() + " is running in a deobfuscated environment!");
		} else {
			ReikaJavaLibrary.pConsole(this.getDisplayName() + " is not running in a deobfuscated environment.");
		}

		version = ModVersion.readFromFile(this);
		modVersions.put(this.getClass().getSimpleName(), version);
		mods.put(this.getTechnicalName(), this);
		ReikaJavaLibrary.pConsole("Registered " + this + " as version " + version);
		if (this.getClass() == DragonAPI.class) {
			apiVersion = version;
		}

		ReikaJavaLibrary.pConsole(this.getTechnicalName() + ": Constructed; Active Classloader is: " + this.getClass().getClassLoader());
		profiler.finishTiming();
	}

	public static DragonAPIMod getByName(String name) {
		return mods.get(name);
	}


	public abstract String getUpdateCheckURL();
	public static Collection<DragonAPIMod> getAllMods() {
		return Collections.unmodifiableCollection(mods.values());
	}


	public final ModContainer getModContainer() {
//		return FMLLoader.instance.getModObjectList().inverse().get(this);
		return ModList.get().getModContainerById(this.getModId()).get(); //todo this is probably fucked lmao
	}

	public abstract String getModId();

	private static void checkFinalPreload(DragonAPIMod mod) {
		preInitSet.add(mod);
		DragonAPI.LOGGER.info("Pre-initialized " + mod.getTechnicalName() + "; preloaded " + preInitSet.size() + "/" + mods.size() + " mods.");
		if (preInitSet.size() == mods.size()) {
			DragonAPI.LOGGER.info("Finished all main preinit phases. Running post-pre init phase on " + preInitSet.size() + " mods.");
		}
	}

	/**
	 * Whether the jar files on client and server need to exactly match.
	 */
	protected boolean requireSameFilesOnClientAndServer() {
		return true;
	}

	public final String getFileHash() {
		return fileHash;
	}

	public final boolean isSource() {
		return version == ModVersion.source;
	}

	protected final void basicSetup() {
		MinecraftForge.EVENT_BUS.register(this);
		checkFinalPreload(this);
//todo		CommandableUpdateChecker.instance.registerMod(this);

//	todo	fileHash = this.isSource() ? "Source" : ReikaFileReader.getHash(this.getModFile(), ReikaFileReader.HashType.SHA256);
//		if (this.requireSameFilesOnClientAndServer() && DragonOptions.COMMON.FILEHASH.get())
//		ModFileVersionChecker.instance.addMod(this);
	}

/*	protected File getModFile() {
		return this.getModContainer().getSource();
	}*/

	public abstract String getDisplayName();

	public abstract String getModAuthorName();

	public final String getTechnicalName() {
		return this.getDisplayName().toUpperCase();
	}
	@Override
	public final String toString() {
		return this.getTechnicalName();
	}

	public final ModVersion getModVersion() {
		return version;
	}

	protected final void startTiming(LoadProfiler.LoadPhase p) {
		profiler.startTiming(p);
	}

	protected final void finishTiming() {
		profiler.finishTiming();
	}

	@Override
	public final boolean equals(Object o) {
		return o.getClass() == this.getClass() && ((DragonAPIMod) o).getTechnicalName().equalsIgnoreCase(this.getTechnicalName());
	}

	@Override
	public final int hashCode() {
		return ~this.getClass().hashCode() ^ this.getTechnicalName().hashCode();
	}

	public final boolean isReikasMod() {
		return this.getClass().getName().startsWith("reika");
	}

	public Logger getModLogger() {
		throw new MisuseException("You need to set a logger for your mod!");
	}

	public static final class LoadProfiler {
		private final DragonAPIMod mod;
		private final EnumMap<LoadPhase, Boolean> loaded = new EnumMap<>(LoadPhase.class);
		private long time = -1;
		private long total;
		private LoadPhase phase = null;

		private LoadProfiler(DragonAPIMod mod) {
			this.mod = mod;
		}

		private void startTiming(LoadPhase p) {
			if (time != -1)
				throw new IllegalStateException(mod.getTechnicalName() + " is already profiling phase " + phase + "!");
			if (loaded.containsKey(p) && loaded.get(p))
				throw new IllegalStateException(mod.getTechnicalName() + " already finished profiling phase " + phase + "!");
			phase = p;
			time = System.currentTimeMillis();
		}

		private void finishTiming() {
			long duration = System.currentTimeMillis() - time;
			if (time == -1)
				throw new IllegalStateException(mod.getTechnicalName() + " cannot stop profiling before it starts!");
			time = -1;
			String s = ReikaDateHelper.millisToHMSms(duration);
			ReikaJavaLibrary.pConsole(mod.getTechnicalName() + ": Completed loading phase " + phase + " in " + duration + " ms (" + s + ").");
			if (duration > 1800000) { //30 min
				ReikaJavaLibrary.pConsole("Loading time exceeded thirty minutes, indicating very weak hardware. Beware of low framerates.");
			} else if (duration > 300000) { //5 min
				ReikaJavaLibrary.pConsole("Loading time exceeded five minutes, indicating weaker hardware. Consider reducing settings.");
			}
		}

		public enum LoadPhase {
			CONSTRUCT(),
			PRELOAD(),
			LOAD()
		}

	}

}
