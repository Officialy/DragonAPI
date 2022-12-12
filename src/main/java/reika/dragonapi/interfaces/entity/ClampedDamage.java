package reika.dragonapi.interfaces.entity;

import net.minecraft.world.damagesource.DamageSource;

/**
 * For entities that have damage caps per hit.
 */
public interface ClampedDamage {

    float getDamageCap(DamageSource src, float dmg);

}
