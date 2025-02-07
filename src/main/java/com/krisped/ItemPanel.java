package com.krisped;

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;

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

    private final Map<Integer, Integer> alchPriceCache = new HashMap<>();
    private final Map<Integer, Integer> gePriceCache = new HashMap<>();

    public ItemPanel(FinderMainPanel parentPanel, ItemManager itemManager, ClientThread clientThread, Client client) {
        this.parentPanel = parentPanel;
        this.itemManager = itemManager;
        this.clientThread = clientThread;
        this.client = client;

        setLayout(new BorderLayout());

        // Top-panel: Overskrift, søkefelt og filter
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // Overskrift med ekstra padding (mer luft mellom overskrift og søkeboks)
        JLabel headerLabel = new JLabel("Item Database");
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        topPanel.add(headerLabel);

        // Søketekstboks – større for bedre synlighet
        searchField = new JTextField("Search for item or ID...");
        searchField.setPreferredSize(new Dimension(300, 30));
        // Fjern placeholder-tekst ved fokus
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
        // Ekstra MouseListener slik at placeholder forsvinner ved klikk
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

        // Filter for sortering
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

        // Ved filter-endring sorteres de nåværende resultatene med en gang
        filterCombo.addActionListener(e -> {
            if (!currentItems.isEmpty()) {
                updateResults(currentItems);
            }
        });

        add(topPanel, BorderLayout.NORTH);

        // Resultat-panelet med tittel som viser totalt antall resultater
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new GridLayout(0, 1));
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Search Results (0)"));
        resultsScrollPane = new JScrollPane(resultsPanel);
        resultsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        resultsScrollPane.setPreferredSize(new Dimension(300, 500));
        add(resultsScrollPane, BorderLayout.CENTER);

        // Nederre panel: Knappene plassert vertikalt over "Back to Home"
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        // Knapp-panel med vertikal ordning (knappene er litt tynnere og plassert over hverandre)
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton inventoryButton = new JButton("Get Inventory Items");
        JButton bankButton = new JButton("Get Bank Items");
        Dimension btnSize = new Dimension(150, 30);
        inventoryButton.setMaximumSize(btnSize);
        inventoryButton.setPreferredSize(btnSize);
        bankButton.setMaximumSize(btnSize);
        bankButton.setPreferredSize(btnSize);
        inventoryButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        bankButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        inventoryButton.addActionListener(e -> getInventoryItems());
        bankButton.addActionListener(e -> getBankItems());
        buttonsPanel.add(inventoryButton);
        buttonsPanel.add(Box.createVerticalStrut(5));
        buttonsPanel.add(bankButton);
        // Litt ekstra padding fra søkefeltet
        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(buttonsPanel);
        bottomPanel.add(Box.createVerticalStrut(10));

        // Tilbake-knapp
        JButton backButton = new JButton("Back to Home");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.addActionListener(e -> parentPanel.showHomePanel());
        bottomPanel.add(backButton);

        add(bottomPanel, BorderLayout.SOUTH);

        // Søket trigges ved Enter
        searchField.addKeyListener(new KeyAdapter(){
            @Override
            public void keyPressed(KeyEvent e){
                if(e.getKeyCode() == KeyEvent.VK_ENTER)
                    searchItems();
            }
        });

        // Last prisdata ved oppstart
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

    // Søket utføres ved Enter
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

    // Oppdaterer søkeresultatene – sorterer etter valgt filter og oppdaterer totalt antall
    private void updateResults(List<ItemComposition> items) {
        currentItems = new ArrayList<>(items);

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

        resultsPanel.setBorder(BorderFactory.createTitledBorder("Search Results (" + currentItems.size() + ")"));
        resultsPanel.removeAll();
        for (ItemComposition item : currentItems) {
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            itemPanel.setPreferredSize(new Dimension(280, 100));
            itemPanel.setBackground(Color.DARK_GRAY);
            itemPanel.addMouseListener(new MouseAdapter(){
                @Override
                public void mouseEntered(MouseEvent e){
                    itemPanel.setBackground(new Color(50,50,50));
                }
                @Override
                public void mouseExited(MouseEvent e){
                    itemPanel.setBackground(Color.DARK_GRAY);
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Bytt side i sidepanelet til detaljert item-info.
                    parentPanel.showItemInfoPanel(new ItemInfoPanel(item.getName(), item.getId(), itemManager, parentPanel));
                }
            });

            Image image = itemManager.getImage(item.getId());
            JLabel imageLabel = new JLabel();
            if (image != null) {
                ImageIcon icon = new ImageIcon(image);
                imageLabel.setIcon(icon);
                imageLabel.revalidate();
                imageLabel.repaint();
            }
            imageLabel.setDoubleBuffered(true);

            JLabel nameLabel = new JLabel(item.getName());
            nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

            JLabel idLabel = new JLabel("(ID: " + item.getId() + ")");
            idLabel.setFont(new Font("Arial", Font.BOLD, 13));
            idLabel.setForeground(Color.LIGHT_GRAY);

            JLabel typeLabel = new JLabel("Type: " + getItemType(item));
            typeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            typeLabel.setForeground(Color.LIGHT_GRAY);
            typeLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

            JLabel priceLabel = new JLabel(getItemPrices(item.getId(), item));
            priceLabel.setFont(new Font("Arial", Font.BOLD, 12));
            priceLabel.setForeground(Color.WHITE);
            priceLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.add(Box.createVerticalStrut(2));
            textPanel.add(nameLabel);
            textPanel.add(Box.createVerticalStrut(2));
            textPanel.add(idLabel);
            textPanel.add(Box.createVerticalStrut(5));
            textPanel.add(typeLabel);
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
        this.revalidate();
        this.repaint();
    }

    private String getItemPrices(int itemId, ItemComposition item) {
        if (isNotedItem(item) || isPlaceholderItem(item))
            return "No price available";
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
                gePriceCache.put(Integer.parseInt(key), data.getJSONObject(key).optInt("high", -1));
            }
        } catch (Exception ignored) {}
    }

    private boolean isNotedItem(ItemComposition item) { return item.getNote() != -1; }
    private boolean isPlaceholderItem(ItemComposition item) { return item.getPlaceholderTemplateId() != -1; }
    private String getItemType(ItemComposition item) { return isNotedItem(item) ? "Noted" : "Normal"; }
    private String formatPrice(int price) { return price <= 0 ? "N/A" : String.format("%,d", price); }
}
