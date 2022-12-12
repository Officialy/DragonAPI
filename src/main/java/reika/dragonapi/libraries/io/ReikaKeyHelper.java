/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.libraries.io;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

//We are assuming default bindings for now
public class ReikaKeyHelper {

    public static int getForwardKey() {
        return Minecraft.getInstance().options.keyUp.getKey().getValue();
        //return InputConstants.KEY_W;
    }

    public static int getJumpKey() {
        return Minecraft.getInstance().options.keyJump.getKey().getValue();
        //return InputConstants.KEY_SPACE;
    }

    public static int getSneakKey() {
        return Minecraft.getInstance().options.keyShift.getKey().getValue();
        //return InputConstants.KEY_LSHIFT;
    }

    public static boolean isKeyPressed(int key) {
        return InputConstants.isKeyDown(1, key);
    }

}
