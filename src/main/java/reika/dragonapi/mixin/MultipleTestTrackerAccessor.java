package reika.dragonapi.mixin;

import java.util.Collection;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.MultipleTestTracker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultipleTestTracker.class)
public interface MultipleTestTrackerAccessor {

    @Accessor("tests")
    Collection<GameTestInfo> dragonapi$getTests();
}
