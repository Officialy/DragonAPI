/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.registry;

public interface RegistryEntry {

    String getBasicName();

    /** Whether to create it or not */
    boolean isDummiedOut();

    /** To avoid casting to Enum */
    int ordinal();

    /** To avoid casting to Enum */
    String name();

    Class<?> getObjectClass();

    String getUnlocalizedName();
}
