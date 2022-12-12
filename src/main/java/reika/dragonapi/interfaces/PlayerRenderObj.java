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

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;



public interface PlayerRenderObj {

    /**
     * Render starts centered on eye position
     */
//    void render(PoseStack stack, Player ep, float ptick, PlayerSpecificRenderer.PlayerRotationData data);

    /**
     * Lower numbers render first. Use high numbers (>> 0) for transparency
     */
    int getRenderPriority();

}
