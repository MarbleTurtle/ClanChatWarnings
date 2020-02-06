package com.ClanChatWarnings;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ClanChatPlus")
public interface ClanChatWarningsConfig extends Config
{
	@ConfigItem(
			keyName = "warnedPlayers",
			name = "Warn On Join",
			description = "List of players you want to be warned when joining",
			position = 1
	)
	default String warnedPlayers() {
		return "";
	}
	@ConfigItem(
			keyName = "warnedAttention",
			name = "Alert On Warning",
			description = "Causes an alter when a player from warning list joins",
			position = 2
	)
	default boolean warnedAttention() {
		return false;
	}
}
