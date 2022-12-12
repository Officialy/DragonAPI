package reika.dragonapi.interfaces.registry;

import net.minecraft.world.item.ItemStack;

/** This is an interface for ENUMS! */
public interface RegistrationList extends RegistryEntry {

    Class<?>[] getConstructorParamTypes();

    Object[] getConstructorParams();

    ItemStack get();

}
