package com.ClanChatWarnings;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ClanChatPlus")
public interface ClanChatWarningsConfig extends Config
{
	@ConfigItem(
			keyName = "notifiedPlayers",
			name = "Warn on join",
			description = "List of players you want to be warned of on joining.\nSupports notes by adding '-' after name.",
			position = 0
	)
	default String warnedPlayers() {
		return "";
	}
	@ConfigItem(
			keyName = "warnedAndAlerted",
			name = "Alert On Warning",
			description = "Ping if player procs a warning",
			position = 1
	)
	default boolean warnedAttention() { return true;}
	@ConfigItem(
			keyName = "Check on self join",
			name = "Check on self join",
			description = "Runs the check when you join cc.",
			position = 2
	)
	default boolean selfCheck() { return false;}
	@ConfigItem(
			keyName = "Ping on self join",
			name = "Ping on self join",
			description = "If \"Check on self join\" is enabled, will ping if players on the list are in cc when you join cc.",
			position = 3
	)
	default boolean selfPing() { return false;}
}
