package reika.dragonapi.interfaces.registry;

import reika.dragonapi.base.StructureBase;

/** Implemented by a mod's structure-registry enum; each entry supplies a {@link StructureBase}. */
public interface StructureEnum<V extends StructureBase> {

	V getStructure();

	/** ie spawns naturally, with worldgen, as opposed to being built by the player */
    boolean isNatural();

}
