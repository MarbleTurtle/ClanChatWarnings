package com.ClanChatWarnings;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ClanChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import net.runelite.client.Notifier;
import net.runelite.api.events.ClanMemberJoined;

import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@PluginDescriptor(
	name = "Clan Chat Warnings"
)
public class ClanChatWarningsPlugin extends Plugin{
	private final List<Pattern> warnings;
	private int clanJoinedTick;
	@Inject
	private Client client;
	@Inject
	private Notifier ping;

	@Inject
	private ClanChatWarningsConfig config;

	public ClanChatWarningsPlugin() {
		this.warnings = new ArrayList();
	}

	@Override
	protected void startUp() throws Exception
	{
		this.updateWarners();
	}

	@Override
	protected void shutDown() throws Exception
	{
		this.warnings.clear();
	}

	void updateWarners() {
		this.warnings.clear();
		Stream var10000 = Text.fromCSV(this.config.warnedPlayers()).stream().map((s) -> {
			return Pattern.compile(Pattern.quote(s), 2);
		});
		List var10001 = this.warnings;
		var10000.forEach(var10001::add);
	}

	private void sendNotification(String player) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(player).append(" has joined Clan Chat, beware.");
		String notification = stringBuilder.toString();
		System.out.println(notification);
		this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, "", notification, "");
		if(this.config.warnedAttention())
			this.ping.notify(notification);
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined event) {
		ClanMember member = event.getMember();
		String memberName = Text.toJagexName(member.getName());
		StringBuffer sb;
		for(Iterator var4 = this.warnings.iterator(); var4.hasNext(); memberName = sb.toString()) {
			Pattern pattern = (Pattern)var4.next();
			Matcher m = pattern.matcher(memberName);
			sb = new StringBuffer();
			while(m.find()) {
				if (this.clanJoinedTick != this.client.getTickCount()) {
					sendNotification(memberName);
				}
			}
			m.appendTail(sb);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals("ClanChatPlus")) {
			this.updateWarners();
		}
	}

	@Subscribe
	public void onClanChanged(ClanChanged event) {
		if (event.isJoined()) {
			this.clanJoinedTick = this.client.getTickCount();
		}
	}

	@Provides
	ClanChatWarningsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClanChatWarningsConfig.class);
	}
}
