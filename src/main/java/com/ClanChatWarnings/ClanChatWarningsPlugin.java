package com.ClanChatWarnings;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import okhttp3.*;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private final List<String> friendChatName = new ArrayList<>();
    private final List<String> remoteNames = new ArrayList<>();
    private boolean hopping;
    private int clanJoinedTick;
    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private boolean hotKeyPressed;
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private Notifier ping;
    @Inject
    private MenuManager menuManager;
    @Inject
    private ClanChatWarningsConfig config;
    @Inject
    CCWInputListener hotKeyListener;
    @Inject
    KeyManager keyManager;
    @Inject
    OkHttpClient httpClient;


    @Override
    protected void startUp() {
        this.updateSets();
        keyManager.registerKeyListener(hotKeyListener);
        fetchRemoteNames(false);
    }

    @Override
    protected void shutDown() {
        this.warnings.clear();
        this.exemptPlayers.clear();
        this.warnPlayers.clear();
        this.cooldownMap.clear();
        this.friendChatName.clear();
        keyManager.unregisterKeyListener(hotKeyListener);
    }

    @Subscribe
    public void onFocusChanged(FocusChanged focusChanged) {
        if (!focusChanged.isFocused()) {
            hotKeyPressed = false;
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        //If you or someone you love is able to figure out how to only have this enabled for clan chat, hit a Turtle up.
        if(config.menu()&&(hotKeyPressed||!config.shiftClick())){
            int groupId = WidgetUtil.componentToInterface(event.getActionParam1());
            String option = event.getOption();
            if (groupId == InterfaceID.FRIENDS_CHAT && (option.equals("Add ignore") || option.equals("Remove friend"))||
                groupId == InterfaceID.PRIVATE_CHAT && (option.equals("Add ignore") || option.equals("Message"))||
                groupId == InterfaceID.CHATBOX && (option.equals("Add ignore") || option.equals("Message"))) {
                client.createMenuEntry(1)
                    .setOption("Add to CC Warnings")
                    .setTarget(event.getTarget())
                    .setType(MenuAction.RUNELITE)
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setIdentifier(event.getIdentifier());
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuOption().equals("Add to CC Warnings")) {
           config.warnPlayers(config.warnPlayers()+", "+Text.standardize(event.getMenuTarget()));
            this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", Text.removeTags(event.getMenuTarget())+" has been added to Clan Chat Warnings.", "");
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
                this.client.addChatMessage(ChatMessageType.FRIENDSCHAT, player, notification, "Warning");
            } else {
                this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, "", player + " " + notification, "");
            }
            if (this.config.warnedAttention()) {
                if (this.clanJoinedTick != this.client.getTickCount() || this.config.selfPing()) {
                    this.ping.notify(player + " " + notification);
                }
            }
        }
    }

    @Subscribe
    public void onFriendsChatMemberJoined(FriendsChatMemberJoined event) {
        if (this.clanJoinedTick != this.client.getTickCount()) {
            hopping = false;
        }

        if (clanJoinedTick != client.getTickCount() || (this.config.selfCheck() && !hopping)) {
            final FriendsChatMember member = event.getMember();
            final String memberName = Text.standardize(member.getName());
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
        if (gameStateChanged.getGameState() == GameState.LOGGING_IN || gameStateChanged.getGameState() == GameState.HOPPING) {
            fetchRemoteNames(false);
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
        username = Text.standardize(username);
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

        if (config.remoteEnabled()) {
            if (remoteNames.contains(username))
                return "";
        }

        return null;
    }

    @Subscribe(priority = -2) // Run after RuneLite's Menu Entry Swapper and geheur's Custom Menu Swaps
    public void onPostMenuSort(PostMenuSort e) {
        MenuEntry[] menuEntries = client.getMenuEntries();
        if (menuEntries.length == 0) return;
        for (MenuEntry entry : menuEntries) {
            if (entry.getType().equals(MenuAction.CC_OP_LOW_PRIORITY)) {
                if (entry.getOption().equalsIgnoreCase("kick") || entry.getOption().equalsIgnoreCase("kick user")) {
                    String target = Text.standardize(entry.getTarget());
                    if (warnPlayers.containsKey(target)) {
                        entry.setType(MenuAction.CC_OP);
                        client.setMenuEntries(new MenuEntry[]{entry});
                    }
                }
            }

            if (entry.getOption().equalsIgnoreCase("chat-channel")) {
                if (config.remoteEnabled()) {
                    client.createMenuEntry(1).setOption("Refresh Remote Warnings").setType(MenuAction.RUNELITE).setTarget("(" + remoteNames.size() + ")")
                            .onClick(clickEvent ->
                            {
                                fetchRemoteNames(true);
                            });
                }
            }
        }
    }

    public void fetchRemoteNames(boolean verbose) {
        if (!config.remoteEnabled() || config.remoteURL().isEmpty())
            return;
        remoteNames.clear();

        HttpUrl url = null;
        try {
            url = HttpUrl.get(config.remoteURL());
        } catch (Exception e) {
            if (verbose) {
                clientThread.invoke(() -> {
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "Error", e.getMessage(), "Error2");
                });
            }
            return;
        }

        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (verbose) {
                    clientThread.invoke(() -> {
                        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "Error", e.getMessage(), "Error");
                    });
                }
                log.error("Error fetching remote warning list", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String raw = response.body().string();
                    for (String name : raw.split(",")) {
                        String standardized = Text.standardize(name);
                        if (!standardized.isEmpty())
                            remoteNames.add(standardized);
                    }

                    if (verbose)
                        clientThread.invoke(() -> {
                            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Fetched " + remoteNames.size() + " names.", "");
                        });
                } else {
                    if (verbose) {
                        clientThread.invoke(() -> {
                            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "Error", "Got response code: " + response.code(), "Error2");
                        });
                    }
                    log.error("Error fetching remote warning list");
                }
            }
        });
    }
}