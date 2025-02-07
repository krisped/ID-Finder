package com.krisped;

import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

public class ItemInfoPanel extends PluginPanel {
    private final String itemName;
    private final int itemId;
    private final ItemManager itemManager;
    private final FinderMainPanel parentPanel;

    public ItemInfoPanel(String itemName, int itemId, ItemManager itemManager, FinderMainPanel parentPanel) {
        this.itemName = itemName;
        this.itemId = itemId;
        this.itemManager = itemManager;
        this.parentPanel = parentPanel;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Øverste header
        JLabel headerLabel = new JLabel("Item Information Panel", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(headerLabel, BorderLayout.NORTH);

        // Hovedpanel med vertikal layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Item bilde (sentrum)
        Image image = itemManager.getImage(itemId);
        JLabel imageLabel = new JLabel();
        if (image != null) {
            ImageIcon icon = new ImageIcon(image);
            imageLabel.setIcon(icon);
        }
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(imageLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Navn og ID
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel nameLabel = new JLabel(itemName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 22));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel idLabel = new JLabel("(ID: " + itemId + ")");
        idLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        idLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        namePanel.add(nameLabel);
        namePanel.add(idLabel);
        mainPanel.add(namePanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Seksjoner – her brukes faste eksempelverdier
        mainPanel.add(createSectionPanel("Market Info:", new String[] {
                "• GE Price: 100,000 gp",
                "• High Alch: 60,000 gp (Nature Rune: 300 gp)",
                "• Low Alch: 40,000 gp",
                "• Daily Volume: 5000",
                "• 30d Avg Price: 95,000 gp"
        }));
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(createSectionPanel("Combat Info:", new String[] {
                "• Type: Melee Weapon",
                "• Attack Bonus: +60",
                "• Special Attack: Dragon Breath",
                "• Equipable: Yes",
                "• Stackable: No"
        }));
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(createSectionPanel("How to Obtain / Crafting:", new String[] {
                "• Fletching Level: 85",
                "• Crafting Method: Cut from Dragonstone"
        }));
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(createSectionPanel("Dropped By:", new String[] {
                "• King Black Dragon (1/512)",
                "• Vorkath (1/200)"
        }));
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(createSectionPanel("Extra Info:", new String[] {
                "• Weight: 1.5 kg",
                "• Tradable: Yes",
                "• Examine: \"A powerful weapon of legend.\"",
                "• Members Only: Yes"
        }));
        mainPanel.add(Box.createVerticalStrut(10));

        // Legg mainPanel i en scroll-pane i tilfelle innholdet blir for langt
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Nederst: knappene
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton wikiButton = new JButton("OSRS Wiki Link");
        JButton backButton = new JButton("Back to Search");
        wikiButton.setFont(new Font("Arial", Font.PLAIN, 16));
        backButton.setFont(new Font("Arial", Font.PLAIN, 16));

        wikiButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String encodedName = java.net.URLEncoder.encode(itemName, "UTF-8");
                    String url = "https://oldschool.runescape.wiki/w/" + encodedName;
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentPanel.showItemDatabasePanel();
            }
        });

        buttonPanel.add(wikiButton);
        buttonPanel.add(backButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // Hjelpemetode for å lage en seksjon med tittel og linjer med informasjon
    private JPanel createSectionPanel(String sectionTitle, String[] lines) {
        JPanel sectionPanel = new JPanel();
        sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
        sectionPanel.setBorder(BorderFactory.createTitledBorder(sectionTitle));
        for (String line : lines) {
            JLabel label = new JLabel(line);
            label.setFont(new Font("Arial", Font.PLAIN, 16));
            sectionPanel.add(label);
        }
        sectionPanel.add(Box.createVerticalStrut(5));
        return sectionPanel;
    }
}
