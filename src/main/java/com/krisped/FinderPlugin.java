package com.krisped;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
        name = "[KP] ID Database",
        description = "A simple RuneLite plugin with a side panel",
        tags = {"finder", "database", "items", "sprites"}
)
public class FinderPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private SpriteManager spriteManager;

    private NavigationButton navButton;
    private FinderMainPanel mainPanel;

    @Provides
    FinderConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(FinderConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        // Opprett hovedpanelet
        mainPanel = new FinderMainPanel(this, itemManager, clientThread, client, spriteManager);

        // Last ikon til sidepanelet (bytt om du har et annet ikon)
        BufferedImage icon = loadIcon();

        // Lag navigasjonsknapp på sidepanelet
        navButton = NavigationButton.builder()
                .tooltip("Finder Plugin")
                .icon(icon)
                .priority(5)
                .panel(mainPanel)
                .build();

        // Legg til sideknappen
        clientToolbar.addNavigation(navButton);

        // Kjør på clientThread for å være sikker på at alt er lastet
        clientThread.invokeLater(() -> {
            // Finn sprite-panelet
            SpriteDatabasePanel spritePanel = mainPanel.getSpritePanel();
            if (spritePanel != null)
            {
                // Last inn spritelisten
                spritePanel.loadSpriteIDs();
            }
        });
    }

    @Override
    protected void shutDown() throws Exception
    {
        clientToolbar.removeNavigation(navButton);
    }

    private BufferedImage loadIcon()
    {
        try
        {
            return ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/side_panel_icon.png")));
        }
        catch (IOException | NullPointerException e)
        {
            log.error("Failed to load side panel icon", e);
            return null;
        }
    }
}
