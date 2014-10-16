/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2014
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.DragonAPI.Libraries;

import java.util.HashMap;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import Reika.DragonAPI.APIPacketHandler.PacketIDs;
import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.DragonAPIInit;
import Reika.DragonAPI.Base.ScheduledTickEvent;
import Reika.DragonAPI.Instantiable.Data.BlockArray;
import Reika.DragonAPI.Libraries.IO.ReikaPacketHelper;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ReikaPlayerAPI extends DragonAPICore {

	private static final HashMap<String, FakePlayer> fakePlayers = new HashMap();

	/** Transfers a player's entire inventory to an inventory. Args: Player, Inventory */
	public static void transferInventoryToChest(EntityPlayer ep, ItemStack[] inv) {
		int num = ReikaInventoryHelper.getTotalUniqueStacks(ep.inventory.mainInventory);
		if (num >= inv.length)
			return;
	}

	/** Clears a player's hotbar. Args: Player */
	public static void clearHotbar(EntityPlayer ep) {
		for (int i = 0; i < 9; i++)
			ep.inventory.mainInventory[i] = null;
	}

	/** Clears a player's inventory. Args: Player */
	public static void clearInventory(EntityPlayer ep) {
		for (int i = 0; i < ep.inventory.mainInventory.length; i++)
			ep.inventory.mainInventory[i] = null;
	}

	/** Sorts a player's inventory. Args: Player, boolean hotbar only */
	public static void cleanInventory(EntityPlayer ep, boolean hotbar) {

	}

	/** Get the block a player is looking at. Args: Player, Range, Detect 'soft' blocks yes/no *//*
	public static MovingObjectPosition getLookedAtBlock(EntityPlayer ep, int range, boolean hitSoft) {/*
		Vec3 norm = ep.getLookVec();
		World world = ep.worldObj;
		for (float i = 0; i <= range; i += 0.2) {
			int[] xyz = ReikaVectorHelper.getPlayerLookBlockCoords(ep, i);
			Block b = world.getBlock(xyz[0], xyz[1], xyz[2]);
			if (b != Blocks.air) {
				boolean isSoft = ReikaWorldHelper.softBlocks(world, xyz[0], xyz[1], xyz[2]);
				if (hitSoft || !isSoft) {
					return new MovingObjectPosition(xyz[0], xyz[1], xyz[2], 0, norm);
				}
			}
		}
		return null;*//*
		return getLookedAtBlock()
	}*/

	public static MovingObjectPosition getLookedAtBlock(EntityPlayer ep, double reach, boolean liq) {
		Vec3 vec = Vec3.createVectorHelper(ep.posX, (ep.posY + 1.62) - ep.yOffset, ep.posZ);
		Vec3 vec2 = ep.getLook(1.0F);
		Vec3 vec3 = vec.addVector(vec2.xCoord*reach, vec2.yCoord*reach, vec2.zCoord*reach);
		MovingObjectPosition hit = ep.worldObj.rayTraceBlocks(vec, vec3, liq);

		if (hit != null && hit.typeOfHit == MovingObjectType.BLOCK)
			return hit;
		return null;
	}

	@SideOnly(Side.CLIENT)
	public static MovingObjectPosition getLookedAtBlockClient(double reach, boolean liq) {
		EntityPlayer ep = Minecraft.getMinecraft().thePlayer;
		return getLookedAtBlock(ep, reach, liq);
	}

	/** Gets a direction from a player's look direction. Args: Player, allow vertical yes/no */
	public static ForgeDirection getDirectionFromPlayerLook(EntityPlayer ep, boolean vertical) {
		if (MathHelper.abs(ep.rotationPitch) < 60 || !vertical) {
			int i = MathHelper.floor_double((ep.rotationYaw * 4F) / 360F + 0.5D);
			while (i > 3)
				i -= 4;
			while (i < 0)
				i += 4;
			switch (i) {
			case 0:
				return ForgeDirection.SOUTH;
			case 1:
				return ForgeDirection.WEST;
			case 2:
				return ForgeDirection.NORTH;
			case 3:
				return ForgeDirection.EAST;
			}
		}
		else { //Looking up/down
			if (ep.rotationPitch > 0)
				return ForgeDirection.DOWN; //set to up
			else
				return ForgeDirection.UP; //set to down
		}
		return ForgeDirection.UNKNOWN;
	}

	public static FakePlayer getFakePlayerByNameAndUUID(WorldServer world, String name, String uuid) {
		FakePlayer fp = fakePlayers.get(name);
		if (fp == null) {
			fp = FakePlayerFactory.get(world, new GameProfile(UUID.fromString(uuid), name));
			fakePlayers.put(name, fp);
		}
		return fp;
	}

	private static boolean isAdmin(WorldServer world, String name, String uuid) {
		FakePlayer fp = getFakePlayerByNameAndUUID(world, name, uuid);
		return isAdmin(fp);
	}

	public static boolean isAdmin(EntityPlayer ep) {
		return MinecraftServer.getServer().getConfigurationManager().func_152596_g(ep.getGameProfile());
	}

	/** Hacky, but it works */
	public static void setPlayerWalkSpeed(EntityPlayer ep, float speed) {
		PlayerCapabilities pc = ep.capabilities;
		NBTTagCompound nbt = new NBTTagCompound();
		pc.writeCapabilitiesToNBT(nbt);
		nbt.setFloat("walkSpeed", speed);
		pc.readCapabilitiesFromNBT(nbt);
	}

	/** Returns true if the player has the given ID and metadata in their inventory, or is in creative mode.
	 * Args: Player, ID, metadata (-1 for any) */
	public static boolean playerHasOrIsCreative(EntityPlayer ep, Item id, int meta) {
		if (ep.capabilities.isCreativeMode)
			return true;
		ItemStack[] ii = ep.inventory.mainInventory;
		if (meta != -1)
			return (ReikaInventoryHelper.checkForItemStack(id, meta, ii));
		else
			return (ReikaInventoryHelper.checkForItem(id, ii));
	}

	public static boolean playerHasOrIsCreative(EntityPlayer ep, Block id, int meta) {
		return playerHasOrIsCreative(ep, Item.getItemFromBlock(id), meta);
	}

	public static void setFoodLevel(EntityPlayer ep, int level) {
		NBTTagCompound NBT = new NBTTagCompound();
		ep.getFoodStats().writeNBT(NBT);
		NBT.setInteger("foodLevel", level);
		ep.getFoodStats().readNBT(NBT);
	}

	public static void setSaturationLevel(EntityPlayer ep, int level) {
		NBTTagCompound NBT = new NBTTagCompound();
		ep.getFoodStats().writeNBT(NBT);
		NBT.setFloat("foodSaturationLevel", level);
		ep.getFoodStats().readNBT(NBT);
	}

	public static boolean playerCanBreakAt(WorldServer world, BlockArray b, EntityPlayer ep) {
		for (int i = 0; i < b.getSize(); i++) {
			int[] xyz = b.getNthBlock(i);
			if (!playerCanBreakAt(world, xyz[0], xyz[1], xyz[2], ep))
				return false;
		}
		return true;
	}

	public static boolean playerCanBreakAt(WorldServer world, int x, int y, int z, EntityPlayer ep) {
		Block b = world.getBlock(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);
		return playerCanBreakAt(world, x, y, z, b, meta, ep);
	}

	public static boolean playerCanBreakAt(WorldServer world, int x, int y, int z, Block id, int meta, EntityPlayer ep) {
		if (ep == null) {
			ReikaJavaLibrary.pConsole("Cannot check permissions of a null player!");
			return false;
		}
		if (DragonAPICore.isSinglePlayer())
			return true;
		if (isAdmin(ep))
			return true;
		BreakEvent evt = new BreakEvent(x, y, z, world, id, meta, ep);
		MinecraftForge.EVENT_BUS.post(evt);
		return !evt.isCanceled();
	}

	public static boolean playerCanBreakAt(WorldServer world, int x, int y, int z, Block id, int meta, String name, String uuid) {
		if (name == null) {
			ReikaJavaLibrary.pConsole("Cannot check permissions of a null player!");
			return false;
		}
		if (DragonAPICore.isSinglePlayer())
			return true;
		if (isAdmin(world, name, uuid))
			return true;
		FakePlayer fp = getFakePlayerByNameAndUUID(world, name, uuid);
		BreakEvent evt = new BreakEvent(x, y, z, world, id, meta, fp);
		MinecraftForge.EVENT_BUS.post(evt);
		return !evt.isCanceled();
	}

	public static boolean playerCanBreakAt(WorldServer world, int x, int y, int z, String name, String uuid) {
		Block b = world.getBlock(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);
		return playerCanBreakAt(world, x, y, z, b, meta, name, uuid);
	}

	public static void removeExperience(EntityPlayer ep, int xp) {
		while (xp > 0 && ep.experienceTotal > 0) {
			ep.addExperience(-1);
			if (ep.experience < 0) {
				ep.addExperienceLevel(-1);
				ep.experience = 0.95F;
			}
			xp--;
		}
	}

	public static void syncCustomData(EntityPlayerMP ep) {
		ReikaPacketHelper.sendNBTPacket(DragonAPIInit.packetChannel, PacketIDs.PLAYERDATSYNC.ordinal(), ep, ep.getEntityData());
	}

	public static NBTTagCompound getDeathPersistentNBT(EntityPlayer ep) {
		NBTTagCompound nbt = ep.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		ep.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, nbt);
		return nbt;
	}

	public static void schedulePlayerTick(EntityPlayer ep, int ticks) {
		ReikaScheduler.scheduleEvent(new ScheduledTickEvent(ep), ticks);
	}
}
