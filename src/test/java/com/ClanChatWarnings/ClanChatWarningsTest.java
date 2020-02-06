package com.ClanChatWarnings;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ClanChatWarningsTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ClanChatWarningsPlugin.class);
		RuneLite.main(args);
	}
}