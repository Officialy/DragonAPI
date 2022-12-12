/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.auxiliary.trackers;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.EnumSet;

import static reika.dragonapi.DragonAPI.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TickRegistry {

    public static final TickRegistry instance = new TickRegistry();
    private final ArrayList<TickHandler> playerTickers = new ArrayList<>();
    private final ArrayList<TickHandler> worldTickers = new ArrayList<>();
    private final ArrayList<TickHandler> renderTickers = new ArrayList<>();
    private final ArrayList<TickHandler> clientTickers = new ArrayList<>();
    private final ArrayList<TickHandler> serverTickers = new ArrayList<>();

    private TickRegistry() {
//        FMLCommonHandler.instance.bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent evt) {
        for (TickHandler h : playerTickers) {
            if (h.canFire(evt.phase)) {
                h.tick(TickType.PLAYER, evt.player, evt.phase);
            }
        }
    }

    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent evt) {
        for (TickHandler h : renderTickers) {
            if (h.canFire(evt.phase)) {
                h.tick(TickType.RENDER, evt.renderTickTime, evt.phase);
            }
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent evt) {
        for (TickHandler h : clientTickers) {
            if (h.canFire(evt.phase)) {
                h.tick(TickType.CLIENT, evt.phase);
            }
        }
    }

    @SubscribeEvent
    public void worldTick(TickEvent.LevelTickEvent evt) {
        for (TickHandler h : worldTickers) {
            if (h.canFire(evt.phase)) {
                h.tick(TickType.WORLD, evt.level, evt.phase);
            }
        }
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent evt) {
        for (TickHandler h : serverTickers) {
            if (h.canFire(evt.phase)) {
                h.tick(TickType.SERVER, evt.phase);
            }
        }
    }

    public void registerTickHandler(TickHandler h) {
        for (TickType type : h.getType()) {
            switch (type) {
                case CLIENT -> clientTickers.add(h);
                case PLAYER -> playerTickers.add(h);
                case RENDER -> renderTickers.add(h);
                case SERVER -> serverTickers.add(h);
                case WORLD -> worldTickers.add(h);
/*
				case ALL:
					clientTickers.add(h);
					playerTickers.add(h);
					renderTickers.add(h);
					serverTickers.add(h);
					worldTickers.add(h);
					break;*/
            }
        }
    }

    public enum TickType {
        /**
         * Fired during the world evaluation loop
         * server side only! ("and client side" is false)
         * <p>
         * arg 0 : The world that is ticking
         */
        WORLD,
        /**
         * client side
         * Fired during the render processing phase
         * arg 0 : float "partial render time"
         */
        RENDER,
        /**
         * client side only
         * Fired once per client tick loop.
         */
        CLIENT,
        /**
         * client and server side.
         * Fired whenever the players update loop runs.
         * arg 0 : the player
         * arg 1 : the world the player is in
         */
        PLAYER,
        /**
         * server side only.
         * This is the server game tick.
         * Fired once per tick loop on the server.
         */
        SERVER
    }

    public interface TickHandler {

        void tick(TickType type, Object... tickData);

        EnumSet<TickType> getType();

        boolean canFire(TickEvent.Phase p);

        String getLabel();

    }

}
