/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.gui;

import reika.dragonapi.libraries.io.ReikaChatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

public class StatusLogger {

	private final ArrayList<String> data = new ArrayList<>();

	public void addStatus(String sg, boolean state) {
		this.addStatus(sg, state ? State.ACTIVE : State.INACTIVE);
	}

	public void addStatus(String sg, State s) {
		data.add(s.color + sg + ": " + s.tag);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String s : data) {
			sb.append(s);
			sb.append("\n");
		}
		return sb.toString();
	}

	public void sendToPlayer(Player ep) {
		for (String s : data) {
			ReikaChatHelper.sendChatToPlayer(ep, s);
		}
	}

	public enum State {
		ACTIVE(ChatFormatting.GREEN, "True"),
		INACTIVE(ChatFormatting.RED, "False"),
		CONDITIONAL(ChatFormatting.BLUE, "Conditional"),
		WARN(ChatFormatting.YELLOW, "Warning"),
		ERROR(ChatFormatting.LIGHT_PURPLE, "Error");

		private final ChatFormatting color;
		private final String tag;

		State(ChatFormatting c, String s) {
			color = c;
			tag = s;
		}
	}

}
