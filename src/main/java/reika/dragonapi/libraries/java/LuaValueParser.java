package reika.dragonapi.libraries.java;

import java.util.Locale;
import java.util.regex.Pattern;

/** Parses scalar script values without loading a bytecode library or the mod loader. */
public final class LuaValueParser {
    private static final Pattern TYPE_HINT = Pattern.compile("\\[datatype=([^\\]]*)\\]");

    private LuaValueParser() {}

    /**
     * Explicit numeric hints select the boxed type. Invalid hints and numeric overflow
     * retain the historical fallback: integer, long, double, then string.
     * Legacy non-scalar hint names likewise fall back to scalar inference.
     */
    public static Object parse(String value) {
        if (value == null || value.isEmpty())
            return value;
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))
            return Boolean.parseBoolean(value);

        var hint = TYPE_HINT.matcher(value);
        if (hint.find()) {
            String type = hint.group(1).toUpperCase(Locale.ROOT);
            value = value.replace(hint.group(), "");
            try {
                switch (type) {
                    case "BYTE": return Byte.parseByte(value);
                    case "SHORT": return Short.parseShort(value);
                    case "INT": return Integer.parseInt(value);
                    case "LONG": return Long.parseLong(value);
                    case "FLOAT": return Float.parseFloat(value);
                    case "DOUBLE": return Double.parseDouble(value);
                    case "BOOLEAN":
                        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))
                            return Boolean.parseBoolean(value);
                        break;
                    default: break;
                }
            } catch (NumberFormatException ignored) {
                // A failed explicit conversion must preserve the value through inference.
            }
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {}
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}
        return value;
    }
}
