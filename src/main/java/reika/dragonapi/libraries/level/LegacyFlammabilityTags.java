package reika.dragonapi.libraries.level;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import reika.dragonapi.DragonAPI;

/** Original catalogue exceptions to vanilla fire data; this does not change fire spread odds. */
public final class LegacyFlammabilityTags {
    public static final TagKey<Block> FLAMMABLE = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(DragonAPI.MODID, "legacy_flammable"));

    private LegacyFlammabilityTags() {}
}
