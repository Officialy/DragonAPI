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

public interface StringArrayConfig extends ConfigList {

	boolean isStringArray();

	String[] getStringArray();

	String[] getDefaultStringArray();

}
