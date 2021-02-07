package com.ClanChatWarnings;

import net.runelite.client.input.KeyListener;

import javax.inject.Inject;
import java.awt.event.KeyEvent;

public class CCWInputListener implements KeyListener {
    private static final int HOTKEY = KeyEvent.VK_SHIFT;

    private final ClanChatWarningsPlugin plugin;

    @Inject
    private CCWInputListener(ClanChatWarningsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == HOTKEY) {
            plugin.setHotKeyPressed(true);
            System.out.println("True");
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == HOTKEY) {
            plugin.setHotKeyPressed(false);
            System.out.println("False");
        }
    }
}
