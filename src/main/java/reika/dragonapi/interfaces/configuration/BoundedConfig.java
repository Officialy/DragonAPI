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

<<<<<<< Updated upstream:Interfaces/Configuration/BoundedConfig.java
=======
import net.neoforged.common.ForgeConfigSpec;
import reika.dragonapi.instantiable.io.oldforge.Property;
>>>>>>> Stashed changes:src/main/java/reika/dragonapi/interfaces/configuration/BoundedConfig.java

public interface BoundedConfig extends ConfigList {

	boolean isValueValid(Property p);

	String getBoundsAsString();

}
