package com.ClanChatWarnings;

import com.google.common.base.Splitter;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ClanManager;
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
	private final List<Integer> coolTimer;
	private final List<String> coolName;
	private final List<Integer> trackTimer;
	private final List<String> trackName;
	private final List<String> clansName;
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
	private ClanManager clanManager;

	public ClanChatWarningsPlugin() {
		this.warnings = new ArrayList();
		this.exemptPlayers = new ArrayList();
		this.warnPlayers= new ArrayList();
		this.coolTimer = new ArrayList();
		this.coolName= new ArrayList();
		this.trackTimer = new ArrayList();
		this.trackName= new ArrayList();
		this.clansName= new ArrayList();
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
		this.coolTimer.clear();
		this.coolName.clear();
		this.trackTimer.clear();
		this.trackName.clear();
		this.clansName.clear();
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


	private void sendNotification(String player,String Comment,int type) {
		StringBuilder stringBuilder = new StringBuilder();
		if(type==1) {
			stringBuilder.append("has joined Clan Chat. ").append(Comment);
			String notification = stringBuilder.toString();
			if (this.config.kickable()) {
				this.client.addChatMessage(ChatMessageType.FRIENDSCHAT, player, notification, "Warning");
			} else {
				this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, "", player + " " + notification, "");
			}
			if (this.config.warnedAttention()) {
				if (this.clanJoinedTick != this.client.getTickCount() || this.config.selfPing()) {
					this.ping.notify(player+" "+notification);
				}
			}
		}else if(type==2){
			stringBuilder.append(" has left Clan Chat.");
			String notification = stringBuilder.toString();
			this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, "", player + " " + notification, "");
			if(this.config.trackerPing()) {
				this.ping.notify(player+" "+notification);
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		if(chatMessage.getType()==ChatMessageType.FRIENDSCHAT&&this.config.track()) {
			if (chatMessage.getName().equals(this.client.getLocalPlayer().getName())) {
				if (chatMessage.getMessage().contains(this.config.trackerTrigger())) {
					for (String name : clansName) {
						if (chatMessage.getMessage().toLowerCase().contains(name.toLowerCase())) {
							if(trackName.contains(name.toLowerCase())){
								trackTimer.set(trackName.indexOf(name.toLowerCase()), (int) (this.config.trackerLength() / .6));
							}else {
								trackName.add(name.toLowerCase());
								trackTimer.add((int) (this.config.trackerLength() / .6));
							}
						}
					}
				}
			}
		}
	}

	@Subscribe
	public void onClanMemberLeft(ClanMemberLeft event){
		String name=event.getMember().getName();
		if(trackName.contains(name.toLowerCase())){
			sendNotification(Text.toJagexName(name),"",2);
			trackTimer.remove(trackName.indexOf(name.toLowerCase()));
			trackName.remove(name.toLowerCase());
		}
		if (clansName.contains(name)){
			clansName.remove(name);
		}
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined event){
		if(this.config.track()){
			clansName.add(event.getMember().getName());
		}
		if (this.clanJoinedTick != this.client.getTickCount()){
			hopping=false;
		}
		//God have mercy on your soul if you're about to check how I did this.
		if ((this.clanJoinedTick != this.client.getTickCount()||this.config.selfCheck())&&!hopping) {
			ClanMember member = event.getMember();
			String memberNameX = Text.toJagexName(member.getName());
			String memberNameP = Text.toJagexName(member.getName());
			String memberNameR = Text.toJagexName(member.getName());
			StringBuffer sx;
			StringBuffer sp;
			StringBuffer sr;
			if(memberNameX.equalsIgnoreCase(Text.toJagexName(this.client.getLocalPlayer().getName())))
				return;
			if(coolName.contains(member.getName()))
				return;
			for(Iterator var2 = this.exemptPlayers.iterator(); var2.hasNext(); memberNameX = sx.toString()) { //For exempting people from being pinged
				Pattern pattern = (Pattern) var2.next();
				Matcher n = pattern.matcher(memberNameX.toLowerCase());
				sx = new StringBuffer();
				while(n.matches()) {
					if(pattern.toString().substring(2,pattern.toString().length()-2).toLowerCase().equals(memberNameX.toLowerCase())) {
						return;
					}
				}
				n.appendTail(sx);
			}
			for(Iterator var4 = this.warnPlayers.iterator(); var4.hasNext(); memberNameP = sp.toString()) { //For checking specific players
				Pattern pattern = (Pattern)var4.next();
				Pattern patternDiv=Pattern.compile("~");
				//Yes Im aware this String is dumb, frankly I spent 20 mins trying to fix it and this is what worked so it stays.
				String slash= "\\\\";
				String sections[]=patternDiv.split(pattern.toString());
				String note="";
				String nameP="";
				if(sections.length>1) {
					for (Integer x = 0; x < sections.length; x++) {
						if (x == sections.length - 1) {
							String notes[] = sections[x].split(slash);
							if(x>1)
								note+="~";
							note += notes[0];
						} else if (x != 0) {
							if(x>1)
								note+="~";
							note += sections[x];
						}
					}
				}
				if(sections.length==1) {
					nameP = sections[0].substring(2, sections[0].length()-2);
					nameP=nameP.trim();
				}else{
					nameP = sections[0].substring(2, sections[0].trim().length());
				}
				pattern=Pattern.compile(nameP.toLowerCase());
				Matcher l = pattern.matcher(memberNameP.toLowerCase());
				sp = new StringBuffer();
				while(l.matches()) {
					if(nameP.toLowerCase().equals(memberNameP.toLowerCase())) {
						sendNotification(Text.toJagexName(member.getName()), note,1);
						if(this.config.cooldown()>0) {
							coolName.add(Text.toJagexName(member.getName()));
							coolTimer.add((int)(this.config.cooldown()/.6));
						}
						break;
					}
				}
				l.appendTail(sp);
			}
			for(Iterator var3 = this.warnings.iterator(); var3.hasNext(); memberNameR = sr.toString()) { //For checking the regex
				Pattern pattern = (Pattern)var3.next();
				Pattern patternDiv=Pattern.compile("~");
				//Yes Im aware this String is dumb, frankly I spent 20 mins trying to fix it and this is what worked so it stays.
				String slash= "\\\\";
				String sections[]=patternDiv.split(pattern.toString());
				String note="";
				if(sections.length>1) {
					for (Integer x = 0; x < sections.length; x++) {
						if (x == sections.length - 1) {
							String notes[] = sections[x].split(slash);
							if(x>1)
								note+="~";
							note += notes[0];
							pattern=Pattern.compile(sections[0].trim().toLowerCase());
						} else if (x != 0) {
							if(x>1)
								note+="~";
							note += sections[x];
						}
					}
				}
				Matcher m = pattern.matcher(memberNameR.toLowerCase());
				sr = new StringBuffer();
				while(m.find()) {
					sendNotification(Text.toJagexName(member.getName()),note,1);
					if(this.config.cooldown()>0) {
						coolName.add(Text.toJagexName(member.getName()));
						coolTimer.add((int)(this.config.cooldown()/.6));
					}
					break;
				}
				m.appendTail(sr);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if(this.config.cooldown()>0){
			for(int i=0; i<coolTimer.size(); i++){
				if (coolTimer.get(i) > 0){
					coolTimer.set(i, coolTimer.get(i) - 1);
				}else{
					coolTimer.remove(i);
					coolName.remove(i);
				}
			}
		}
		if(!trackName.isEmpty()){
			for(int i=0; i<trackTimer.size(); i++){
				if (trackTimer.get(i) > 0){
					trackTimer.set(i, trackTimer.get(i) - 1);
				}else{
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
