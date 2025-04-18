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

public interface IntegerConfig extends ConfigList {

	boolean isNumeric();

	//public int setValue(Configuration config);

	int getValue();

	int getDefaultValue();

}
