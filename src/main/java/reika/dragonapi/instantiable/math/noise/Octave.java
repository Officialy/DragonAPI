package reika.dragonapi.instantiable.math.noise;

class Octave {

    protected final double frequency;
    protected final double amplitude;
    protected final double phaseShift;

    Octave(double f, double a, double p) {
        amplitude = a;
        frequency = f;
        phaseShift = p;
    }

}
