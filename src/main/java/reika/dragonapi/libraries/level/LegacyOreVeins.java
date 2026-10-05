package reika.dragonapi.libraries.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The original WorldGenMinable ellipsoids, with modern base-stone and ore-equivalence tags. */
public final class LegacyOreVeins {
    private LegacyOreVeins() {}

    public static boolean hasEquivalent(Block own, String material, Block... otherOwnVariants) {
        var tag = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/" + material));
        return BuiltInRegistries.BLOCK.get(tag).stream().flatMap(set -> set.stream()).anyMatch(holder -> {
            if (holder.value() == own) return false;
            // A camouflage variant converts an existing deposit; it cannot supply replacement generation.
            if (BuiltInRegistries.BLOCK.getKey(holder.value()).getNamespace().equals("geostrata")) return false;
            for (Block variant : otherOwnVariants) if (holder.value() == variant) return false;
            return true;
        });
    }

    public static boolean adjacentLava(LevelAccessor world, BlockPos pos) {
        for (Direction direction : Direction.values())
            if (world.getBlockState(pos.relative(direction)).is(Blocks.LAVA)) return true;
        return false;
    }

    public static boolean place(LevelAccessor world, RandomSource random, BlockState ore,
                                int size, int dimensionType, int x, int y, int z) {
        if (size < 1) throw new IllegalArgumentException("Vein size must be positive");
        float angle = random.nextFloat() * (float)Math.PI;
        double x1 = x + 8 + Mth.sin(angle) * size / 8F, x2 = x + 8 - Mth.sin(angle) * size / 8F;
        double z1 = z + 8 + Mth.cos(angle) * size / 8F, z2 = z + 8 - Mth.cos(angle) * size / 8F;
        double y1 = y + random.nextInt(3) - 2, y2 = y + random.nextInt(3) - 2;
        boolean placed = false;
        for (int step = 0; step <= size; step++) {
            double cx = x1 + (x2-x1)*step/size, cy = y1 + (y2-y1)*step/size, cz = z1 + (z2-z1)*step/size;
            double spread = random.nextDouble()*size/16D;
            double width = (Mth.sin(step*(float)Math.PI/size)+1F)*spread+1D;
            double height = (Mth.sin(step*(float)Math.PI/size)+1F)*spread+1D;
            for (int dx = Mth.floor(cx-width/2); dx <= Mth.floor(cx+width/2); dx++) {
                double nx = (dx+.5-cx)/(width/2);
                if (nx*nx >= 1) continue;
                for (int dy = Mth.floor(cy-height/2); dy <= Mth.floor(cy+height/2); dy++) {
                    double ny = (dy+.5-cy)/(height/2);
                    if (nx*nx+ny*ny >= 1) continue;
                    for (int dz = Mth.floor(cz-width/2); dz <= Mth.floor(cz+width/2); dz++) {
                        double nz = (dz+.5-cz)/(width/2);
                        if (nx*nx+ny*ny+nz*nz >= 1) continue;
                        BlockPos pos = new BlockPos(dx,dy,dz);
                        if (!world.isInsideBuildHeight(dy)) continue;
                        BlockState host = world.getBlockState(pos);
                        boolean replace = switch (dimensionType) {
                            case 1 -> host.is(BlockTags.BASE_STONE_NETHER);
                            case 2 -> host.is(Blocks.END_STONE);
                            default -> host.is(BlockTags.STONE_ORE_REPLACEABLES) || host.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
                        };
                        if (replace) placed |= world.setBlock(pos,ore,2);
                    }
                }
            }
        }
        return placed;
    }
}
