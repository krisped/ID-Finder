package com.krisped;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.PluginPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NpcPanel extends PluginPanel
{
    private final FinderMainPanel parentPanel;
    private final Client client;
    private final ClientThread clientThread;
    private final JTextField searchField;
    private final JPanel resultsPanel;

    public NpcPanel(Client client, ClientThread clientThread, FinderMainPanel parentPanel)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.parentPanel = parentPanel;
        setLayout(new BorderLayout());

        // Header (samme som i Sprite og Item)
        CustomHeader header = new CustomHeader("KrisPed's", "NPC Database");
        add(header, BorderLayout.NORTH);

        // Midtpanel med søkefelt og resultatområde
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());

        searchField = new JTextField("Search for NPC name or ID...");
        searchField.setPreferredSize(new Dimension(450, 30));
        searchField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                if (searchField.getText().equals("Search for NPC name or ID..."))
                {
                    searchField.setText("");
                }
            }
            @Override
            public void focusLost(FocusEvent e)
            {
                if (searchField.getText().isEmpty())
                {
                    searchField.setText("Search for NPC name or ID...");
                }
            }
        });
        searchField.addActionListener(e -> searchNpc());
        centerPanel.add(searchField, BorderLayout.NORTH);

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBorder(createTitledBorder("Search Results (0)"));

        JScrollPane scrollPane = new JScrollPane(resultsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(460, 650));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Nederst: en panel med "Get Nearby NPC's" og "Back to Home" knapper
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));

        JButton nearbyButton = new JButton("Get Nearby NPC's");
        nearbyButton.setFont(new Font("Arial", Font.PLAIN, 12));
        nearbyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        nearbyButton.addActionListener(e -> getNearbyNpcs());
        southPanel.add(nearbyButton);
        southPanel.add(Box.createVerticalStrut(10));

        JButton backButton = new JButton("Back to Home");
        backButton.setFont(new Font("Arial", Font.PLAIN, 12));
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.addActionListener(e -> parentPanel.showHomePanel());
        southPanel.add(backButton);

        add(southPanel, BorderLayout.SOUTH);
    }

    private void searchNpc()
    {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty() || query.equals("search for npc name or id..."))
        {
            clearResults();
            return;
        }

        resultsPanel.removeAll();
        resultsPanel.setBorder(createTitledBorder("Searching..."));
        resultsPanel.revalidate();
        resultsPanel.repaint();

        SwingWorker<List<NPCComposition>, Void> worker = new SwingWorker<List<NPCComposition>, Void>()
        {
            @Override
            protected List<NPCComposition> doInBackground() throws Exception
            {
                List<NPCComposition> matches = new ArrayList<>();
                clientThread.invokeLater(() ->
                {
                    for (int i = 0; i < 10000; i++)
                    {
                        NPCComposition npc = client.getNpcDefinition(i);
                        if (npc != null)
                        {
                            String nameLower = npc.getName().toLowerCase();
                            if (nameLower.contains(query) || String.valueOf(i).contains(query))
                            {
                                matches.add(npc);
                            }
                        }
                    }
                });
                Thread.sleep(500);
                return matches;
            }

            @Override
            protected void done()
            {
                try
                {
                    List<NPCComposition> matches = get();
                    updateResults(matches);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    clearResults();
                }
            }
        };
        worker.execute();
    }

    /**
     * Henter NPC-er i nærheten via client.getNpcs() som nå returnerer en List<NPC>
     */
    private void getNearbyNpcs()
    {
        SwingWorker<List<NPCComposition>, Void> worker = new SwingWorker<List<NPCComposition>, Void>()
        {
            @Override
            protected List<NPCComposition> doInBackground() throws Exception
            {
                List<NPCComposition> nearby = new ArrayList<>();
                clientThread.invokeLater(() ->
                {
                    List<NPC> npcs = client.getNpcs();
                    if (npcs != null)
                    {
                        for (NPC npc : npcs)
                        {
                            if (npc != null)
                            {
                                nearby.add(npc.getComposition());
                            }
                        }
                    }
                });
                Thread.sleep(300);
                return nearby;
            }
            @Override
            protected void done()
            {
                try
                {
                    List<NPCComposition> nearby = get();
                    updateResults(nearby);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void updateResults(List<NPCComposition> matches)
    {
        resultsPanel.removeAll();
        resultsPanel.setBorder(createTitledBorder("Search Results (" + matches.size() + ")"));

        for (NPCComposition npc : matches)
        {
            JPanel npcPanel = new JPanel(new BorderLayout());
            npcPanel.setOpaque(false);
            npcPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
            npcPanel.setPreferredSize(new Dimension(440, 100));

            // Placeholder for NPC-bilde
            JLabel imageLabel = new JLabel();
            imageLabel.setPreferredSize(new Dimension(80, 80));
            imageLabel.setIcon(getDefaultNpcIcon());

            // NPC-navn og ID
            JLabel nameAndIdLabel = new JLabel("<html><b>" + npc.getName() + "</b><br>(ID: " + npc.getId() + ")</html>");
            nameAndIdLabel.setFont(new Font("Arial", Font.BOLD, 14));
            nameAndIdLabel.setForeground(Color.WHITE);

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.add(nameAndIdLabel);

            npcPanel.add(imageLabel, BorderLayout.WEST);
            npcPanel.add(textPanel, BorderLayout.CENTER);
            resultsPanel.add(npcPanel);
            resultsPanel.add(Box.createVerticalStrut(5));
        }
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private void clearResults()
    {
        resultsPanel.removeAll();
        resultsPanel.setBorder(createTitledBorder("Search Results (0)"));
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private TitledBorder createTitledBorder(String title)
    {
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleColor(Color.WHITE);
        return border;
    }

    /**
     * Enkel CustomHeader brukt i andre paneler.
     */
    private static class CustomHeader extends JPanel
    {
        private final String title;
        private final String subtitle;

        public CustomHeader(String title, String subtitle)
        {
            this.title = title;
            this.subtitle = subtitle;
            setPreferredSize(new Dimension(300, 100));
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gradient = new GradientPaint(0, 0, Color.BLUE, getWidth(), getHeight(), Color.MAGENTA);
            g2d.setPaint(gradient);
            g2d.setFont(new Font("Verdana", Font.BOLD, 28));
            g2d.drawString(title, 10, 40);
            g2d.setFont(new Font("Verdana", Font.PLAIN, 18));
            g2d.drawString(subtitle, 10, 70);
        }
    }

    /**
     * Returnerer et standard NPC-ikon. Hvis ressursen "/default_npc_image.png" ikke finnes,
     * opprettes en enkel placeholder.
     */
    private ImageIcon getDefaultNpcIcon() {
        try {
            URL defaultImageUrl = getClass().getResource("/default_npc_image.png");
            if (defaultImageUrl != null) {
                BufferedImage img = ImageIO.read(defaultImageUrl);
                if (img != null) {
                    return new ImageIcon(img);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        BufferedImage placeholder = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();
        g2d.setColor(Color.GRAY);
        g2d.fillRect(0, 0, 80, 80);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString("?", 25, 55);
        g2d.dispose();
        return new ImageIcon(placeholder);
    }
}
