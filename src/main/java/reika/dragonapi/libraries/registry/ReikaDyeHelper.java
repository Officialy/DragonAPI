package reika.dragonapi.libraries.registry;

import net.minecraft.world.item.DyeColor;
import reika.dragonapi.DragonAPI;

/**
 * Helper enum/class for the 16 vanilla dye colors.
 * Each constant carries its 24-bit RGB and matching DyeColor for cross-API use.
 */
public enum ReikaDyeHelper {
    BLACK(0x191919, DyeColor.BLACK),
    RED(0x993333, DyeColor.RED),
    GREEN(0x667F33, DyeColor.GREEN),
    BROWN(0x664C33, DyeColor.BROWN),
    BLUE(0x334CB2, DyeColor.BLUE),
    PURPLE(0x7F3FB2, DyeColor.PURPLE),
    CYAN(0x4C7F99, DyeColor.CYAN),
    LIGHTGRAY(0x999999, DyeColor.LIGHT_GRAY),
    GRAY(0x4C4C4C, DyeColor.GRAY),
    PINK(0xF27FA5, DyeColor.PINK),
    LIME(0x7FCC19, DyeColor.LIME),
    YELLOW(0xE5E533, DyeColor.YELLOW),
    LIGHTBLUE(0x6699D8, DyeColor.LIGHT_BLUE),
    MAGENTA(0xB24CD8, DyeColor.MAGENTA),
    ORANGE(0xD87F33, DyeColor.ORANGE),
    WHITE(0xFFFFFF, DyeColor.WHITE);

    public static final ReikaDyeHelper[] dyes = values();

    public final int color;
    public final DyeColor dye;

    ReikaDyeHelper(int rgb, DyeColor dye) {
        this.color = rgb;
        this.dye = dye;
    }

    public int getColor() {
        return color;
    }

    public DyeColor getDye() {
        return dye;
    }

    public static ReikaDyeHelper getFromDye(DyeColor dye) {
        for (ReikaDyeHelper r : dyes) {
            if (r.dye == dye) return r;
        }
        return null;
    }

    /** Stub for legacy startup wiring; left empty until/unless dye-aware caching is needed. */
    public static void buildItemCache() {
        DragonAPI.LOGGER.debug("Building dye item cache...");
    }
}
