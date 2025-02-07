package com.krisped;

import net.runelite.api.Client;
import net.runelite.api.NPCComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.PluginPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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

        // Øverst: Søkefelt
        JPanel topPanel = new JPanel(new BorderLayout());
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
        topPanel.add(searchField, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Resultatpanel med scrollbar
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBorder(createTitledBorder("Search Results (0)"));

        JScrollPane scrollPane = new JScrollPane(resultsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(460, 650));
        add(scrollPane, BorderLayout.CENTER);

        // Tilbake-knapp nederst
        JButton backButton = new JButton("Back to Home");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.addActionListener(e -> parentPanel.showHomePanel());
        add(backButton, BorderLayout.SOUTH);
    }

    private void searchNpc()
    {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty() || query.equals("search for npc name or id..."))
        {
            clearResults();
            return;
        }

        // Vis "Searching..."
        resultsPanel.removeAll();
        resultsPanel.setBorder(createTitledBorder("Searching..."));
        resultsPanel.revalidate();
        resultsPanel.repaint();

        SwingWorker<List<NPCComposition>, Void> worker = new SwingWorker<List<NPCComposition>, Void>()
        {
            @Override
            protected List<NPCComposition> doInBackground() throws Exception
            {
                CountDownLatch latch = new CountDownLatch(1);
                List<NPCComposition> matches = new ArrayList<>();

                // Henter NPC-definisjoner på clientThread
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
                    latch.countDown();
                });
                latch.await();
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

    private void updateResults(List<NPCComposition> matches)
    {
        resultsPanel.removeAll();
        resultsPanel.setBorder(createTitledBorder("Search Results (" + matches.size() + ")"));

        for (NPCComposition npc : matches)
        {
            JPanel npcPanel = new JPanel(new BorderLayout());
            npcPanel.setOpaque(false);
            npcPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
            npcPanel.setPreferredSize(new Dimension(440, 100)); // Litt større panel

            // Placeholder for bilde
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

            // Asynkron nedlasting av bilde
            SwingWorker<ImageIcon, Void> imageWorker = new SwingWorker<ImageIcon, Void>()
            {
                @Override
                protected ImageIcon doInBackground() throws Exception
                {
                    // Prøver først med Wiki API for å hente _fullstendig_ bilde (ikke kun hodet)
                    ImageIcon icon = fetchNpcImageViaWikiAPI(npc.getName());
                    if (icon == null)
                    {
                        // Fallback: prøv varianter med gjetting av filnavn
                        icon = fetchNpcImageByVariants(npc.getName());
                    }
                    return icon;
                }
                @Override
                protected void done()
                {
                    try
                    {
                        ImageIcon icon = get();
                        if (icon != null)
                        {
                            // Skaler bildet med bevart aspekt
                            Image scaled = scalePreservingRatio(icon.getImage(), 80, 80);
                            imageLabel.setIcon(new ImageIcon(scaled));
                        }
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
            };
            imageWorker.execute();
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

    /**
     * Bruker MediaWiki API med piprop=original for å hente den fullstendige bildeadressen.
     * Dette skal gi riktig bilde (f.eks. Museum_guard.png) i stedet for bare et beskåret head-thumb.
     */
    private ImageIcon fetchNpcImageViaWikiAPI(String npcName)
    {
        try
        {
            String encodedName = URLEncoder.encode(npcName, "UTF-8");
            String apiUrl = "https://oldschool.runescape.wiki/api.php?action=query&format=json&titles="
                    + encodedName + "&prop=pageimages&piprop=original";
            URL url = new URL(apiUrl);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
            StringBuilder jsonResult = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                jsonResult.append(line);
            }
            reader.close();
            String json = jsonResult.toString();

            int index = json.indexOf("\"source\":\"");
            if (index != -1)
            {
                int start = index + 10;
                int end = json.indexOf("\"", start);
                if (end != -1)
                {
                    String imageUrl = json.substring(start, end);
                    BufferedImage fetched = ImageIO.read(new URL(imageUrl));
                    if (fetched != null)
                    {
                        return new ImageIcon(fetched);
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Fallback-metode: prøver flere varianter av filnavn (f.eks. med _(1), _(2) osv.)
     */
    private ImageIcon fetchNpcImageByVariants(String npcName)
    {
        String baseName = npcName.replace(" ", "_");
        for (int i = 0; i < 4; i++)
        {
            String testName = (i == 0)
                    ? baseName + ".png"
                    : baseName + "_(" + i + ").png";
            String urlStr = "https://oldschool.runescape.wiki/images/thumb/"
                    + testName
                    + "/120px-"
                    + testName;
            try
            {
                URL imageUrl = new URL(urlStr);
                BufferedImage fetched = ImageIO.read(imageUrl);
                if (fetched != null)
                {
                    return new ImageIcon(fetched);
                }
            }
            catch (IOException ignored)
            {
                // Prøver neste variant
            }
        }
        return null;
    }

    private ImageIcon getDefaultNpcIcon()
    {
        try
        {
            URL defaultImageUrl = getClass().getClassLoader().getResource("default_npc_image.png");
            if (defaultImageUrl != null)
            {
                BufferedImage img = ImageIO.read(defaultImageUrl);
                if (img != null)
                {
                    return new ImageIcon(img);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        // Enkel placeholder med "?"
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

    private Image scalePreservingRatio(Image src, int maxWidth, int maxHeight)
    {
        int originalWidth = src.getWidth(null);
        int originalHeight = src.getHeight(null);
        if (originalWidth <= 0 || originalHeight <= 0)
        {
            return src;
        }
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double scale = Math.min(widthRatio, heightRatio);
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);
        return src.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
    }

    private TitledBorder createTitledBorder(String title)
    {
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleColor(Color.WHITE);
        return border;
    }
}
