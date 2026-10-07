/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.extras;

import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.libraries.java.ReikaStringParser;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import net.neoforged.fml.ModList;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipFile;

import static net.neoforged.fml.loading.FMLEnvironment.isProduction;

public class ModVersion implements Comparable<ModVersion> {

	public static final ModVersion source = new ModVersion(0) {
		@Override
		public boolean equals(Object o) {
			return o == this;
		}

		@Override
		public String toString() {
			return "Source Code";
		}

		@Override
		public boolean isCompiled() {
			return false;
		}

		@Override
		public boolean verify() {
			return false;
		}
	};
	public static final ModVersion timeout = new ModVersion(-1) {
		@Override
		public boolean equals(Object o) {
			return o == this;
		}

		@Override
		public String toString() {
			return "[URL TIMEOUT]";
		}

		@Override
		public boolean verify() {
			return false;
		}
	};
	private static final ModVersion error = new ModVersion(1) {
		@Override
		public boolean equals(Object o) {
			return o == this;
		}

		@Override
		public String toString() {
			return "[NO FILE]";
		}

		@Override
		public boolean verify() {
			return false;
		}
	};
	public final int majorVersion;
	public final String subVersion;
	private final String artifactVersion;

	private ModVersion(int major) {
		this(major, '\0');
	}

	private ModVersion(int major, char minor) {
		artifactVersion = null;
		majorVersion = major;
		subVersion = minor == '\0' ? "" : Character.toString(minor).toLowerCase(Locale.ENGLISH);
	}

	private ModVersion(String version) {
		artifactVersion = version;
		String[] numbers = version.split("[.\\-+]", 3);
		majorVersion = Integer.parseInt(numbers[0]);
		subVersion = numbers[1];
	}

	public static ModVersion getFromString(String s) {
		if (s == null || s.isBlank())
			return error;
		s = s.trim();
		if (s.equals("Source Code") || s.startsWith("$") || s.startsWith("@"))
			return source;
		if (s.contains("URL TIMEOUT"))
			return timeout;
		if (s.startsWith("v") || s.startsWith("V"))
			s = s.substring(1);
		if (s.isEmpty())
			return error;
		try {
			if (s.matches("[0-9]+\\.[0-9]+(?:\\.[0-9]+)*(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?"))
				return new ModVersion(s);
			char c = s.charAt(s.length() - 1);
			if (Character.isDigit(c))
				return new ModVersion(Integer.parseInt(s));
			String major = s.substring(0, s.length() - 1);
			return major.isEmpty() || !Character.isLetter(c) ? error : new ModVersion(Integer.parseInt(major), c);
		} catch (NumberFormatException e) {
			return error;
		}
	}

	/**
	 * Not for setting ModContainer data; only use this during construction to pass to FML @Mod!
	 */
	public static ModVersion readFromJar(ZipFile jar, String innerName) {
		Properties p = new Properties();
		String path = ReikaStringParser.stripSpaces("version_" + ReikaStringParser.stripSpaces(innerName + ".properties"));
		try {
			var entry = jar.getEntry(path);
			if (entry == null) {
				return ModVersion.error;
			}
			try (InputStream stream = jar.getInputStream(entry)) {
				p.load(stream);
			}
			String mj = p.getProperty("Major");
			String mn = p.getProperty("Minor");
			if (mj == null || mn == null || mj.equals("null") || mn.equals("null") || mj.isEmpty() || mn.isEmpty())
				return ModVersion.error;
			return getFromString(mj + mn);
		} catch (IOException e) {
			e.printStackTrace();
			return ModVersion.error;
		}
	}

	public static ModVersion readFromFile(DragonAPIMod mod) {
		// NeoForge metadata describes the artifact actually loaded, including patch and prerelease versions.
		var container = ModList.get().getModContainerById(mod.getModId());
		if (container.isPresent())
			return getFromString(container.get().getModInfo().getVersion().toString());
		return isProduction() ? error : source;
	}

	public static ModVersion fromSemanticVersion(String s) {
		return getFromString(s);
	}

	@Override
	public int hashCode() {
		return !verify() ? System.identityHashCode(this) : java.util.Objects.hash(majorVersion, subVersion, artifactVersion);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ModVersion m) {
			return verify() && m.verify() && m.majorVersion == majorVersion && m.subVersion.equals(subVersion) && java.util.Objects.equals(m.artifactVersion, artifactVersion);
		}
		return false;
	}

	public boolean isCompiled() {
		return true;
	}

	public boolean verify() {
		return true;
	}

	@Override
	public String toString() {
		return artifactVersion != null ? artifactVersion : "v" + majorVersion + subVersion;
	}

	private int getSubVersionIndex() {
		return subVersion == null || subVersion.isEmpty() ? 0 : subVersion.charAt(0) - 'a';
	}

	@Override
	public int compareTo(ModVersion v) {
		if (artifactVersion != null || v.artifactVersion != null)
			return new DefaultArtifactVersion(toSemanticVersion()).compareTo(new DefaultArtifactVersion(v.toSemanticVersion()));
		int major = Integer.compare(majorVersion, v.majorVersion);
		return major != 0 ? major : Integer.compare(getSubVersionIndex(), v.getSubVersionIndex());
	}

	public boolean isNewerMinorVersion(ModVersion v) {
		return v.majorVersion == majorVersion && compareTo(v) > 0;
	}

	public String toSemanticVersion() {
		return artifactVersion != null ? artifactVersion : majorVersion + "." + (subVersion.isEmpty() ? 0 : 1 + getSubVersionIndex());
	}
}
