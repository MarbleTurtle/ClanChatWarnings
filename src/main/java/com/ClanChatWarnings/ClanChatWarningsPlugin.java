package com.ClanChatWarnings;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import net.runelite.client.Notifier;
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
	private MenuManager menuManager;
	@Inject
	private ClanChatWarningsConfig config;

	public ClanChatWarningsPlugin() {
		this.warnings = new ArrayList();
	}

	@Override
	protected void startUp()
	{
		this.updateWarners();
	}

	@Override
	protected void shutDown()
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


	private void sendNotification(String player,String Comment) {
		StringBuilder stringBuilder = new StringBuilder();
		if(Comment.isEmpty()) {
			stringBuilder.append(player).append(" has joined Clan Chat, beware.");
		}else{
			stringBuilder.append(player).append(" has joined Clan Chat. ").append(Comment);
		}
		String notification = stringBuilder.toString();
		this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, "", notification, "");
		if(this.config.warnedAttention()) {
			if (this.clanJoinedTick != this.client.getTickCount() || this.config.selfPing()) {
				this.ping.notify(notification);
				System.out.println(notification);
			}
		}
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined event){
		if (this.clanJoinedTick != this.client.getTickCount()||this.config.selfCheck()) {
			ClanMember member = event.getMember();
			String memberName = Text.toJagexName(member.getName());
			StringBuffer sb;
			for(Iterator var4 = this.warnings.iterator(); var4.hasNext(); memberName = sb.toString()) {
				Pattern pattern = (Pattern)var4.next();
				Pattern pattern1=Pattern.compile("-");
				String temp= "\\\\";
				String test[]=pattern1.split(pattern.toString());
				String note="";
				if(test.length>1) {
					for (Integer x = 0; x < test.length; x++) {
						if (x == test.length - 1) {
							String test2[] = test[x].split(temp);
							if(x>1)
								note+="-";
							note += test2[0];
							String temp2= String.format("%s\\%s", test[0], test2[1]);
							pattern=Pattern.compile(temp2);
						} else if (x != 0) {
							if(x>1)
								note+="-";
							note += test[x];
						}
					}
				}
				Matcher m = pattern.matcher(memberName);
				sb = new StringBuffer();
				while(m.find()) {
					sendNotification(memberName,note);
				}
				m.appendTail(sb);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals("Clan Chat Warnings")) {
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
	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widget) {
		if(widget.getGroupId()==94) {
			System.out.println("Clan setup opened");
			Widget[] clanNames = this.client.getWidget(94,0).getChildren();
			System.out.println(clanNames.length);
		}
	}
}
