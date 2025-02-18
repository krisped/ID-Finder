package com.krisped;

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemPanel extends PluginPanel {
    private final FinderMainPanel parentPanel;
    private final JTextField searchField;
    private final JPanel resultsPanel;
    private final JScrollPane resultsScrollPane;
    private final ItemManager itemManager;
    private final ClientThread clientThread;
    private final Client client;
    private final JComboBox<String> filterCombo; // Filter for sortering

    // Lagrer de siste søkeresultatene for re-sortering
    private List<ItemComposition> currentItems = new ArrayList<>();

    private final Map<Integer, Integer> alchPriceCache = new LinkedHashMap<>();
    private final Map<Integer, Integer> gePriceCache = new LinkedHashMap<>();
    private final Map<Integer, Integer> day30PriceCache = new LinkedHashMap<>();
    private final Map<Integer, Integer> volumeCache = new LinkedHashMap<>();

    // Enkel cache for wiki-extrainfo
    private final Map<String, String> wikiInfoCache = new LinkedHashMap<>();

    public ItemPanel(FinderMainPanel parentPanel, ItemManager itemManager, ClientThread clientThread, Client client) {
        this.parentPanel = parentPanel;
        this.itemManager = itemManager;
        this.clientThread = clientThread;
        this.client = client;

        setLayout(new BorderLayout());

        // Top-panel: Overskrift, søkefelt og filter
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel headerLabel = new JLabel("Item Database");
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        topPanel.add(headerLabel);

        searchField = new JTextField("Search for item or ID...");
        searchField.setPreferredSize(new Dimension(300, 30));
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Search for item or ID..."))
                    searchField.setText("");
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty())
                    searchField.setText("Search for item or ID...");
            }
        });
        searchField.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent e) {
                if (searchField.getText().equals("Search for item or ID..."))
                    searchField.setText("");
            }
        });
        JPanel searchFieldPanel = new JPanel(new BorderLayout());
        searchFieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        searchFieldPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(searchFieldPanel);

        filterCombo = new JComboBox<>(new String[]{
                "Sort by: None",
                "Price: High to Low",
                "Price: Low to High",
                "High Alch: High to Low",
                "High Alch: Low to High",
                "ID: High to Low",
                "ID: Low to High",
                "Name: A-Z",
                "Name: Z-A"
        });
        filterCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        filterCombo.setMaximumSize(new Dimension(200, 25));
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        filterPanel.add(filterCombo);
        topPanel.add(filterPanel);

        filterCombo.addActionListener(e -> {
            if (!currentItems.isEmpty()) {
                updateResults(currentItems);
            }
        });

        add(topPanel, BorderLayout.NORTH);

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new GridLayout(0, 1));
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Search Results (0)"));
        resultsScrollPane = new JScrollPane(resultsPanel);
        resultsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        resultsScrollPane.setPreferredSize(new Dimension(300, 500));
        add(resultsScrollPane, BorderLayout.CENTER);

        // Bottom panel: Inneholder knappene
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension btnSize = new Dimension(150, 30);

        JButton inventoryButton = new JButton("Get Inventory Items");
        inventoryButton.setFont(new Font("Arial", Font.PLAIN, 12));
        inventoryButton.setMaximumSize(btnSize);
        inventoryButton.setPreferredSize(btnSize);
        inventoryButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        inventoryButton.addActionListener(e -> getInventoryItems());

        JButton equipmentButton = new JButton("Get Equipment Items");
        equipmentButton.setFont(new Font("Arial", Font.PLAIN, 12));
        equipmentButton.setMaximumSize(btnSize);
        equipmentButton.setPreferredSize(btnSize);
        equipmentButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        equipmentButton.addActionListener(e -> getEquipmentItems());

        JButton bankButton = new JButton("Get Bank Items");
        bankButton.setFont(new Font("Arial", Font.PLAIN, 12));
        bankButton.setMaximumSize(btnSize);
        bankButton.setPreferredSize(btnSize);
        bankButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        bankButton.addActionListener(e -> getBankItems());

        buttonsPanel.add(wrapButton(inventoryButton));
        buttonsPanel.add(Box.createVerticalStrut(5));
        buttonsPanel.add(wrapButton(equipmentButton));
        buttonsPanel.add(Box.createVerticalStrut(5));
        buttonsPanel.add(wrapButton(bankButton));
        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(buttonsPanel);
        bottomPanel.add(Box.createVerticalStrut(10));
        JButton backButton = new JButton("Back to Home");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setFont(new Font("Arial", Font.PLAIN, 12));
        backButton.addActionListener(e -> parentPanel.showHomePanel());
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);

        searchField.addKeyListener(new KeyAdapter(){
            @Override
            public void keyPressed(KeyEvent e){
                if(e.getKeyCode() == KeyEvent.VK_ENTER)
                    searchItems();
            }
        });

        loadHighAlchPrices();
        loadGEPrices();
    }

    private void getInventoryItems() {
        clientThread.invokeLater(() -> {
            ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
            if (inventory == null)
                return;
            List<ItemComposition> items = new ArrayList<>();
            for (Item item : inventory.getItems()) {
                if (item.getId() > 0)
                    items.add(itemManager.getItemComposition(item.getId()));
            }
            SwingUtilities.invokeLater(() -> updateResults(items));
        });
    }

    private void getEquipmentItems() {
        clientThread.invokeLater(() -> {
            ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
            if (equipment == null)
                return;
            List<ItemComposition> items = new ArrayList<>();
            for (Item item : equipment.getItems()) {
                if (item.getId() > 0)
                    items.add(itemManager.getItemComposition(item.getId()));
            }
            SwingUtilities.invokeLater(() -> updateResults(items));
        });
    }

    private void getBankItems() {
        clientThread.invokeLater(() -> {
            ItemContainer bank = client.getItemContainer(InventoryID.BANK);
            if (bank == null)
                return;
            List<ItemComposition> items = new ArrayList<>();
            for (Item item : bank.getItems()) {
                if (item.getId() > 0)
                    items.add(itemManager.getItemComposition(item.getId()));
            }
            SwingUtilities.invokeLater(() -> updateResults(items));
        });
    }

    private void searchItems(){
        String query = searchField.getText().trim().toLowerCase();
        if(query.isEmpty() || itemManager == null)
            return;
        clientThread.invokeLater(() -> {
            List<ItemComposition> items = new ArrayList<>();
            for(int id = 0; id < 30000; id++){
                ItemComposition item = itemManager.getItemComposition(id);
                if(item != null && (item.getName().toLowerCase().contains(query) || String.valueOf(id).equals(query)))
                    items.add(item);
            }
            SwingUtilities.invokeLater(() -> updateResults(items));
        });
    }

    /**
     * Oppdaterer søkeresultatene.
     * Her grupperes like items slik at for eksempel 20 Shark vises som "Shark (x20)",
     * totalt antall items vises også i tittelen.
     * I tillegg vises en ekstra linje med info hentet fra OSRS Wiki om itemet.
     */
    private void updateResults(List<ItemComposition> items) {
        currentItems = new ArrayList<>(items);
        // Sortering basert på valgt filter
        String selectedSort = (String) filterCombo.getSelectedItem();
        if(selectedSort != null) {
            switch(selectedSort) {
                case "Price: High to Low":
                    currentItems.sort((a, b) -> Integer.compare(
                            gePriceCache.getOrDefault(b.getId(), 0),
                            gePriceCache.getOrDefault(a.getId(), 0)
                    ));
                    break;
                case "Price: Low to High":
                    currentItems.sort((a, b) -> Integer.compare(
                            gePriceCache.getOrDefault(a.getId(), 0),
                            gePriceCache.getOrDefault(b.getId(), 0)
                    ));
                    break;
                case "High Alch: High to Low":
                    currentItems.sort((a, b) -> Integer.compare(
                            alchPriceCache.getOrDefault(b.getId(), 0),
                            alchPriceCache.getOrDefault(a.getId(), 0)
                    ));
                    break;
                case "High Alch: Low to High":
                    currentItems.sort((a, b) -> Integer.compare(
                            alchPriceCache.getOrDefault(a.getId(), 0),
                            alchPriceCache.getOrDefault(b.getId(), 0)
                    ));
                    break;
                case "ID: High to Low":
                    currentItems.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
                    break;
                case "ID: Low to High":
                    currentItems.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
                    break;
                case "Name: A-Z":
                    currentItems.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                    break;
                case "Name: Z-A":
                    currentItems.sort((a, b) -> b.getName().compareToIgnoreCase(a.getName()));
                    break;
                default:
                    break;
            }
        }

        // Gruppér items basert på ID
        Map<Integer, Integer> countMap = new LinkedHashMap<>();
        Map<Integer, ItemComposition> compMap = new LinkedHashMap<>();
        for (ItemComposition item : currentItems) {
            int id = item.getId();
            countMap.put(id, countMap.getOrDefault(id, 0) + 1);
            if (!compMap.containsKey(id)) {
                compMap.put(id, item);
            }
        }
        // Beregn totalt antall items (inkludert duplikater)
        int totalCount = 0;
        for (Integer count : countMap.values()) {
            totalCount += count;
        }
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Search Results (" + totalCount + ")"));
        resultsPanel.removeAll();

        // For hver gruppe opprettes en rad
        for (Map.Entry<Integer, ItemComposition> entry : compMap.entrySet()) {
            int itemId = entry.getKey();
            ItemComposition item = entry.getValue();
            int count = countMap.get(itemId);

            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            itemPanel.setPreferredSize(new Dimension(280, 85));
            itemPanel.setBackground(Color.DARK_GRAY);

            // Ekstra venstrepadding for bildet
            JLabel imageLabel = new JLabel();
            imageLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
            new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    Image image = itemManager.getImage(item.getId());
                    return image != null ? new ImageIcon(image) : null;
                }
                @Override
                protected void done() {
                    try {
                        ImageIcon icon = get();
                        if (icon != null) {
                            imageLabel.setIcon(icon);
                            imageLabel.revalidate();
                            imageLabel.repaint();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.execute();

            String nameText = item.getName() + (count > 1 ? " (x" + count + ")" : "");
            JLabel nameLabel = new JLabel(nameText);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

            JLabel idLabel = new JLabel("(ID: " + item.getId() + ")");
            idLabel.setFont(new Font("Arial", Font.BOLD, 11));
            idLabel.setForeground(Color.LIGHT_GRAY);
            idLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

            JLabel typeLabel = new JLabel("Type: " + getItemType(item));
            typeLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            typeLabel.setForeground(Color.LIGHT_GRAY);
            typeLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

            // Hent ekstra info fra wiki asynkront
            JLabel extraInfoLabel = new JLabel();
            extraInfoLabel.setFont(new Font("Arial", Font.PLAIN, 9));
            extraInfoLabel.setForeground(Color.LIGHT_GRAY);
            extraInfoLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return fetchWikiExtraInfo(item.getName());
                }
                @Override
                protected void done() {
                    try {
                        String info = get();
                        extraInfoLabel.setText(info);
                        extraInfoLabel.revalidate();
                        extraInfoLabel.repaint();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.execute();

            JLabel priceLabel = new JLabel(getItemPrices(item.getId(), item));
            priceLabel.setFont(new Font("Arial", Font.BOLD, 11));
            priceLabel.setForeground(Color.WHITE);
            priceLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.add(Box.createVerticalStrut(2));
            textPanel.add(nameLabel);
            textPanel.add(Box.createVerticalStrut(2));
            textPanel.add(idLabel);
            textPanel.add(Box.createVerticalStrut(2));
            textPanel.add(typeLabel);
            textPanel.add(extraInfoLabel);
            textPanel.add(Box.createVerticalGlue());
            textPanel.add(priceLabel);
            textPanel.add(Box.createVerticalStrut(2));

            itemPanel.add(imageLabel, BorderLayout.WEST);
            itemPanel.add(textPanel, BorderLayout.CENTER);

            resultsPanel.add(itemPanel);
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
        resultsScrollPane.getViewport().revalidate();
        resultsScrollPane.getViewport().repaint();
        SwingUtilities.invokeLater(() -> resultsScrollPane.repaint());
    }

    private String getItemPrices(int itemId, ItemComposition item) {
        if (isNotedItem(item) || isPlaceholderItem(item))
            return "";
        int gePrice = gePriceCache.getOrDefault(itemId, -1);
        int haPrice = alchPriceCache.getOrDefault(itemId, -1);
        return "G/E: " + formatPrice(gePrice) + " | HA: " + formatPrice(haPrice);
    }

    private void loadHighAlchPrices() {
        try {
            URL url = new URL("https://prices.runescape.wiki/api/v1/osrs/mapping");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JSONArray array = new JSONArray(reader.readLine());
            reader.close();
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                alchPriceCache.put(item.getInt("id"), item.optInt("highalch", -1));
            }
        } catch (Exception ignored) {}
    }

    private void loadGEPrices() {
        try {
            URL url = new URL("https://prices.runescape.wiki/api/v1/osrs/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JSONObject data = new JSONObject(reader.readLine()).getJSONObject("data");
            reader.close();
            for (String key : data.keySet()) {
                int id = Integer.parseInt(key);
                JSONObject obj = data.getJSONObject(key);
                gePriceCache.put(id, obj.optInt("high", -1));
                day30PriceCache.put(id, obj.optInt("day30", -1));
                volumeCache.put(id, obj.optInt("volume", -1));
            }
        } catch (Exception ignored) {}
    }

    private boolean isNotedItem(ItemComposition item) { return item.getNote() != -1; }
    private boolean isPlaceholderItem(ItemComposition item) { return item.getPlaceholderTemplateId() != -1; }
    private String getItemType(ItemComposition item) { return isNotedItem(item) ? "Noted" : "Normal"; }
    private String formatPrice(int price) { return price <= 0 ? "N/A" : String.format("%,d gp", price); }

    /**
     * Henter ekstra info fra OSRS Wiki for et gitt item (basert på item-navn).
     * Returnerer for eksempel "(Untradable, Quest Item)" for items der prikkene er satt.
     */
    private String fetchWikiExtraInfo(String itemName) {
        try {
            if (wikiInfoCache.containsKey(itemName)) {
                return wikiInfoCache.get(itemName);
            }
            String encodedName = URLEncoder.encode(itemName, "UTF-8");
            String urlStr = "https://oldschool.runescape.wiki/api.php?action=query&format=json&titles=" + encodedName + "&prop=pageprops";
            URL url = new URL(urlStr);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder jsonResult = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonResult.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(jsonResult.toString());
            JSONObject query = json.getJSONObject("query");
            JSONObject pages = query.getJSONObject("pages");
            String extraInfo = "";
            for (String key : pages.keySet()) {
                JSONObject page = pages.getJSONObject(key);
                if (page.has("pageprops")) {
                    JSONObject props = page.getJSONObject("pageprops");
                    if (props.has("infobox_item")) {
                        String info = props.getString("infobox_item").toLowerCase();
                        if (!info.contains("tradeable") || info.contains("no")) {
                            extraInfo += "Untradable";
                        }
                        if (!info.contains("drop") && !info.contains("sell")) {
                            if (!extraInfo.isEmpty()) extraInfo += ", ";
                            extraInfo += "Quest Item";
                        }
                    }
                }
            }
            if (!extraInfo.isEmpty()) {
                extraInfo = "(" + extraInfo + ")";
            }
            wikiInfoCache.put(itemName, extraInfo);
            return extraInfo;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Pakk en knapp inn i et JPanel for å kontrollere størrelsen bedre.
     */
    private JPanel wrapButton(JButton button) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.setMaximumSize(button.getMaximumSize());
        panel.add(button);
        return panel;
    }
}
