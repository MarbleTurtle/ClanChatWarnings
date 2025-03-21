package com.ClanChatWarnings;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("ClanChatPlus")
public interface ClanChatWarningsConfig extends Config
{

	@ConfigSection(name = "Player Lists", description = "List of players to give warnings for, regex lists, and exempt players", position = -1)
	final String sectionLists = "sectionLists";

	@ConfigItem(
			keyName = "notifiedPlayer",
			name = "Players Warnings",
			description = "List of players you want to be warned of on joining. Supports notes by adding '~' after name. Separate with commas.",
			position = 0,
			section = sectionLists
	)
	default String warnPlayers() {
		return "";
	}
	@ConfigItem(
			keyName = "notifiedPlayer",
			name = "Players Warnings",
			description = "List of players you want to be warned of on joining. Supports notes by adding '~' after name. Separate with commas.",
			position = 0,
			section = sectionLists
	)
	void warnPlayers(String str);
	@ConfigItem(
			keyName = "notifiedMass",
			name = "Regex Warnings",
			description = "Regex warnings for players joining Friends Chat. Supports notes by adding '~' after name. Separate with new lines.",
			position = 1,
			section = sectionLists
	)
	default String warnings() {
		return "";
	}
	@ConfigItem(
			keyName = "exemptPlayers",
			name = "Exempt Players",
			description = "Players to be ignored when joining Friends Chat.",
			position = 2,
			section = sectionLists
	)
	default String exemptPlayers() { return "";}
	@ConfigItem(
			keyName = "cooldown",
			name = "Cooldown",
			description = "Cooldown, in seconds, before you will be notified of a player joining again.",
			position = 3,
			section = sectionLists
	)
	default int cooldown() { return 30;}
	@ConfigItem(
			keyName = "warnedAndAlerted",
			name = "Warning Notification",
			description = "Ping if player procs a warning.",
			position = 4
	)
	default Notification warningNotification() { return Notification.ON;}
	@ConfigItem(
			keyName = "Check on self join",
			name = "Check on Joining",
			description = "Runs the check when you join Friends Chat.",
			position = 5
	)
	default boolean selfCheck() { return false;}
	@ConfigItem(
			keyName = "Ping on self join",
			name = "Ping on Joining",
			description = "If \"Check on Joining\" is enabled, will ping if players on the list are in cc when you join Friends Chat.",
			position = 6
	)
	default boolean selfPing() { return false;}
	@ConfigItem(
			keyName = "Kicks",
			name = "Kick from Warning",
			description = "Changes message to support kicking players from warning.",
			position = 7
	)
	default boolean kickable() { return false;}
	@ConfigItem(
			keyName = "Menu Add",
			name = "Right click add to Player Warnings",
			description = "Adds a right click option to player messages to add them to the list (Warning will not visually update if config is open).",
			position = 8
	)
	default boolean menu() { return false;}
	@ConfigItem(
			keyName = "ShiftMenu",
			name = "Shift required",
			description = "Requires shift to be held to see the add to warnings option.",
			position = 9
	)
	default boolean shiftClick() { return false;}

    @ConfigItem(
            keyName = "MenuSwap",
            name = "Menu Entry Swap",
            description = "Swap menu entries to allow left click kicks. (Warning: removes all other right click options on players in the list)",
            position = 10
    )
    default boolean menuSwap() { return false;}

    @ConfigSection(name = "Remote Settings", description = "Use a website as an external list", position = 100)
    final String sectionRemote = "sectionRemote";

    @ConfigItem(
            keyName = "remoteEnabled",
            name = "Enabled",
            description = "Enable pulling from an external website on login. (Refresh list by right clicking a chat-channel icon)",
			warning = "Warning: This plugin can fetch text from 3rd party websites not controlled or verified by the RuneLite Developers",
            position = 101,
            section = sectionRemote
    )
    default boolean remoteEnabled() {
        return false;
    }

	@ConfigItem(
			keyName = "remoteURL",
			name = "Remote URL",
			description = "The url of the website you want to pull, must be plain text, use commas as a delimiter.",
			position = 102,
			section = sectionRemote
	)
	default String remoteURL() {
		return "";
	}

	@ConfigItem(
			keyName = "remoteAuthorization",
			name = "Remote Authorization",
			description = "Set the authorization header (if server requires it)",
			position = 103,
			section = sectionRemote
	)
	default String remoteAuthorization() {
		return "";
	}


	@ConfigItem(
			keyName = "postNewNames",
			name = "Post New Names",
			description = "Add a menu entry to submit to a remote url",
			warning = "Warning: This setting will submit your new names added by the right click menu, your username, the current chat owner, and an authorization header(if set) to a 3rd party websites not controlled or verified by the RuneLite Developers",
			position = 104,
			section = sectionRemote
	)
	default boolean postNewNames() { return false; }

	@ConfigItem(
			keyName = "submissionURL",
			name = "Submission URL",
			description = "Submit new names added to your list to a remote url",
			position = 105,
			section = sectionRemote
	)
	default String submissionURL() { return ""; }

	@ConfigItem(
			keyName = "submissionAuthorization",
			name = "Authorization",
			description = "Set the authorization header (if server requires it)",
			position = 106,
			section = sectionRemote
	)
	default String submissionAuthorization() { return ""; }

}
