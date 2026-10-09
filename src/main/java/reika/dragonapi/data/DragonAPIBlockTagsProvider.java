package reika.dragonapi.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.level.LegacyFlammabilityTags;

public final class DragonAPIBlockTagsProvider extends BlockTagsProvider {
    public DragonAPIBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, DragonAPI.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // The other entries in the 1.7.10 catalogue already have native 26.3 fire odds.
        var flammable = tag(LegacyFlammabilityTags.FLAMMABLE);
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.NOTE_BLOCK, Blocks.JUKEBOX})
            flammable.add(BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow());
    }
}
