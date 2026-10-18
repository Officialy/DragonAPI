/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.event.client;


import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.Event;


public class BossColorEvent extends Event {
    public static final Color DEFAULT = new Color(0.7F, 0.6F, 0.6F);
    public final float originalRed = DEFAULT.red();
    public final float originalGreen = DEFAULT.green();
    public final float originalBlue = DEFAULT.blue();
    public final boolean isLightmap;
    public float red = originalRed;
    public float green = originalGreen;
    public float blue = originalBlue;

    public BossColorEvent(boolean light) {
        isLightmap = light;
    }

    /** One immutable result belongs to this dispatch, including nested or concurrent dispatches. */
    public static Color fire(boolean lightmap) {
        BossColorEvent event = new BossColorEvent(lightmap);
        NeoForge.EVENT_BUS.post(event);
        return new Color(event.red, event.green, event.blue);
    }

    public record Color(float red, float green, float blue) {}

    /** Original per-renderer-tick listener cleanup notification; no shared output state remains. */
    public static void reset() {
        NeoForge.EVENT_BUS.post(new BossColorResetEvent());
    }

    public static class BossColorResetEvent extends Event {}
}
