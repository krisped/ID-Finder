package com.krisped;

import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;

public class FinderMainPanel extends PluginPanel
{
    private final FinderPlugin plugin;
    private final CardLayout cardLayout;
    private final JPanel contentPanel;

    private final ItemPanel itemPanel;
    private final SpriteDatabasePanel spritePanel;
    private final NpcPanel npcPanel; // Nytt panel for NPC Database

    public FinderMainPanel(FinderPlugin plugin, ItemManager itemManager, ClientThread clientThread,
                           Client client, SpriteManager spriteManager)
    {
        this.plugin = plugin;
        this.cardLayout = new CardLayout();
        this.contentPanel = new JPanel(cardLayout);

        // Opprett panelene
        this.itemPanel = new ItemPanel(this, itemManager, clientThread, client);
        this.spritePanel = new SpriteDatabasePanel(client, this, spriteManager, clientThread);
        this.npcPanel = new NpcPanel(client, clientThread, this); // Fjernet SpriteManager

        // Opprett hovedpanelet med BorderLayout
        JPanel homePanel = new JPanel(new BorderLayout());
        homePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding rundt kantene

        // Header: Bruker CustomHeader direkte i denne filen
        CustomHeader header = new CustomHeader("KrisPed's", "Database");
        homePanel.add(header, BorderLayout.NORTH);

        // Opprett knappene
        JButton itemDatabaseButton = createStyledButton("Item Database");
        JButton spriteDatabaseButton = createStyledButton("Sprite ID Database");
        JButton npcDatabaseButton = createStyledButton("NPC Database"); // Ny knapp

        itemDatabaseButton.addActionListener(e -> showItemDatabasePanel());
        spriteDatabaseButton.addActionListener(e -> showSpriteDatabasePanel());
        npcDatabaseButton.addActionListener(e -> showNpcDatabasePanel()); // Ny handling

        // Legg knappene i et eget panel for kontroll
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);

        buttonPanel.add(wrapButton(itemDatabaseButton));
        buttonPanel.add(Box.createVerticalStrut(5)); // Mellomrom mellom knappene
        buttonPanel.add(wrapButton(spriteDatabaseButton));
        buttonPanel.add(Box.createVerticalStrut(5)); // Mellomrom mellom knappene
        buttonPanel.add(wrapButton(npcDatabaseButton)); // Legg til NPC-knappen

        homePanel.add(buttonPanel, BorderLayout.CENTER); // Plasser knappene midt i panelet

        contentPanel.add(homePanel, "Home");
        contentPanel.add(itemPanel, "ItemDatabase");
        contentPanel.add(spritePanel, "SpriteDatabase");
        contentPanel.add(npcPanel, "NpcDatabase"); // Legg til NPC-panelet

        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);

        // Start med "Home"‐siden
        showHomePanel();
    }

    /**
     * Opprett en stilisert knapp.
     */
    private JButton createStyledButton(String text)
    {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 12)); // Mindre skrift
        button.setPreferredSize(new Dimension(150, 30)); // Små og avlange knapper
        button.setMaximumSize(new Dimension(150, 30)); // Hindrer layout fra å strekke dem
        button.setBackground(new Color(50, 50, 50)); // Mørk bakgrunnsfarge
        button.setForeground(Color.WHITE); // Hvit tekst
        button.setFocusPainted(false); // Fjern fokusmarkering
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY), // Ytre kant
                BorderFactory.createEmptyBorder(5, 10, 5, 10) // Indre padding
        ));
        button.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt)
            {
                button.setBackground(new Color(70, 70, 70)); // Lysere ved hover
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt)
            {
                button.setBackground(new Color(50, 50, 50)); // Tilbake til original farge
            }
        });
        return button;
    }

    /**
     * Pakk en knapp inn i et JPanel for å kontrollere størrelsen bedre.
     */
    private JPanel wrapButton(JButton button)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false); // Transparent bakgrunn
        panel.setMaximumSize(button.getMaximumSize()); // Tving fast størrelse
        panel.add(button);
        return panel;
    }

    public void showHomePanel()
    {
        cardLayout.show(contentPanel, "Home");
    }

    public void showItemDatabasePanel()
    {
        cardLayout.show(contentPanel, "ItemDatabase");
    }

    public void showSpriteDatabasePanel()
    {
        cardLayout.show(contentPanel, "SpriteDatabase");
    }

    public void showNpcDatabasePanel()
    {
        cardLayout.show(contentPanel, "NpcDatabase");
    }

    public SpriteDatabasePanel getSpritePanel()
    {
        return spritePanel;
    }

    public NpcPanel getNpcPanel()
    {
        return npcPanel;
    }

    /**
     * CustomHeader: Lager en kul overskrift med gradientfarger, mer skygge og understrek.
     */
    private static class CustomHeader extends JPanel
    {
        private final String title;
        private final String subtitle;

        public CustomHeader(String title, String subtitle)
        {
            this.title = title;
            this.subtitle = subtitle;
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Gradient for teksten
            GradientPaint gradient = new GradientPaint(0, 0, Color.BLUE, getWidth(), getHeight(), Color.MAGENTA);

            // Skygge for hovedteksten
            g2d.setFont(new Font("Verdana", Font.BOLD, 28));
            g2d.setColor(Color.BLACK); // Skyggen
            g2d.drawString(title, 14, 44); // Skygge med offset

            g2d.setPaint(gradient); // Gradient for hovedtekst
            g2d.drawString(title, 10, 40); // Hovedtekst

            // Undertekst med understrek
            g2d.setFont(new Font("Verdana", Font.PLAIN, 18));
            g2d.setColor(Color.BLACK); // Skygge for undertekst
            g2d.drawString(subtitle, 12, 72); // Skygge med offset
            g2d.setColor(new Color(192, 192, 192)); // Lys grå farge
            g2d.drawString(subtitle, 10, 70); // Undertekst

            // Understrek
            int underlineStart = 10;
            int underlineEnd = 10 + g2d.getFontMetrics().stringWidth(subtitle);
            g2d.setColor(new Color(192, 192, 192)); // Samme farge som undertekst
            g2d.drawLine(underlineStart, 75, underlineEnd, 75); // Understrek
        }

        @Override
        public Dimension getPreferredSize()
        {
            return new Dimension(300, 100); // Bare nok plass til overskriften
        }
    }
}
