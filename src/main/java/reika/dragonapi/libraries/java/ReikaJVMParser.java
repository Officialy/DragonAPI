package reika.dragonapi.libraries.java;

import reika.dragonapi.client.ClientEnvironment;
import reika.dragonapi.DragonAPI;
import org.jspecify.annotations.Nullable;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class ReikaJVMParser {

    private static final Set<String> args = Collections.unmodifiableSet(
            new LinkedHashSet<>(ManagementFactory.getRuntimeMXBean().getInputArguments()));

    private static final int[] version = getJavaVersion();

    public static Set<String> getAllArguments() {
        return args;
    }

    public static boolean isArgumentPresent(String arg) {
        return args.contains(arg);
    }

    public static @Nullable String getArgumentModifier(String pre) {
        for (String s : args) {
            if (s.startsWith(pre)) {
                return s.substring(pre.length());
            }
        }
        return null;
    }

    public static int getArgumentInteger(String pre) {
        for (String s : args) {
            if (s.startsWith(pre)) {
                int idx = s.indexOf('=');
                if (idx < 0 || idx == s.length() - 1)
                    return -1;
                return ReikaJavaLibrary.safeIntParse(s.substring(idx + 1));
            }
        }
        return -1;
    }

    public static long getAllocatedHeapMemory() {
        for (String s : args) {
            if (s.startsWith("-Xmx")) {
                String ret = s.substring(4);
                if (ret.isEmpty())
                    continue;
                char size = ret.charAt(ret.length()-1);
                if (Character.isLowerCase(size))
                    size = Character.toUpperCase(size);
                long multiplier;
                if (size == 'K') {
                    multiplier = 1024L;
                }
                else if (size == 'M') {
                    multiplier = 1024L * 1024L;
                }
                else if (size == 'G') {
                    multiplier = 1024L * 1024L * 1024L;
                }
                else {
                    if (!Character.isDigit(size))
                        throw new IllegalArgumentException("Invalid memory specification: " + s);
                    multiplier = 1;
                }
                String number = multiplier == 1 ? ret : ret.substring(0, ret.length() - 1);
                return Long.parseLong(number) * multiplier;
            }
        }
        return Runtime.getRuntime().maxMemory();
    }

    static {
        ReikaJavaLibrary.pConsole("Java Version: "+ Arrays.toString(version));
        ReikaJavaLibrary.pConsole("Heap memory allocation: "+getAllocatedHeapMemory());
        ReikaJavaLibrary.pConsole(args.size()+" Java arguments present: "+args);
    }

    private static int[] getJavaVersion() {
        try {
            return Runtime.version().version().stream().mapToInt(Integer::intValue).toArray();
        }
        catch (RuntimeException error) {
            DragonAPI.LOGGER.error("Could not read runtime version components for {}", getFullJavaInfo(), error);
            return new int[] {-1, -1, -1};
        }
    }

    /** Zero-based Runtime.version() components: feature, interim, update and patch, when present. */
    public static int getJavaVersion(int subindex) {
        if (subindex < 0 || subindex >= version.length)
            throw new IllegalArgumentException("Java version component " + subindex + " is unavailable");
        return version[subindex];
    }

    public static String getFullJavaInfo() {
        return System.getProperty("java.version")+" "+System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.info") + "), " + System.getProperty("java.vm.vendor");
    }

    public static String getLauncher() {
        return FMLEnvironment.getDist() == Dist.CLIENT ? getLauncherClient() : "Server";
    }

    public static String getLauncherClient() {
        try {
            return ClientEnvironment.launchedVersion();
        }
        catch (Exception error) {
            DragonAPI.LOGGER.error("Could not read the client launcher version", error);
            return error.toString();
        }
    }
}



