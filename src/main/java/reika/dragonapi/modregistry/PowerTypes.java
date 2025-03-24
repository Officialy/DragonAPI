/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.modregistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagManager;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.ModList;
import reika.dragonapi.interfaces.registry.Dependency;

public enum PowerTypes implements Dependency {

    RF(),
    FE(),
//    EU("ic2.api.energy.tile.IEnergyTile", "ic2.api.item.IElectricItem"),
    ROTARYCRAFT("reika.rotarycraft.api.power.ShaftMachine"),
    PNEUMATIC("pneumaticCraft.api.blockentity.IPneumaticMachine"),
    HYDRAULIC(),
    STEAM(TagManager.getTagDir(FluidTags.create(ResourceLocation.fromNamespaceAndPath("forge", "steam")).registry()).isEmpty()),
    ELECTRICRAFT(ModList.ELECTRICRAFT.isLoaded());

    private final boolean exists;

    PowerTypes(boolean f) {
        exists = f;

        DragonAPI.LOGGER.debug("Power type " + this + " loaded: " + f);
    }

    PowerTypes(String... cl) {
        this(cl != null && cl.length > 0 && checkAllClasses(cl));
    }

    private static boolean checkAllClasses(String... cl) {
        for (String s : cl) {
            try {
                Class<?> c = Class.forName(s);
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isLoaded() {
        return exists;
    }

    @Override
    public String getDisplayName() {
        return this.name();
    }
}
