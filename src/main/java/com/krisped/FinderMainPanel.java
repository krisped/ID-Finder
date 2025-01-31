package com.krisped;

import net.runelite.client.game.ItemManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;

public class FinderMainPanel extends PluginPanel
{
    private final FinderPlugin plugin;
    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private final ItemPanel itemPanel;
    private final JPanel homePanel;

    public FinderMainPanel(FinderPlugin plugin, ItemManager itemManager, ClientThread clientThread)
    {
        this.plugin = plugin;
        this.cardLayout = new CardLayout();
        this.contentPanel = new JPanel(cardLayout);
        this.itemPanel = new ItemPanel(this, itemManager, clientThread);

        // Hovedside
        homePanel = new JPanel();
        homePanel.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Finder Plugin", SwingConstants.CENTER);
        JButton itemDatabaseButton = new JButton("Item Database");

        itemDatabaseButton.addActionListener(e -> showItemPanel());

        homePanel.add(titleLabel, BorderLayout.NORTH);
        homePanel.add(itemDatabaseButton, BorderLayout.CENTER);

        // Legg til paneler
        contentPanel.add(homePanel, "Home");
        contentPanel.add(itemPanel, "ItemDatabase");

        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
    }

    public void showHomePanel()
    {
        cardLayout.show(contentPanel, "Home");
    }

    public void showItemPanel()
    {
        cardLayout.show(contentPanel, "ItemDatabase");
    }
}
