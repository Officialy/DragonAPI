/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.configuration;

import net.minecraftforge.common.ForgeConfigSpec;
import reika.dragonapi.instantiable.io.oldforge.Property;

public interface BoundedConfig extends ConfigList {

	boolean isValueValid(Property p);

	String getBoundsAsString();

}
