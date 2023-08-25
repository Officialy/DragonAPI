package reika.dragonapi.extras;


public class BlockFlags {
    public static final int NOTIFY_NEIGHBORS = 1;
    public static final int BLOCK_UPDATE = 2;
    public static final int NO_RERENDER = 4; //todo figure out what this does
    public static final int RERENDER_MAIN_THREAD = 8;
    public static final int UPDATE_NEIGHBORS = 16;
    public static final int NO_NEIGHBOR_DROPS = 32;
    public static final int ALL = 3;

}