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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Common tick dispatch; physical-client events are bridged by ClientTickEvents. */
public class TickRegistry {
    public static final TickRegistry instance = new TickRegistry();
    private final EnumMap<TickType, CopyOnWriteArrayList<TickHandler>> handlers = new EnumMap<>(TickType.class);

    private TickRegistry() {
        for (TickType type : TickType.values()) handlers.put(type, new CopyOnWriteArrayList<>());
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent public void playerTickPre(PlayerTickEvent.Pre event) {
        dispatch(TickType.PLAYER, Phase.START, event.getEntity(), Phase.START);
    }

    @SubscribeEvent public void playerTickPost(PlayerTickEvent.Post event) {
        dispatch(TickType.PLAYER, Phase.END, event.getEntity(), Phase.END);
    }

    @SubscribeEvent public void worldTickPre(LevelTickEvent.Pre event) {
        dispatch(TickType.WORLD, Phase.START, event.getLevel(), Phase.START);
    }

    @SubscribeEvent public void worldTickPost(LevelTickEvent.Post event) {
        dispatch(TickType.WORLD, Phase.END, event.getLevel(), Phase.END);
    }

    @SubscribeEvent public void serverTickPre(ServerTickEvent.Pre event) {
        dispatch(TickType.SERVER, Phase.START, Phase.START, event.getServer());
    }

    @SubscribeEvent public void serverTickPost(ServerTickEvent.Post event) {
        dispatch(TickType.SERVER, Phase.END, Phase.END, event.getServer());
    }

    /** Client bridge entry point; original CLIENT payload is [phase]. */
    public void fireClient(Phase phase) {
        dispatch(TickType.CLIENT, phase, phase);
    }

    /** Client bridge entry point; original RENDER payload is [Float partial tick, phase]. */
    public void fireRender(float partialTick, Phase phase) {
        dispatch(TickType.RENDER, phase, partialTick, phase);
    }

    private void dispatch(TickType type, Phase phase, Object... data) {
        Objects.requireNonNull(phase, "phase");
        // Each phase sees a stable registration snapshot, including during nested/reentrant registration.
        for (TickHandler handler : handlers.get(type)) {
            if (handler.canFire(phase)) handler.tick(type, data.clone());
        }
    }

    public void registerTickHandler(TickHandler handler) {
        Objects.requireNonNull(handler, "handler");
        for (TickType type : Objects.requireNonNull(handler.getType(), "handler tick types")) handlers.get(type).add(handler);
    }

    /** Remove this handler's registrations by identity, without removing equal-but-distinct handlers. */
    public void unregisterTickHandler(TickHandler handler) {
        Objects.requireNonNull(handler, "handler");
        handlers.values().forEach(list -> list.removeIf(existing -> existing == handler));
    }

    public enum TickType {
        /** Level evaluation: [level, phase]. NeoForge emits both logical sides; consumers retain their own side policy. */
        WORLD,
        /** Physical-client frame: [Float pause/freeze-aware partial tick, phase]. */
        RENDER,
        /** Physical-client game tick: [phase]. */
        CLIENT,
        /** Player update on either logical side: [player, phase]. */
        PLAYER,
        /** Server game tick: [phase, server]. The optional server context retains the accepted port's second slot. */
        SERVER
    }

    public enum Phase { START, END }

    public interface TickHandler {
        void tick(TickType type, Object... tickData);
        EnumSet<TickType> getType();
        boolean canFire(Phase phase);
        String getLabel();
    }
}
