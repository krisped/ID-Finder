package com.krisped;

import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONObject;
import java.text.DecimalFormat;

public class ItemPanel extends PluginPanel
{
    private final FinderMainPanel parentPanel;
    private final JTextField searchField;
    private final JPanel resultsPanel;
    private final JScrollPane resultsScrollPane;
    private final ItemManager itemManager;
    private final ClientThread clientThread;

    // Cache for raskere oppslag
    private final Map<Integer, String> priceCache = new HashMap<>();
    private static final int NATURE_RUNE_PRICE = 200; // Antatt pris for en nature rune (kan gjøres dynamisk)

    public ItemPanel(FinderMainPanel parentPanel, ItemManager itemManager, ClientThread clientThread)
    {
        this.parentPanel = parentPanel;
        this.itemManager = itemManager;
        this.clientThread = clientThread;
        setLayout(new BorderLayout());

        // Søkefelt
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField("Search for item or ID...");

        // Fjerner placeholder-tekst ved klikk
        searchField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                if (searchField.getText().equals("Search for item or ID..."))
                {
                    searchField.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                if (searchField.getText().isEmpty())
                {
                    searchField.setText("Search for item or ID...");
                }
            }
        });

        searchPanel.add(searchField, BorderLayout.CENTER);

        // Resultatboks
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new GridLayout(0, 1));
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Search Results"));

        resultsScrollPane = new JScrollPane(resultsPanel);
        resultsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        resultsScrollPane.setPreferredSize(new Dimension(300, 500));

        // Tilbake-knapp
        JButton backButton = new JButton("Back to Home");
        backButton.addActionListener(e -> parentPanel.showHomePanel());

        // Legg til komponenter
        add(searchPanel, BorderLayout.NORTH);
        add(resultsScrollPane, BorderLayout.CENTER);
        add(backButton, BorderLayout.SOUTH);

        // Bruk "Enter" for å søke
        searchField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    searchItems();
                }
            }
        });
    }

    private void searchItems()
    {
        String query = searchField.getText().trim().toLowerCase();

        if (query.isEmpty() || itemManager == null)
        {
            return;
        }

        clientThread.invokeLater(() ->
        {
            List<ItemComposition> items = new ArrayList<>();

            for (int id = 0; id < 30_000; id++)
            {
                ItemComposition item = itemManager.getItemComposition(id);
                if (item != null && (item.getName().toLowerCase().contains(query) || String.valueOf(id).equals(query)))
                {
                    items.add(item);
                }
            }

            SwingUtilities.invokeLater(() -> updateResults(items));
        });
    }

    private void updateResults(List<ItemComposition> items)
    {
        resultsPanel.removeAll();

        for (ItemComposition item : items)
        {
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            itemPanel.setPreferredSize(new Dimension(280, 90));
            itemPanel.setBackground(Color.DARK_GRAY);

            // Lett hover-effekt
            itemPanel.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    itemPanel.setBackground(new Color(50, 50, 50));
                }

                @Override
                public void mouseExited(MouseEvent e)
                {
                    itemPanel.setBackground(Color.DARK_GRAY);
                }
            });

            BufferedImage itemImage = itemManager.getImage(item.getId());
            JLabel imageLabel = new JLabel(new ImageIcon(itemImage));
            imageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            JLabel nameLabel = new JLabel(item.getName());
            nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

            JLabel idLabel = new JLabel("(ID: " + item.getId() + ")");
            idLabel.setFont(new Font("Arial", Font.BOLD, 13));
            idLabel.setForeground(Color.LIGHT_GRAY);

            JLabel typeLabel = new JLabel("Type: " + getItemType(item));
            typeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            typeLabel.setForeground(Color.LIGHT_GRAY);
            typeLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);

            textPanel.add(nameLabel);
            textPanel.add(idLabel);
            textPanel.add(typeLabel);

            // Vis pris bare for Normal items
            if (isNormalItem(item))
            {
                JLabel priceLabel = new JLabel("Loading price...");
                priceLabel.setFont(new Font("Arial", Font.BOLD, 12));
                priceLabel.setForeground(Color.WHITE);
                priceLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
                textPanel.add(Box.createVerticalGlue()); // For å plassere pris i bunnen
                textPanel.add(priceLabel);
                fetchPriceAsync(item.getId(), priceLabel, item.getPrice());
            }

            itemPanel.add(imageLabel, BorderLayout.WEST);
            itemPanel.add(textPanel, BorderLayout.CENTER);

            resultsPanel.add(itemPanel);
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private boolean isNormalItem(ItemComposition item)
    {
        return item.getNote() == -1 && item.getPlaceholderTemplateId() == -1;
    }

    private void fetchPriceAsync(int itemId, JLabel priceLabel, int storePrice)
    {
        if (priceCache.containsKey(itemId))
        {
            priceLabel.setText(priceCache.get(itemId));
            return;
        }

        SwingWorker<String, Void> worker = new SwingWorker<>()
        {
            @Override
            protected String doInBackground() throws Exception
            {
                return getItemPrices(itemId, storePrice);
            }

            @Override
            protected void done()
            {
                try
                {
                    String priceText = get();
                    priceCache.put(itemId, priceText);
                    priceLabel.setText(priceText);
                }
                catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private String getItemPrices(int itemId, int storePrice)
    {
        try
        {
            URL url = new URL("https://prices.runescape.wiki/api/v1/osrs/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            JSONObject data = json.getJSONObject("data");

            if (!data.has(String.valueOf(itemId)))
            {
                return "G/E: N/A | HA: " + formatHighAlch(storePrice);
            }

            JSONObject itemData = data.getJSONObject(String.valueOf(itemId));
            int gePrice = itemData.optInt("high", -1);

            return "G/E: " + formatPrice(gePrice) + " | HA: " + formatHighAlch(storePrice);
        }
        catch (Exception e)
        {
            return "G/E: N/A | HA: " + formatHighAlch(storePrice);
        }
    }

    private String formatHighAlch(int storePrice)
    {
        if (storePrice <= 0) return "N/A";
        return new DecimalFormat("###,###,###").format((int) (storePrice * 0.6));
    }

    private String formatPrice(int price)
    {
        if (price <= 0) return "N/A";

        DecimalFormat df = new DecimalFormat("#,###.#");

        if (price >= 1_000_000)
        {
            return df.format(price / 1_000_000.0) + "M";
        }
        else if (price >= 1_000)
        {
            return df.format(price / 1_000.0) + "K";
        }

        return df.format(price);
    }

    private String getItemType(ItemComposition item)
    {
        if (item.getNote() != -1) return "Noted";
        if (item.getPlaceholderTemplateId() != -1) return "Placeholder";
        return "Normal";
    }
}
