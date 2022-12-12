/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces;


public interface CustomToStringRecipe {

    /**
     * Only suitable for display, as it may vary unpredictably (eg item list ordering). Do NOT use this for map keys.
     */
    String toDisplayString();

    /**
     * This needs to be constant across platforms and game instances! This needs to be keysafe!
     */
    String toDeterministicString();

}
