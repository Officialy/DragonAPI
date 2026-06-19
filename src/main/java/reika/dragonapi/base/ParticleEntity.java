/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.base;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

public abstract class ParticleEntity extends InertEntity implements IEntityWithComplexSpawn {

	private int oldBlockX;
	private int oldBlockY;
	private int oldBlockZ;

	private WorldLocation spawnLocation;

	private boolean outOfSpawn = false;

	public ParticleEntity(EntityType<? extends Entity> type, Level world) {
		super(type, world);
	}

	public ParticleEntity(EntityType<? extends Entity> type, Level world, BlockPos pos) {
		super(type, world);
		this.spawnAt(pos);
	}

	public ParticleEntity(EntityType<? extends Entity> type, Level world, BlockPos pos, Direction dir) {
		this(type, world, pos);
		this.setDirection(dir, true);
	}

	protected final void spawnAt(BlockPos pos) {
		oldBlockX = pos.getX();
		oldBlockY = pos.getY();
		oldBlockZ = pos.getZ();
		spawnLocation = new WorldLocation(this.level(), pos);
		this.snapTo(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 0, 0);
	}

	@Override
	public final boolean shouldRenderAtSqrDistance(double rsq) {
		return rsq <= this.getRenderRangeSquared();
	}

	public abstract double getRenderRangeSquared();

	public WorldLocation getSpawnLocation() {
		return spawnLocation;
	}

	protected void setDirection(Direction dir, boolean setPos) {
		if (setPos)
			this.snapTo(this.getBlockX()+0.5, this.getBlockY()+0.5, this.getBlockZ()+0.5, 0, 0);
		this.setDeltaMovement(dir.getStepX()*this.getSpeed(), dir.getStepY()*this.getSpeed(), dir.getStepZ()*this.getSpeed());
		this.hurtMarked = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {

	}

	public abstract double getHitboxSize();

	public abstract boolean despawnOverTime();

	public abstract boolean despawnOverDistance();

	protected double getDespawnDistance() {
		return 0;
	}

	public abstract boolean canInteractWithSpawnLocation();

	@Override
	public final void tick() {
		this.baseTick();
		if (this.needsSpeedUpdates()) {
			this.updateSpeed();
		}

		Vec3 mot = this.getDeltaMovement();
		if (this.dieOnNoVelocity() && mot.x == 0 && mot.y == 0 && mot.z == 0 && tickCount > 20) {
			this.discard();
			this.onDeath();
			return;
		}
		if (this.getY() > 1024 || this.getY() < -256) {
			this.discard();
			this.onDeath();
			return;
		}
		if (this.despawnOverTime() && tickCount > 120 && ReikaRandomHelper.doWithChance(tickCount-120)) {
			this.discard();
			this.onDeath();
			return;
		}
		if (this.despawnOverDistance() && spawnLocation != null && spawnLocation.getDistanceTo(this) >= this.getDespawnDistance()) {
			this.discard();
			this.onDeath();
			return;
		}

		if (this.isNewBlock()) {
			int x = this.getBlockX();
			int y = this.getBlockY();
			int z = this.getBlockZ();
			oldBlockX = x;
			oldBlockY = y;
			oldBlockZ = z;
			outOfSpawn = spawnLocation == null || spawnLocation.pos.getX() != x || spawnLocation.pos.getY() != y || spawnLocation.pos.getZ() != z;
			if (!this.canInteractWithSpawnLocation() && !outOfSpawn) {

			}
			else {
				if (this.onEnterBlock(this.level(), new BlockPos(x, y, z))) {
					this.onDeath();
					this.discard();
					return;
				}
			}
		}

		if (!this.level().isClientSide()) {
			double s = this.getHitboxSize();
			AABB box = new AABB(this.getX(), this.getY(), this.getZ(), this.getX(), this.getY(), this.getZ()).inflate(s, s, s);
			List<Entity> inbox = this.level().getEntities(this, box);
			for (Entity e : inbox) {
				this.applyEntityCollision(e);
			}
		}

		this.setPos(this.getX()+mot.x, this.getY()+mot.y, this.getZ()+mot.z);

		this.onTick();
	}

	protected boolean dieOnNoVelocity() {
		return true;
	}

	protected boolean needsSpeedUpdates() {
		return false;
	}

	protected void updateSpeed() {

	}

	protected void onDeath() {

	}

	protected abstract void onTick();

	public abstract double getSpeed();

	/** Returns true if the particle is absorbed */
	protected abstract boolean onEnterBlock(Level world, BlockPos pos);

	public abstract void applyEntityCollision(Entity e);

	public final boolean isNewBlock() {
		int x = this.getBlockX();
		int y = this.getBlockY();
		int z = this.getBlockZ();
		return !this.compareBlocks(x, y, z) && this.isInsideThreshold(x, y, z);
	}

	private boolean isInsideThreshold(int x, int y, int z) {
		double t = this.getBlockThreshold();
		return ReikaMathLibrary.isValueInsideBounds(x+0.5-t, x+0.5+t, this.getX()) && ReikaMathLibrary.isValueInsideBounds(y+0.5-t, y+0.5+t, this.getY()) && ReikaMathLibrary.isValueInsideBounds(z+0.5-t, z+0.5+t, this.getZ());
	}

	protected double getBlockThreshold() {
		return 0.5;
	}

	private boolean compareBlocks(int x, int y, int z) {
		return x == oldBlockX && y == oldBlockY && z == oldBlockZ;
	}

	@Override
	public boolean displayFireAnimation() {
		return false;
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf data) {
		BlockPos p = spawnLocation != null ? spawnLocation.pos : BlockPos.ZERO;
		data.writeInt(p.getX());
		data.writeInt(p.getY());
		data.writeInt(p.getZ());
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf data) {
		int x = data.readInt();
		int y = data.readInt();
		int z = data.readInt();
		spawnLocation = new WorldLocation(this.level(), new BlockPos(x, y, z));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		int x = input.getIntOr("spawnx", 0);
		int y = input.getIntOr("spawny", 0);
		int z = input.getIntOr("spawnz", 0);
		spawnLocation = new WorldLocation(this.level(), new BlockPos(x, y, z));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		if (spawnLocation != null) {
			output.putInt("spawnx", spawnLocation.pos.getX());
			output.putInt("spawny", spawnLocation.pos.getY());
			output.putInt("spawnz", spawnLocation.pos.getZ());
		}
	}

}
