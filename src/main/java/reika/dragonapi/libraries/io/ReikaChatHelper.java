/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.libraries.io;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.ForgeRegistries;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;

public final class ReikaChatHelper {

    public static void clearChat() {
        clearChat(null);
    }

    public static void clearChat(ServerPlayer ep) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            clearChatGui();
        } else if (ep != null) {
            ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.CLEARCHAT.ordinal(), ep);
        }
    }

/*    public static void sendChatToAllOnServer(String sg) {
        String[] parts = sg.split("\\n"); // \n no longer works in chat as of 1.7
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv != null) {
            ServerConfigurationManager cfg = srv.getConfigurationManager();
            if (cfg != null) {
                for (int i = 0; i < parts.length; i++) {
                    Component chat = Component.translatable(parts[i]);
                    cfg.sendChatMsg(chat);
                }
            }
            else {
                DragonAPI.LOGGER.error("Something tried to send chat to a server with null configurations!");
                ReikaJavaLibrary.dumpStack();
            }
        }
        else {
            DragonAPI.LOGGER.error("Something tried to send chat to a null server!");
            ReikaJavaLibrary.dumpStack();
        }
    }*/
    private static void clearChatGui() {
        Minecraft.getInstance().gui.getChat().clearMessages(true);
    }

    /**
     * Writes an itemstack to the chat.
     * Args: Level, itemstack
     */
    public static void writeItemStack(Level world, ItemStack is) {
        String msg;
        if (is == null)
            msg = "Null Stack!";
        else
            msg = String.format("%d, %d, %d", is.getItem(), is.getCount());
        writeString(msg);
    }

    /**
     * Writes coordinates to the chat.
     * Args: Level, x, y, z
     */
    public static void writeCoords(Level world, BlockPos pos) {
        String msg;
        msg = String.format("%.2f, %.2f, %.2f", pos.getX(), pos.getY(), pos.getZ());
        writeString(msg);
    }

    /**
     * Writes a block ID and coordinates to the chat.
     * Args: Level, x, y, z
     */
    public static void writeBlockAtCoords(Level world, BlockPos pos) {
        StringBuilder sb = new StringBuilder();
        String name;
        Block id = world.getBlockState(pos).getBlock();
        if (id != Blocks.AIR)
            name = id.toString();
        else
            name = "Air";
        sb.append(String.format("Block %s pos=%d", name, Block.getId(id.defaultBlockState()), pos) + "\n");
        BlockEntity te = world.getBlockEntity(pos);
        if (te == null) {
            sb.append("No Tile Entity at this location.");
        } else {
            sb.append("Tile Entity at this location:\n");
            sb.append(te);
        }
        writeString(sb.toString());
    }

    /**
     * Writes an integer to the chat. Args: Integer
     */
    public static void writeInt(int num) {
        writeString(String.format("%d", num));
    }

    public static void writeString(String sg) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            writeChatString(sg);
        }
    }

    /**
     * Writes any general-purpose string to the chat. Args: String
     */

    private static void writeChatString(String sg) {
        if (Minecraft.getInstance().player != null)
            sendChatToPlayer(Minecraft.getInstance().player, sg);
    }

    /**
     * Automatically translates if possible.
     */
    public static void writeLocalString(String tag) {
        //writeString(I18n.get(tag));
    }

    /**
     * A general object-to-chat function. Autoclips doubles to 3 decimals. Args: Object
     */
    public static void write(Object obj) {
        if (obj == null) {
            writeString("null");
            return;
        }
        String str;
        if (obj.getClass() == Double.class)
            str = String.format("%.3f", obj);
        else
            str = String.valueOf(obj);
        writeString(str);
    }

    public static void writeFormattedString(String str, ChatFormatting... fm) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fm.length; i++)
            sb.append(fm[i].toString());
        writeString(sb + str);
    }

    public static void writeEntity(Level world, Entity ent) {
        if (world == null)
            return;
        if (ent == null)
            writeString("null");
        else
            writeString(ent.getName() + " @ " + String.format("%.2f, %.2f, %.2f", ent.getX(), ent.getY(), ent.getZ()));
    }

    public static void writeItem(Level world, Item id) {
        if (id == null)
            writeString("Null Item");
            //else if (id < 256)
            //	writeBlock(world, id, dmg);
        else
            writeString(id + " is " + id.getDefaultInstance().getItem().getName(new ItemStack(id, 1)));
    }

    public static void writeBlock(Level world, Block id) {
        if (id == Blocks.AIR)
            writeString("Null Item");
            //else if (id > 4096)
            //	writeItem(world, id, meta);
        else
            writeString(id + ":" + " is " + ForgeRegistries.BLOCKS.getKey(id).getNamespace());
    }

    public static void writeSide() {
        writeString(String.valueOf(FMLLoader.getDist()));
    }

    public static void sendChatToPlayer(Player ep, String sg) {
        if (sg.length() > 16384) {
            int idx = 0;
            while (idx < sg.length()) {
                String sg2 = sg.substring(idx, Math.min(idx + 16384, sg.length()));
                sendChatToPlayer(ep, sg2);
                idx += 16384;
            }
            return;
        }
        String[] parts = sg.split("\\n");
        for (String part : parts) {
            Component chat = Component.literal(part);
            ep.displayClientMessage(chat, true);
        }
    }
/*
	public static void sendChatToAllOnServer(String sg) {
		String[] parts = sg.split("\\n"); // \n no longer works in chat as of 1.7
		MinecraftServer srv = Minecraft.getServer();
		if (srv != null) {
			ServerConfigurationManager cfg = srv.getConfigurationManager();
			if (cfg != null) {
				for (int i = 0; i < parts.length; i++) {
					TextComponent chat = Component.literal(parts[i]);
					cfg.sendChatMsg(chat);
				}
			} else {
				DragonAPI.LOGGER.error("Something tried to send chat to a server with null configurations!");
				ReikaJavaLibrary.dumpStack();
			}
		} else {
			DragonAPI.LOGGER.error("Something tried to send chat to a null server!");
			ReikaJavaLibrary.dumpStack();
		}
	}*/

}
