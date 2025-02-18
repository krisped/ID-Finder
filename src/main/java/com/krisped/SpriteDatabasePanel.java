package com.krisped;

import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class SpriteDatabasePanel extends PluginPanel
{
    private final FinderMainPanel parentPanel;
    private final SpriteManager spriteManager;
    private final ClientThread clientThread;
    private final Client client;

    // Lagrer ID -> navnet på feltet (f.eks. "SPELL_ENTANGLE")
    private final Map<Integer, String> spriteMap = new HashMap<>();

    // GUI‐komponenter
    private final JTextField searchField;
    private final JPanel resultsPanel;
    private final JScrollPane resultsScrollPane;

    public SpriteDatabasePanel(
            Client client,
            FinderMainPanel parentPanel,
            SpriteManager spriteManager,
            ClientThread clientThread
    )
    {
        this.client = client;
        this.parentPanel = parentPanel;
        this.spriteManager = spriteManager;
        this.clientThread = clientThread;

        setLayout(new BorderLayout());

        // 1) Toppanel med tittel + søkefelt
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Sprite Database");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        topPanel.add(titleLabel);

        searchField = new JTextField("Search for sprite ID or name...");
        searchField.setPreferredSize(new Dimension(250, 30));
        searchField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                if (searchField.getText().equals("Search for sprite ID or name..."))
                {
                    searchField.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                if (searchField.getText().isEmpty())
                {
                    searchField.setText("Search for sprite ID or name...");
                }
            }
        });
        searchField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    searchSprites();
                }
            }
        });

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        searchPanel.add(searchField, BorderLayout.CENTER);

        topPanel.add(searchPanel);
        add(topPanel, BorderLayout.NORTH);

        // 2) Midtdel: resultatpanel i en scroll
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Search Results (0)"));

        resultsScrollPane = new JScrollPane(resultsPanel);
        resultsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        resultsScrollPane.setPreferredSize(new Dimension(300, 500));
        add(resultsScrollPane, BorderLayout.CENTER);

        // 3) Bunnpanel: "Back to Home"
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        JButton backButton = new JButton("Back to Home");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.addActionListener(e -> parentPanel.showHomePanel());
        bottomPanel.add(backButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Kall denne (f.eks. i startUp()) for å hente alle spriteIDs fra SpriteID‐klassen
     */
    public void loadSpriteIDs()
    {
        spriteMap.clear();

        try
        {
            for (Field field : SpriteID.class.getDeclaredFields())
            {
                if (field.getType() == int.class)
                {
                    int id = field.getInt(null);
                    spriteMap.put(id, field.getName());
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Kjører søk basert på innholdet i søkefeltet.
     * Hvis søkefeltet er tomt, vises alle sprite-IDer.
     */
    private void searchSprites()
    {
        String text = searchField.getText().trim().toLowerCase();
        List<Integer> matches = new ArrayList<>();

        // Om søkefeltet er tomt, legg til alle sprite-IDer
        if (text.isEmpty() || text.equals("search for sprite id or name..."))
        {
            matches.addAll(spriteMap.keySet());
        }
        else
        {
            // Finn matcher basert på tekst
            for (Map.Entry<Integer, String> entry : spriteMap.entrySet())
            {
                int spriteId = entry.getKey();
                String spriteName = entry.getValue().toLowerCase();

                if (spriteName.contains(text) || String.valueOf(spriteId).contains(text))
                {
                    matches.add(spriteId);
                }
            }
        }
        updateResults(matches);
    }

    /**
     * Viser radene for hver sprite som ble funnet.
     * Dersom navnet er veldig langt, justeres bredden på raden dynamisk.
     */
    private void updateResults(List<Integer> spriteIds)
    {
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Search Results (" + spriteIds.size() + ")"));
        resultsPanel.removeAll();

        // Standard minimum bredde for en rad
        final int minWidth = 280;
        // Fast høyde for hver rad
        final int rowHeight = 60;
        // Bredde for ikonet og padding
        final int iconAreaWidth = 48 + 20; // 48 for ikonet og 20 for padding

        for (Integer spriteId : spriteIds)
        {
            // Opprett radpanel
            JPanel rowPanel = new JPanel(new BorderLayout());
            rowPanel.setBackground(Color.DARK_GRAY);
            rowPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            rowPanel.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    rowPanel.setBackground(new Color(60,60,60));
                }

                @Override
                public void mouseExited(MouseEvent e)
                {
                    rowPanel.setBackground(Color.DARK_GRAY);
                }
            });

            // Ikon med padding
            JLabel iconLabel = new JLabel();
            iconLabel.setPreferredSize(new Dimension(48, 48));
            iconLabel.setOpaque(false);
            iconLabel.setBorder(new EmptyBorder(0, 10, 0, 0)); // 10px venstre padding

            spriteManager.getSpriteAsync(spriteId, 0, (img) ->
            {
                if (img != null)
                {
                    SwingUtilities.invokeLater(() -> {
                        iconLabel.setIcon(new ImageIcon(img));
                        iconLabel.revalidate();
                        iconLabel.repaint();
                    });
                }
            });

            // Tekst: Navn og ID
            String spriteName = spriteMap.get(spriteId);
            JLabel nameLabel = new JLabel(spriteName);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
            nameLabel.setForeground(Color.WHITE);

            JLabel idLabel = new JLabel("ID: " + spriteId);
            idLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            idLabel.setForeground(Color.LIGHT_GRAY);

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.add(nameLabel);
            textPanel.add(idLabel);

            // Beregn nødvendig bredde for raden basert på navnets bredde
            FontMetrics fm = nameLabel.getFontMetrics(nameLabel.getFont());
            int nameWidth = fm.stringWidth(spriteName);
            int desiredWidth = iconAreaWidth + nameWidth + 20; // 20 piksler ekstra margin
            int rowWidth = Math.max(minWidth, desiredWidth);

            rowPanel.setPreferredSize(new Dimension(rowWidth, rowHeight));
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight));

            // Legg til komponenter i raden
            rowPanel.add(iconLabel, BorderLayout.WEST);
            rowPanel.add(textPanel, BorderLayout.CENTER);

            // Legg raden til resultatpanelet
            resultsPanel.add(rowPanel);
            resultsPanel.add(Box.createVerticalStrut(5));
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
        resultsScrollPane.revalidate();
        resultsScrollPane.repaint();
    }
}
