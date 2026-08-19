package reika.dragonapi.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Opens {@link Camera}'s position and rotation setters, which are protected and which NeoForge does not
 * widen.
 *
 * <p>The camera can be turned from a public API — {@code ViewportEvent.ComputeCameraAngles} hands out
 * yaw, pitch and roll — but it cannot be <em>moved</em>. Anything that wants the camera somewhere other
 * than on the player, such as a scripted flythrough, has no other route: turning the player instead is
 * not the same thing, because that moves a real entity and takes its collision and reach with it.
 *
 * <p>An accessor rather than an access transformer deliberately. The AT in this mod still carries SRG
 * field names from an older mappings setup, so whether it currently applies is not something to build a
 * feature on; the two mixins beside this one demonstrably load.
 *
 * <p>Whoever moves the camera owns putting it back. Nothing here does that for you.
 */
@Mixin(Camera.class)
public interface CameraAccessor {

	@Invoker("setPosition")
	void dragonapi$setPosition(Vec3 position);

	/** The three-argument form NeoForge adds; the two-argument one is deprecated in its patch. */
	@Invoker("setRotation")
	void dragonapi$setRotation(float yRot, float xRot, float roll);
}
