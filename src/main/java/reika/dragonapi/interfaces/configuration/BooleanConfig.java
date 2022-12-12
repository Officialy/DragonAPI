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

public interface BooleanConfig extends ConfigList {

	boolean isBoolean();

	//public boolean setState(Configuration config);

	boolean getState();

	boolean getDefaultState();

}
