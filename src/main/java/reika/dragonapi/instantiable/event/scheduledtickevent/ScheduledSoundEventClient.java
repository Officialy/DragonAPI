/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.event.scheduledtickevent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.fml.LogicalSide;

import reika.dragonapi.interfaces.registry.SoundEnum;
import reika.dragonapi.libraries.io.ReikaSoundHelper;

public class ScheduledSoundEventClient implements ScheduledEvent {

	private final SoundEnum sound;
	private final float volume;
	private final float pitch;

	private Level world;
	private double posX;
	private double posY;
	private double posZ;

	private Entity entity;

	public boolean attenuate = true;

	private ScheduledSoundEventClient(SoundEnum s, float v, float p) {
		sound = s;
		volume = v;
		pitch = p;
	}

	public ScheduledSoundEventClient(SoundEnum s, Entity e, float v, float p) {
		this(s, v, p);
		entity = e;
	}

	public ScheduledSoundEventClient(SoundEnum s, Level w, double x, double y, double z, float v, float p) {
		this(s, v, p);
		world = w;
		posX = x;
		posY = y;
		posZ = z;
	}

	@Override
	public void fire() {
		ReikaSoundHelper.playClientSound(sound, this.getX(), this.getY(), this.getZ(), volume, pitch, attenuate);
	}

	protected Entity getEntity() {
		return entity;
	}

	private double getX() {
		return entity != null ? entity.getX() : posX;
	}

	private double getY() {
		return entity != null ? entity.getY() : posY;
	}

	private double getZ() {
		return entity != null ? entity.getZ() : posZ;
	}

	@Override
	public boolean runOnSide(LogicalSide s) {
		return s == LogicalSide.CLIENT;
	}

}
