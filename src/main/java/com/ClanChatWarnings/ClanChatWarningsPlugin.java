package com.ClanChatWarnings;

import com.google.common.base.Splitter;
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
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

@Slf4j
@PluginDescriptor(
	name = "Clan Chat Warnings"
)
public class ClanChatWarningsPlugin extends Plugin{
	private static final Splitter NEWLINE_SPLITTER = Splitter.on("\n").omitEmptyStrings().trimResults();
	private final List<Pattern> warnings;
	private final List<Pattern> warnPlayers;
	private final List<Pattern> exemptPlayers;
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

	public ClanChatWarningsPlugin() {

		this.warnings = new ArrayList();
		this.exemptPlayers = new ArrayList();
		this.warnPlayers= new ArrayList();

	}

	@Override
	protected void startUp()
	{
		this.updateSets();
	}

	@Override
	protected void shutDown()
	{
		this.warnings.clear();
		this.exemptPlayers.clear();
		this.warnPlayers.clear();
	}

	void updateSets() {
		this.warnings.clear();
		this.exemptPlayers.clear();
		this.warnPlayers.clear();

		Stream var10000 = Text.fromCSV(this.config.warnPlayers()).stream().map((s) -> {
			return Pattern.compile(Pattern.quote(s), 2);
		});
		List var10001 = this.warnPlayers;
		var10000.forEach(var10001::add);
		Stream var10002 = NEWLINE_SPLITTER.splitToList(this.config.warnings()).stream().map((s) -> {
			try {
				return Pattern.compile(s, 2);
			} catch (PatternSyntaxException var2) {
				return null;
			}
		}).filter(Objects::nonNull);
		List var10003 = this.warnings;
		var10002.forEach(var10003::add);
		Stream var10004 = Text.fromCSV(this.config.exemptPlayers()).stream().map((s) -> {
			return Pattern.compile(Pattern.quote(s), 2);
		});
		List var10005 = this.exemptPlayers;
		var10004.forEach(var10005::add);
	}


	private void sendNotification(String player,String Comment) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("has joined Clan Chat. ").append(Comment);
		String notification = stringBuilder.toString();
		if(this.config.kickable()) {
			this.client.addChatMessage(ChatMessageType.FRIENDSCHAT, player, notification, "Warning");
		}else{
			this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION,"",player+" "+notification,"");
		}
		if(this.config.warnedAttention()) {
			if (this.clanJoinedTick != this.client.getTickCount() || this.config.selfPing()) {
				this.ping.notify(notification);
			}
		}
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined event){
		if (this.clanJoinedTick != this.client.getTickCount()){
			hopping=false;
		}
		//God have mercy on your soul if you're about to check how I did this.

		if ((this.clanJoinedTick != this.client.getTickCount()||this.config.selfCheck())&&!hopping) {
			ClanMember member = event.getMember();
			String memberName = Text.toJagexName(member.getName());
			String memberName2 = Text.toJagexName(member.getName());
			String memberName3 = Text.toJagexName(member.getName());
			StringBuffer sb;
			StringBuffer sc;
			StringBuffer sa;
			if(memberName.equalsIgnoreCase(Text.toJagexName(this.client.getLocalPlayer().getName())))
				return;
			for(Iterator var2 = this.exemptPlayers.iterator(); var2.hasNext(); memberName = sb.toString()) { //For exempting people from being pinged
				Pattern pattern = (Pattern) var2.next();
				Matcher n = pattern.matcher(memberName.toLowerCase());
				sb = new StringBuffer();
				while(n.find()) {
					if(pattern.toString().substring(2,pattern.toString().length()-2).toLowerCase().equals(memberName.toLowerCase())) {
						return;
					}
				}
				n.appendTail(sb);
			}
			for(Iterator var3 = this.warnings.iterator(); var3.hasNext(); memberName2 = sc.toString()) { //For checking the regex
				Pattern pattern1 = (Pattern)var3.next();
				Pattern pattern2=Pattern.compile("-");
				String temp= "\\\\";
				String test[]=pattern2.split(pattern1.toString());
				String note="";
				if(test.length>1) {
					for (Integer x = 0; x < test.length; x++) {
						if (x == test.length - 1) {
							String test2[] = test[x].split(temp);
							if(x>1)
								note+="-";
							note += test2[0];
							pattern1=Pattern.compile(test[0].trim().toLowerCase());
						} else if (x != 0) {
							if(x>1)
								note+="-";
							note += test[x];
						}
					}
				}
				Matcher m = pattern1.matcher(memberName2.toLowerCase());
				sc = new StringBuffer();
				while(m.find()) {
					sendNotification(Text.toJagexName(member.getName()),note);
					break;
				}
				m.appendTail(sc);
			}
			for(Iterator var4 = this.warnPlayers.iterator(); var4.hasNext(); memberName3 = sa.toString()) { //For checking specific players
				Pattern pattern3 = (Pattern)var4.next();
				Pattern pattern4=Pattern.compile("-");
				String temp= "\\\\";
				String test[]=pattern4.split(pattern3.toString());
				String note="";
				String temp2="";
				if(test.length>1) {
					for (Integer x = 0; x < test.length; x++) {
						if (x == test.length - 1) {
							String test2[] = test[x].split(temp);
							if(x>1)
								note+="-";
							note += test2[0];
						} else if (x != 0) {
							if(x>1)
								note+="-";
							note += test[x];
						}
					}
				}
				if(test.length==1) {
					System.out.println(test[0]);
					temp2 = test[0].substring(2, test[0].length()-2);
					temp2=temp2.trim();
				}else{
					System.out.println(test[0]);
					temp2 = test[0].substring(2, test[0].trim().length());
				}
				pattern3=Pattern.compile(temp2.toLowerCase());
				Matcher l = pattern3.matcher(memberName3.toLowerCase());
				sa = new StringBuffer();
				System.out.println(temp2+","+memberName3);
				while(l.find()) {
					if(temp2.toLowerCase().equals(memberName3.toLowerCase())) {
						sendNotification(Text.toJagexName(member.getName()), note);
						break;
					}
				}
				l.appendTail(sa);
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
		if(gameStateChanged.getGameState()==GameState.HOPPING){
			hopping=true;
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
