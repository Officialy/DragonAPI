package reika.dragonapi.instantiable.math.noise;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collection;

public abstract class NoiseGeneratorBase {

    public final long seed;
    protected final Collection<Octave> octaves = new ArrayList<>();
    /**
     * As opposed to scaling
     */
    public boolean clampEdge = false;
    protected double inputFactor = 1;
    protected double maxRange = 1;
    private NoiseGeneratorBase xNoise;
    private NoiseGeneratorBase yNoise;
    private NoiseGeneratorBase zNoise;
    private double xNoiseScale;
    private double yNoiseScale;
    private double zNoiseScale;

    protected NoiseGeneratorBase(long s) {
        seed = s;
    }

    public final double getValue(double x, double z) {
        return this.calculateValues(x * inputFactor, 0, z * inputFactor);
    }

    public final double getValue(double x, double y, double z) {
        return this.calculateValues(x * inputFactor, y * inputFactor, z * inputFactor);
    }

    private double calculateValues(double x, double y, double z) {
        x += this.getXDisplacement(x, y, z);
        y += this.getYDisplacement(x, y, z);
        z += this.getZDisplacement(x, y, z);

        double val = this.calcValue(x, y, z, 1, 1);

        if (!octaves.isEmpty()) {
            for (Octave o : octaves) {
                val += this.calcValue(x + o.phaseShift, y + o.phaseShift, z + o.phaseShift, o.frequency, o.amplitude);
            }
            if (clampEdge)
                val = Mth.clamp(val, -1, 1);
            else
                val /= maxRange;
        }

        return val;
    }

    protected abstract double calcValue(double x, double y, double z, double freq, double amp);

    public final NoiseGeneratorBase setFrequency(double f) {
        inputFactor = f;
        return this;
    }

    public final double getFrequencyScale() {
        return inputFactor;
    }

    public final NoiseGeneratorBase addOctave(double relativeFrequency, double relativeAmplitude) {
        return this.addOctave(relativeFrequency, relativeAmplitude, 0);
    }

    public final NoiseGeneratorBase addOctave(double relativeFrequency, double relativeAmplitude, double phaseShift) {
        octaves.add(new Octave(relativeFrequency, relativeAmplitude, phaseShift));
        maxRange += relativeAmplitude;
        return this;
    }

    public final NoiseGeneratorBase setDisplacementSimple(long seedX, double fx, long seedZ, double fz, double s) {
        return this.setDisplacement(new SimplexNoiseGenerator(seedX).setFrequency(fx), s, null, s, new SimplexNoiseGenerator(seedZ).setFrequency(fz), s);
    }

    public final NoiseGeneratorBase setDisplacementSimple(long seedX, double fx, long seedY, double fy, long seedZ, double fz, double s) {
        return this.setDisplacement(new SimplexNoiseGenerator(seedX).setFrequency(fx), s, new SimplexNoiseGenerator(seedY).setFrequency(fy), s, new SimplexNoiseGenerator(seedZ).setFrequency(fz), s);
    }

    public final NoiseGeneratorBase setDisplacement(NoiseGeneratorBase x, NoiseGeneratorBase y, NoiseGeneratorBase z, double s) {
        return this.setDisplacement(x, s, y, s, z, s);
    }

    public final NoiseGeneratorBase setDisplacement(NoiseGeneratorBase x, double xs, NoiseGeneratorBase y, double ys, NoiseGeneratorBase z, double zs) {
        xNoise = x;
        yNoise = y;
        zNoise = z;
        xNoiseScale = xs;
        yNoiseScale = ys;
        zNoiseScale = zs;
        return this;
    }

    public final double getXDisplacement(double x, double y, double z) {
        return xNoise != null ? xNoise.getValue(x, y, z) * xNoiseScale : 0;
    }

    public final double getYDisplacement(double x, double y, double z) {
        return yNoise != null ? yNoise.getValue(x, y, z) * yNoiseScale : 0;
    }

    public final double getZDisplacement(double x, double y, double z) {
        return zNoise != null ? zNoise.getValue(x, y, z) * zNoiseScale : 0;
    }
}
