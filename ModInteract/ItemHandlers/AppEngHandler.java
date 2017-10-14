/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.DragonAPI.ModInteract.ItemHandlers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.ModList;
import Reika.DragonAPI.Base.ModHandlerBase;

public class AppEngHandler extends ModHandlerBase {

	private static final AppEngHandler instance = new AppEngHandler();

	private ItemStack certus;
	private ItemStack dust;

	private ItemStack fluix;
	private ItemStack fluixdust;

	private ItemStack silicon;

	private ItemStack basicChip;
	private ItemStack advChip;
	private ItemStack basicChipPattern;
	private ItemStack advChipPattern;

	private ItemStack siliconPress;
	private ItemStack logicPress;
	private ItemStack calcPress;
	private ItemStack engPress;

	private ItemStack goldProcessor;
	private ItemStack quartzProcessor;
	private ItemStack diamondProcessor;

	private Item cell1k;
	private Item cell4k;
	private Item cell16k;
	private Item cell64k;

	public final Block skystone;
	public final Block quartzGlass;

	private Object itemList;
	private Object matList;
	private Object blockList;
	private Object partList;

	private Class itemClass;
	private Class matClass;
	private Class blockClass;
	private Class partClass;

	private Method itemGet;
	private Method itemstackGet;
	private Method blockGet;

	private AppEngHandler() {
		super();
		Block sky = null;
		Block glass = null;
		if (this.hasMod()) {
			try {
				this.initGetters();

				certus = this.getMaterial("materialCertusQuartzCrystal");
				dust = this.getMaterial("materialCertusQuartzDust");
				fluix = this.getMaterial("materialFluixCrystal");
				fluixdust = this.getMaterial("materialFluixDust");

				silicon = this.getMaterial("materialSilicon");

				basicChip = this.getMaterial("materialBasicCard");
				advChip = this.getMaterial("materialAdvCard");
				basicChipPattern = this.getMaterial("materialBasicCard");
				advChipPattern = this.getMaterial("materialAdvCard");

				calcPress = this.getMaterial("materialCalcProcessorPress");
				engPress = this.getMaterial("materialEngProcessorPress");
				logicPress = this.getMaterial("materialLogicProcessorPress");
				siliconPress = this.getMaterial("materialSiliconPress");

				goldProcessor = this.getMaterial("materialLogicProcessor");
				quartzProcessor = this.getMaterial("materialCalcProcessor");
				diamondProcessor = this.getMaterial("materialEngProcessor");

				sky = this.getBlock("blockSkyStone");
				glass = this.getBlock("blockQuartzGlass");

				cell1k = this.getItem("itemCell1k");
				cell4k = this.getItem("itemCell4k");
				cell16k = this.getItem("itemCell16k");
				cell64k = this.getItem("itemCell64k");
			}
			catch (Exception e) {
				DragonAPICore.logError("Cannot read AE class contents!");
				e.printStackTrace();
				this.logFailure(e);
			}
		}
		else {
			this.noMod();
		}
		skystone = sky;
		quartzGlass = glass;
	}

	private void initGetters() throws Exception {
		Class ae = Class.forName("appeng.core.Api", false, Launch.classLoader);
		Object inst = this.getInstance(ae);

		Field b = ae.getDeclaredField("blocks");
		Field i = ae.getDeclaredField("items");
		Field p = ae.getDeclaredField("parts");
		Field m = ae.getDeclaredField("materials");

		b.setAccessible(true);
		i.setAccessible(true);
		p.setAccessible(true);
		m.setAccessible(true);

		partList = p.get(inst);
		itemList = i.get(inst);
		blockList = b.get(inst);
		matList = m.get(inst);

		Class def = Class.forName("appeng.api.util.AEItemDefinition");

		itemGet = def.getMethod("item");
		blockGet = def.getMethod("block");
		itemstackGet = def.getMethod("stack", int.class);

		partClass = Class.forName("appeng.api.definitions.Parts");
		itemClass = Class.forName("appeng.api.definitions.Items");
		blockClass = Class.forName("appeng.api.definitions.Blocks");
		matClass = Class.forName("appeng.api.definitions.Materials");
	}

	private Object getInstance(Class ae) throws Exception {
		String[] f = {"instance", "INSTANCE", "Instance"};
		Field instance = null;
		for (int i = 0; i < f.length; i++) {
			try {
				instance = ae.getField(f[i]);
				if (instance != null)
					break;
			}
			catch (NoSuchFieldException e) {

			}
		}
		if (instance == null) {
			throw new NoSuchFieldException("AE API Instance field not found!");
		}
		Object inst = instance.get(null);
		if (inst == null) {
			throw new NullPointerException("Instance field found but was empty!");
		}
		return inst;
	}

	private Block getBlock(String field) throws Exception {
		Field f = blockClass.getField(field);
		Object def = f.get(blockList);
		return (Block)blockGet.invoke(def);
	}

	private Item getItem(String field) throws Exception {
		Field f = itemClass.getField(field);
		Object def = f.get(itemList);
		return (Item)itemGet.invoke(def);
	}

	private ItemStack getMaterial(String field) throws Exception {
		Field f = matClass.getField(field);
		Object def = f.get(matList);
		return (ItemStack)itemstackGet.invoke(def, 1);
	}

	private ItemStack getPart(String field) throws Exception {
		Field f = partClass.getField(field);
		Object def = f.get(partList);
		return (ItemStack)itemstackGet.invoke(def, 1);
	}

	public static AppEngHandler getInstance() {
		return instance;
	}

	@Override
	public boolean initializedProperly() {
		return certus != null && dust != null && skystone != null;
	}

	@Override
	public ModList getMod() {
		return ModList.APPENG;
	}

	public ItemStack getCertusQuartz() {
		return certus != null ? certus.copy() : null;
	}

	public ItemStack getCertusQuartzDust() {
		return dust != null ? dust.copy() : null;
	}

	public ItemStack getFluixCrystal() {
		return fluix != null ? fluix.copy() : null;
	}

	public ItemStack getFluixDust() {
		return fluixdust != null ? fluixdust.copy() : null;
	}

	public Collection<ItemStack> getPossibleMeteorChestLoot() {
		ArrayList<ItemStack> li = new ArrayList();
		li.add(calcPress);
		li.add(engPress);
		li.add(logicPress);
		li.add(siliconPress);
		return li;
	}

	public Collection<ItemStack> getMeteorChestLoot() {
		ArrayList<ItemStack> li = new ArrayList();
		int n = 1+rand.nextInt(3);
		for (int i = 0; i < n; i++) {
			switch (rand.nextInt(4)) {
				case 0:
					li.add(calcPress);
					break;
				case 1:
					li.add(engPress);
					break;
				case 2:
					li.add(logicPress);
					break;
				case 3:
					li.add(siliconPress);
					break;
			}
		}
		return li;
	}

	public ItemStack getBasicChipPattern() {
		return basicChipPattern != null ? basicChipPattern.copy() : null;
	}

	public ItemStack getAdvancedChipPattern() {
		return advChipPattern != null ? advChipPattern.copy() : null;
	}

	public ItemStack getSilicon() {
		return silicon != null ? silicon.copy() : null;
	}

	public ItemStack getGoldProcessor() {
		return goldProcessor != null ? goldProcessor.copy() : null;
	}

	public ItemStack getQuartzProcessor() {
		return quartzProcessor != null ? quartzProcessor.copy() : null;
	}

	public ItemStack getDiamondProcessor() {
		return diamondProcessor != null ? diamondProcessor.copy() : null;
	}

	public Item get1KCell() {
		return cell1k;
	}

	public Item get4KCell() {
		return cell4k;
	}

	public Item get16KCell() {
		return cell16k;
	}

	public Item get64KCell() {
		return cell64k;
	}

}
