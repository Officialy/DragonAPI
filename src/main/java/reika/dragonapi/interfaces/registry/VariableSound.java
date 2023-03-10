package reika.dragonapi.interfaces.registry;


import reika.dragonapi.instantiable.io.SoundVariant;

import java.util.Collection;

public interface VariableSound extends SoundEnum {

	Collection<SoundVariant<?>> getVariants();

	SoundVariant<?> getVariant(String name);

}
