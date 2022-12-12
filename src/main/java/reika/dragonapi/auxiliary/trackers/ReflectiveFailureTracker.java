/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.auxiliary.trackers;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.data.maps.MultiMap;
import reika.dragonapi.interfaces.registry.ModEntry;

import java.util.Collection;

public class ReflectiveFailureTracker {

	public static final ReflectiveFailureTracker instance = new ReflectiveFailureTracker();

	private final MultiMap<ModEntry, Object> data = new MultiMap<>();

	private ReflectiveFailureTracker() {

	}

	public void logModReflectiveFailure(ModEntry mod, Exception e) {
		data.addValue(mod, new ExceptionLog(this.getClassname(), e));
	}

	public void logModReflectiveFailure(ModEntry mod, String e) {
		data.addValue(mod, new StringLog(this.getClassname(), e));
	}

	private String getClassname() {
		StringBuilder sb = new StringBuilder();
		StackTraceElement[] tr = Thread.currentThread().getStackTrace();
		//0 is Thread, 1 is ReflectiveFailureTracker, 2&3 are the class
		for (int i = 2; i <= 3; i++) {
			if (i < tr.length) {
				sb.append(tr[i].getClassName());
				if (i < tr.length - 1 && i < 3) {
					sb.append(" / ");
				}
			}
		}
		return sb.toString();
	}

	public void print() {
		if (!data.isEmpty()) {
			DragonAPI.LOGGER.info("===============================================================================================");

			this.log("Some reflective mod handlers have failed.");
			this.log("Please try updating all involved mods, and if this fails to fix the issue, notify the author of the handlers.");

			for (ModEntry mod : data.keySet()) {
				Collection<Object> c = data.get(mod);
				this.log(String.format("%d failure%s for %s ('%s'):", c.size(), c.size() > 1 ? "s" : "", mod.getDisplayName(), mod.getModid()));
				for (Object e : c) {
					this.log(e.toString());
				}
				DragonAPI.LOGGER.info("");
			}


			this.log("For further information, including full stacktraces, consult the loading logs, and search for the erroring classes' names.");
			DragonAPI.LOGGER.info("===============================================================================================");
		}
	}

	private void log(String s) {
		DragonAPI.LOGGER.error(s);
	}

	private static class StringLog {

		private final String erroredClass;
		private final String error;

		private StringLog(String c, String e) {
			error = e;
			erroredClass = c;
		}

		@Override
		public final String toString() {
			return error.getClass().getSimpleName() + " \"" + error;
		}

	}

	private static class ExceptionLog {

		private final String erroredClass;
		private final Exception error;

		private ExceptionLog(String c, Exception e) {
			error = e;
			erroredClass = c;
		}

		@Override
		public final String toString() {
			return error.getClass().getSimpleName() + " \"" + error.getMessage() + "\" thrown from " + erroredClass;
		}

	}

}
