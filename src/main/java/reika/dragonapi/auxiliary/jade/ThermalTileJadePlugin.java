/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 ******************************************************************************/
package reika.dragonapi.auxiliary.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.interfaces.blockentity.ThermalTile;

/**
 * Jade integration: adds a "Temperature: N°C" line to the tooltip of EVERY block whose block entity
 * implements {@link ThermalTile} — which is the common super-interface of ReactorCraft's
 * {@code Temperatured}, RotaryCraft's {@code TemperatureTE}, and any other DragonAPI thermal machine,
 * so one provider covers all of them across every dependent mod.
 *
 * <p>The temperature is read server-side ({@link IServerDataProvider#appendServerData}) and shipped to
 * the client, so it works regardless of whether a given machine syncs its temperature to clients.</p>
 */
@WailaPlugin(DragonAPI.MODID)
public class ThermalTileJadePlugin implements IWailaPlugin {

    public static final Identifier UID = Identifier.fromNamespaceAndPath(DragonAPI.MODID, "temperature");

    private static final String TAG = "dragonapi_temperature";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ServerData.INSTANCE, Block.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Component_.INSTANCE, Block.class);
    }

    // Jade 1.21.6+ forbids a single provider from implementing both IComponentProvider and
    // IServerDataProvider — they must be separate classes (server-side data vs client-side tooltip).
    private enum ServerData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof ThermalTile t)
                data.putInt(TAG, t.getTemperature());
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    private enum Component_ implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains(TAG))
                tooltip.add(Component.literal("Temperature: " + data.getIntOr(TAG, 0) + "°C"));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
