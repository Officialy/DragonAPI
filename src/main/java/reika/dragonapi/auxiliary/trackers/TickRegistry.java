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

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.EnumSet;

// 1.21.5: handlers here are instance methods; class is registered via NeoForge.EVENT_BUS.register(this)
// in the constructor. Do NOT add @EventBusSubscriber — it requires static @SubscribeEvent methods.
public class TickRegistry {

    public static final TickRegistry instance = new TickRegistry();
    private final ArrayList<TickHandler> playerTickers = new ArrayList<>();
    private final ArrayList<TickHandler> worldTickers = new ArrayList<>();
    private final ArrayList<TickHandler> renderTickers = new ArrayList<>();
    private final ArrayList<TickHandler> clientTickers = new ArrayList<>();
    private final ArrayList<TickHandler> serverTickers = new ArrayList<>();

    private TickRegistry() {
//        FMLCommonHandler.instance.bus().register(this);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void playerTickPre(PlayerTickEvent.Pre evt) {
        for (TickHandler h : playerTickers) {
            if (h.canFire(Phase.START)) {
                h.tick(TickType.PLAYER, evt.getEntity(), Phase.START);
            }
        }
    }

    @SubscribeEvent
    public void playerTickPost(PlayerTickEvent.Post evt) {
        for (TickHandler h : playerTickers) {
            if (h.canFire(Phase.END)) {
                h.tick(TickType.PLAYER, evt.getEntity(), Phase.END);
            }
        }
    }

    @SubscribeEvent
    public void renderTick(RenderFrameEvent.Pre evt) { // TODO: Verify this is correct event
        for (TickHandler h : renderTickers) {
            // Phase concept may have changed in render events
            h.tick(TickType.RENDER, evt.getPartialTick());
        }
    }

    @SubscribeEvent
    public void clientTickPre(ClientTickEvent.Pre evt) {
        for (TickHandler h : clientTickers) {
            if (h.canFire(Phase.START)) {
                h.tick(TickType.CLIENT, Phase.START);
            }
        }
    }

    @SubscribeEvent
    public void clientTickPost(ClientTickEvent.Post evt) {
        for (TickHandler h : clientTickers) {
            if (h.canFire(Phase.END)) {
                h.tick(TickType.CLIENT, Phase.END);
            }
        }
    }

    @SubscribeEvent
    public void worldTickPre(LevelTickEvent.Pre evt) {
        for (TickHandler h : worldTickers) {
            if (h.canFire(Phase.START)) {
                h.tick(TickType.WORLD, evt.getLevel(), Phase.START);
            }
        }
    }

    @SubscribeEvent
    public void worldTickPost(LevelTickEvent.Post evt) {
        for (TickHandler h : worldTickers) {
            if (h.canFire(Phase.END)) {
                h.tick(TickType.WORLD, evt.getLevel(), Phase.END);
            }
        }
    }

    @SubscribeEvent
    public void serverTickPre(ServerTickEvent.Pre evt) {
        for (TickHandler h : serverTickers) {
            if (h.canFire(Phase.START)) {
                h.tick(TickType.SERVER, Phase.START);
            }
        }
    }

    @SubscribeEvent
    public void serverTickPost(ServerTickEvent.Post evt) {
        for (TickHandler h : serverTickers) {
            if (h.canFire(Phase.END)) {
                h.tick(TickType.SERVER, Phase.END);
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

    // Define Phase enum locally to maintain backward compatibility
    public enum Phase {
        START, END
    }

    public interface TickHandler {

        void tick(TickType type, Object... tickData);

        EnumSet<TickType> getType();

        boolean canFire(Phase p);

        String getLabel();

    }

}

