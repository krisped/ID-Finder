package com.krisped;

import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ItemPanel extends PluginPanel
{
    private final FinderMainPanel parentPanel;
    private final JTextField searchField;
    private final JPanel resultsPanel;
    private final JScrollPane resultsScrollPane;
    private final ItemManager itemManager;
    private final ClientThread clientThread;

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

        // Resultatboks (egen boks)
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new GridLayout(0, 1));
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Search Results"));

        resultsScrollPane = new JScrollPane(resultsPanel);
        resultsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        resultsScrollPane.setPreferredSize(new Dimension(300, 500)); // Økt høyde på søkeresultater

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
            JPanel itemPanel = new JPanel();
            itemPanel.setLayout(new BorderLayout());
            itemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            itemPanel.setPreferredSize(new Dimension(280, 90)); // Større søkeresultater
            itemPanel.setBackground(Color.DARK_GRAY); // Bakgrunnsfarge

            // Hover-effekt
            itemPanel.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    itemPanel.setBackground(Color.LIGHT_GRAY);
                }

                @Override
                public void mouseExited(MouseEvent e)
                {
                    itemPanel.setBackground(Color.DARK_GRAY);
                }
            });

            BufferedImage itemImage = itemManager.getImage(item.getId());
            JLabel imageLabel = new JLabel(new ImageIcon(itemImage));

            // Navn + ID + Type + Pris
            JLabel nameLabel = new JLabel(item.getName());
            JLabel idLabel = new JLabel("(ID " + item.getId() + ")");
            JLabel typeLabel = new JLabel("Type: " + getItemType(item));
            JLabel priceLabel = new JLabel(getItemPrices(item.getId())); // Henter pris

            nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
            idLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            typeLabel.setFont(new Font("Arial", Font.ITALIC, 12));
            priceLabel.setFont(new Font("Arial", Font.PLAIN, 12));

            JPanel textPanel = new JPanel(new GridLayout(4, 1));
            textPanel.add(nameLabel);
            textPanel.add(idLabel);
            textPanel.add(typeLabel);
            textPanel.add(priceLabel);

            itemPanel.add(imageLabel, BorderLayout.WEST);
            itemPanel.add(textPanel, BorderLayout.CENTER);

            resultsPanel.add(itemPanel);
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private String getItemType(ItemComposition item)
    {
        if (item.getNote() != -1)
        {
            return "Noted";
        }
        else if (item.getPlaceholderTemplateId() != -1)
        {
            return "Placeholder";
        }
        else
        {
            return "Normal";
        }
    }

    private String getItemPrices(int itemId)
    {
        // Simulert metode for henting av priser fra OSRS Wiki API
        int gePrice = getGEPrice(itemId);
        int highAlch = getHighAlchPrice(itemId);

        return "GE Price: " + formatPrice(gePrice) + " | High Alch: " + formatPrice(highAlch);
    }

    private int getGEPrice(int itemId)
    {
        // Simulert API-kall, erstatt med faktisk OSRS Wiki API-integrasjon
        return (int) (Math.random() * 100000);
    }

    private int getHighAlchPrice(int itemId)
    {
        // Simulert API-kall, erstatt med faktisk OSRS Wiki API-integrasjon
        return (int) (Math.random() * 50000);
    }

    private String formatPrice(int price)
    {
        if (price >= 1_000_000)
        {
            return (price / 1_000_000) + "M";
        }
        else if (price >= 1_000)
        {
            return (price / 1_000) + "K";
        }
        return String.valueOf(price);
    }
}
