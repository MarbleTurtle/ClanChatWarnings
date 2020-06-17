package com.ClanChatWarnings;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.FriendChatManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@PluginDescriptor(
        name = "Clan Chat Warnings"
)
public class ClanChatWarningsPlugin extends Plugin {
    private static final Splitter NEWLINE_SPLITTER = Splitter.on("\n").omitEmptyStrings().trimResults();
    private static final String MESSAGE_DELIMITER = "~";
    private static final ImmutableList<String> AFTER_OPTIONS = ImmutableList.of("Message", "Add ignore", "Remove friend", "Kick");
    private final Map<Pattern, String> warnings = new HashMap<>();
    private final Map<String, String> warnPlayers = new HashMap<>();
    private final Set<String> exemptPlayers = new HashSet<>();
    private final Map<String, Instant> cooldownMap = new HashMap<>();
    private final List<Integer> trackTimer = new ArrayList<>();
    private final List<String> trackName = new ArrayList<>();
    private final List<String> friendChatName = new ArrayList<>();
    private boolean hopping;
    private int clanJoinedTick;
    @Inject
    private Client client;
    @Inject
    private Notifier ping;
    @Inject
    private MenuManager menuManager;
    @Inject
    private ClanChatWarningsConfig config;
    @Inject
    private FriendChatManager friendChatManager;

    @Override
    protected void startUp() {
        this.updateSets();
    }

    @Override
    protected void shutDown() {
        this.warnings.clear();
        this.exemptPlayers.clear();
        this.warnPlayers.clear();
        this.cooldownMap.clear();
        this.trackTimer.clear();
        this.trackName.clear();
        this.friendChatName.clear();
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        //If you or someone you love is able to figure out how to only have this enabled for clan and private chat, hit a Turtle up.
        if (true && this.config.track()) {
            int groupId = WidgetInfo.TO_GROUP(event.getActionParam1());
            String option = event.getOption();
            if (groupId == WidgetInfo.CHATBOX.getGroupId() && !"Kick".equals(option) || groupId == WidgetInfo.PRIVATE_CHAT_MESSAGE.getGroupId()) {
                if (!AFTER_OPTIONS.contains(option)) {
                    return;
                }
                MenuEntry track = new MenuEntry();
                track.setOption("Track Player");
                track.setTarget(event.getTarget());
                track.setType(MenuAction.RUNELITE.getId());
                track.setParam0(event.getActionParam0());
                track.setParam1(event.getActionParam1());
                track.setIdentifier(event.getIdentifier());
                this.insertMenuEntry(track, this.client.getMenuEntries());
            }
        }
    }

    private void insertMenuEntry(MenuEntry newEntry, MenuEntry[] entries) {
        MenuEntry[] newMenu = ObjectArrays.concat(entries, newEntry);
        int menuEntryCount = newMenu.length;
        ArrayUtils.swap(newMenu, menuEntryCount - 1, menuEntryCount - 2);
        this.client.setMenuEntries(newMenu);
    }

    @Subscribe
    public void onPlayerMenuOptionClicked(PlayerMenuOptionClicked event) {
        if (event.getMenuOption().equals("Track Player")) {
            for (String name : friendChatName) {
                if (event.getMenuTarget().toLowerCase().contains(name.toLowerCase())) {
                    if (trackName.contains(name.toLowerCase())) {
                        trackTimer.set(trackName.indexOf(name.toLowerCase()), (int) (this.config.trackerLength() / .6));
                    } else {
                        trackName.add(name.toLowerCase());
                        trackTimer.add((int) (this.config.trackerLength() / .6));
                    }
                }
            }
        }
    }

    void updateSets() {
        this.warnings.clear();
        this.exemptPlayers.clear();
        this.warnPlayers.clear();
        warnings.putAll(NEWLINE_SPLITTER.splitToList(this.config.warnings()).stream()
                .map((s) -> s.split(MESSAGE_DELIMITER))
                .collect(Collectors.toMap(p -> Pattern.compile(p[0].trim(), Pattern.CASE_INSENSITIVE), p -> p.length > 1 ? p[1].trim() : ""))
        );
        exemptPlayers.addAll(Text.fromCSV(this.config.exemptPlayers()).stream()
                .map((s) -> s.toLowerCase().trim())
                .collect(Collectors.toSet())
        );


        warnPlayers.putAll(Text.fromCSV(this.config.warnPlayers()).stream()
                .map((s) -> s.split(MESSAGE_DELIMITER))
                .collect(Collectors.toMap(p -> p[0].toLowerCase().trim(), p -> p.length > 1 ? p[1].trim() : "",(p1,p2)->p1))
        );
    }


    private void sendNotification(String player, String Comment, int type) {
        StringBuilder stringBuilder = new StringBuilder();
        if (type == 1) {
            stringBuilder.append("has joined Friends Chat. ").append(Comment);
            String notification = stringBuilder.toString();
            if (this.config.kickable()) {
                this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, player, notification, "Warning");
            } else {
                this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, "", player + " " + notification, "");
            }
            if (this.config.warnedAttention()) {
                if (this.clanJoinedTick != this.client.getTickCount() || this.config.selfPing()) {
                    this.ping.notify(player + " " + notification);
                }
            }
        } else if (type == 2) {
            stringBuilder.append(" has left Friends Chat.");
            String notification = stringBuilder.toString();
            this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, "", player + " " + notification, "");
            if (this.config.trackerPing()) {
                this.ping.notify(player + " " + notification);
            }
        }
    }

    @Subscribe
    public void onFriendsChatMemberLeft(FriendsChatMemberLeft event) {
        String name = event.getMember().getName();
        if (trackName.contains(name.toLowerCase())) {
            sendNotification(toTrueName(name), "", 2);
            trackTimer.remove(trackName.indexOf(name.toLowerCase()));
            trackName.remove(name.toLowerCase());
        }
        friendChatName.remove(name);
    }

    @Subscribe
    public void onFriendsChatMemberJoined(FriendsChatMemberJoined event) {
        if (this.config.track()) {
            friendChatName.add(event.getMember().getName());
        }

        if (this.clanJoinedTick != this.client.getTickCount()) {
            hopping = false;
        }

        if (clanJoinedTick != client.getTickCount() || (this.config.selfCheck() && !hopping)) {
            final FriendsChatMember member = event.getMember();
            final String memberName = toTrueName(member.getName().trim());
            final String localName = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getName();

            if (memberName == null || (memberName.equalsIgnoreCase(localName) && !config.selfCheck())) {
                return;
            }

            final String warningMessage = getWarningMessageByUsername(memberName);
            if (warningMessage != null) {
                if (config.cooldown() > 0) {
                    cooldownMap.put(memberName.toLowerCase(), Instant.now());
                }
                sendNotification(memberName, warningMessage, 1);
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!trackName.isEmpty()) {
            for (int i = 0; i < trackTimer.size(); i++) {
                if (trackTimer.get(i) > 0) {
                    trackTimer.set(i, trackTimer.get(i) - 1);
                } else {
                    trackTimer.remove(i);
                    trackName.remove(i);
                }
            }
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals("ClanChatPlus")) {
            this.updateSets();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.HOPPING) {
            hopping = true;
        }
    }

    @Subscribe
    public void onFriendsChatChanged(FriendsChatChanged event) {
        if (event.isJoined()) {
            this.clanJoinedTick = this.client.getTickCount();
        }
    }

    @Provides
    ClanChatWarningsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ClanChatWarningsConfig.class);
    }


    /**
     * Grabs the relevant warning message for the specified username accounting for all config options
     *
     * @param username players in-game name (shown in join message)
     * @return warning message or null if username should be ignored
     */
    @Nullable
    private String getWarningMessageByUsername(String username) {
        username = username.toLowerCase();
        // This player is exempt from any warning.
        if (exemptPlayers.contains(username)) {
            return null;
        }

        if (cooldownMap.containsKey(username)) {
            final Instant cutoff = Instant.now().minus(config.cooldown(), ChronoUnit.SECONDS);
            // If the cutoff period is after (greater) than the stored time they should come off cooldown
            if (cutoff.compareTo(cooldownMap.get(username)) > 0) {
                cooldownMap.remove(username);
            } else {
                return null;
            }
        }

        // This player name is listed inside the config
        if (warnPlayers.containsKey(username)) {
            return warnPlayers.get(username);
        }

        for (final Map.Entry<Pattern, String> entry : warnings.entrySet()) {
            final Matcher m = entry.getKey().matcher(username);
            if (m.find()) {
                return entry.getValue();
            }
        }

        return null;
    }
	private String toTrueName(String str)
	{
		return CharMatcher.ascii().retainFrom(str.replace('\u00A0', ' ')).trim();
	}
}