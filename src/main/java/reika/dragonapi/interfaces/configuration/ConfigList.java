package reika.dragonapi.interfaces.configuration;

/** This is an interface for ENUMS! */
public interface ConfigList {

    Class<?> getPropertyType();

    String getLabel();

    //public boolean isDummiedOut();

    boolean isEnforcingDefaults();

    boolean shouldLoad();

    /** To avoid casting */
    int ordinal();
}
