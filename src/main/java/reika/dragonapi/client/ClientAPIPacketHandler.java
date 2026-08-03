package reika.dragonapi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import reika.dragonapi.libraries.rendering.ReikaRenderHelper;

import reika.dragonapi.auxiliary.PacketTypes;
import reika.dragonapi.auxiliary.trackers.CommandableUpdateChecker;
import reika.dragonapi.command.EntityListCommand;
import reika.dragonapi.instantiable.effects.StringParticleFX;
import reika.dragonapi.libraries.io.ReikaSoundHelper;
import reika.dragonapi.libraries.registry.ReikaParticleHelper;
import reika.dragonapi.APIPacketHandler.PacketIDs;
import reika.dragonapi.instantiable.event.client.ClientLoginEvent;
import net.neoforged.neoforge.common.NeoForge;
import reika.dragonapi.auxiliary.trackers.SettingInterferenceTracker;
import reika.dragonapi.auxiliary.trackers.KeyWatcher;
import reika.dragonapi.auxiliary.trackers.ModFileVersionChecker;
import reika.dragonapi.auxiliary.PopupWriter;
import reika.dragonapi.auxiliary.ModularLogger;
import reika.dragonapi.command.BiomeMapCommand;
import reika.dragonapi.instantiable.event.client.ClientLogoutEvent;
import reika.dragonapi.instantiable.event.client.PlayerInteractEventClient;
import reika.dragonapi.instantiable.event.RawKeyPressEvent;
import reika.dragonapi.libraries.ReikaEntityHelper;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.base.BlockEntityBase;
import reika.dragonapi.interfaces.registry.SoundEnum;


/**
 * The client half of {@code APIPacketHandler}.
 *
 * <p>Split out because the handler class is loaded and verified on a dedicated server during common
 * setup, and this code names {@code ClientLevel}, {@code ParticleEngine} and {@code Particle} --
 * enough to make the whole class unloadable there and abort DragonAPI's setup.
 */
public final class ClientAPIPacketHandler {

    private ClientAPIPacketHandler() {}

    public static void handle(int x, int y, int z, PacketIDs pack, int[] data, String sg, Player player) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null)
            return;
        switch (pack) {
            case NUMBERPARTICLE ->
                    Minecraft.getInstance().particleEngine.add(new StringParticleFX(world, x + 0.5, y + 0.5, z + 0.5, String.valueOf(data[0]), 0, 0, 0));
            case STRINGPARTICLE -> {
                StringParticleFX fx = new StringParticleFX(world, x + 0.5, y + 0.5, z + 0.5, sg, 0, 0, 0);
                fx.setLife(Math.max(15, 3 * sg.length()));
                fx.setScale(Math.max(0.01F, Math.min(1, 0.5F / sg.length())));
                Minecraft.getInstance().particleEngine.add(fx);
            }
            case ENTITYDUMP -> EntityListCommand.dumpClientside();
            case EXPLODE -> {
                ReikaSoundHelper.playSoundAtBlock(world, x, y, z, SoundEvents.GENERIC_EXPLODE.value());
                ReikaParticleHelper.EXPLODE.spawnAroundBlock(world, new BlockPos(x, y, z), 1);
            }
            case OLDMODS -> CommandableUpdateChecker.instance.onClientReceiveOldModID(sg);
            case LOGIN -> {
                NeoForge.EVENT_BUS.post(new ClientLoginEvent(player, data[0] > 0));
                SettingInterferenceTracker.instance.onLogin(player);
            }
            case LOGOUT -> NeoForge.EVENT_BUS.post(new ClientLogoutEvent(player));
            case BREAKPARTICLES -> {
                Block b = Block.stateById(data[0]).getBlock();
                ReikaRenderHelper.spawnDropParticles(world, x, y, z, b, data[1]);
            }
            case ITEMDROPPER -> {
                Entity e = world.getEntity(data[0]);
                if (e instanceof ItemEntity) {
                    e.getPersistentData().putString("dropper", sg);
                }
            }
            case GUIRELOAD -> {
//                if (Minecraft.getInstance().screen != null)
//                    Minecraft.getInstance().screen.initGui();
            }
            case POPUP -> PopupWriter.instance().addMessage(new PopupWriter.Warning(sg, data[0]));
            case SENDLATENCY -> {
                long t3 = System.currentTimeMillis();
                long t1 = ReikaJavaLibrary.buildLong(data[0], data[1]);
                long t2 = ReikaJavaLibrary.buildLong(data[2], data[3]);
                long toServerTime = t2 - t1;
                long toClientTime = t3 - t2;
                ReikaChatHelper.write("Total latency: " + toServerTime + "ms to server, " + toClientTime + "ms from server.");
            }
            case REDSTONECHANGE ->
                    ((BlockEntityBase) world.getBlockEntity(new BlockPos(x, y, z))).onRedstoneChangedClientside(data[0] > 0, data[1] > 0);
            case CLEARCHAT -> ReikaChatHelper.clearChat();

//            case MODLOCK:
//                ModLockController.instance.readSync(player, sg);
//                break;
//            case OREDUMP:
//                OreDumpCommand.dumpClientside(sg);
//                break;
            default -> {
            }
        }
    }
}
