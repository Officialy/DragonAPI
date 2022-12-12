package reika.dragonapi.auxiliary.trackers;

import reika.dragonapi.instantiable.math.noise.SimplexNoiseGenerator;

import java.util.Calendar;

public class SpecialDayTracker {

    public static final SpecialDayTracker instance = new SpecialDayTracker();

    private final Calendar calendar;

    private final SimplexNoiseGenerator weatherNoise = new SimplexNoiseGenerator(System.currentTimeMillis());

    private SpecialDayTracker() {
        calendar = Calendar.getInstance();
    }

    public boolean loadAprilTextures() {
        return calendar.get(Calendar.MONTH) == Calendar.APRIL && calendar.get(Calendar.DAY_OF_MONTH) <= 2;
    }

    public boolean loadXmasTextures() {
        return (calendar.get(Calendar.MONTH) == Calendar.DECEMBER && calendar.get(Calendar.DAY_OF_MONTH) >= 18) || (calendar.get(Calendar.MONTH) == Calendar.JANUARY && calendar.get(Calendar.DAY_OF_MONTH) <= 5);
    }

    /*
        
        public float getXmasWeatherStrength(Level world) {
            if (!this.isWinterEnabled())
                return 0;
            if (world.provider.hasNoSky || PlanetDimensionHandler.isOtherWorld(world))
                return 0;
            double val = weatherNoise.getValue(world.getGameTime()/6000D, world.provider.dimensionId*200);
            float norm = (float)Math.sqrt(ReikaMathLibrary.normalizeToBounds(val, 0, 1));

            Player ep = Minecraft.getInstance().player;
            Biome biome = world.getBiomeGenForCoords(Mth.floor(ep.posX), Mth.floor(ep.posZ));
            if (biome instanceof WinterBiomeStrengthControl)
                norm *= ((WinterBiomeStrengthControl)biome).getWinterSkyStrength(world, ep);

            return Mth.clamp(norm*1.1F, 0, 1);
        }
    */
    public boolean isWinterEnabled() {
        return this.loadXmasTextures();
    }

    public boolean isHalloween() {
        return calendar.get(Calendar.MONTH) == Calendar.OCTOBER && calendar.get(Calendar.DAY_OF_MONTH) >= 30;
    }

}
