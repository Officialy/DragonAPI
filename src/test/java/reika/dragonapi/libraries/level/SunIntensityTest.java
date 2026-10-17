package reika.dragonapi.libraries.level;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SunIntensityTest {
    @Test void nativeDegreeAnglesRetainTheOriginalNormalizedCurveAcrossWholeCycles() {
        for (int degrees = -720; degrees <= 720; degrees++) {
            float expected = (float)Math.clamp(Math.cos(Math.toRadians(degrees)) * 2 + 0.2, 0, 1);
            float actual = ReikaWorldHelper.getSunIntensity(degrees, 0, 0);
            assertEquals(expected, actual, 0.0003, "angle=" + degrees);
            assertTrue(actual >= 0 && actual <= 1);
        }
        assertEquals(1, ReikaWorldHelper.getSunIntensity(0, 0, 0));
        assertEquals(0, ReikaWorldHelper.getSunIntensity(180, 0, 0));
        assertEquals(0.2F, ReikaWorldHelper.getSunIntensity(90, 0, 0), 0.0001);
    }

    @Test void weatherUsesOriginalRainAndWeightedThunderFactorsWithoutAnAmbientFloor() {
        for (float rain : new float[]{0, 0.25F, 0.5F, 1}) {
            for (float thunder : new float[]{0, 0.25F, 0.5F, 1}) {
                float expected = (1 - rain * 5F / 16F) * (1 - thunder * 5F / 16F);
                assertEquals(expected, ReikaWorldHelper.getSunIntensity(0, rain, thunder), 0.000001);
                assertEquals(0, ReikaWorldHelper.getSunIntensity(180, rain, thunder));
            }
        }
        assertEquals(0.6875F, ReikaWorldHelper.getSunIntensity(0, 1, 0));
        assertEquals(0.47265625F, ReikaWorldHelper.getSunIntensity(0, 1, 1));
    }
}
