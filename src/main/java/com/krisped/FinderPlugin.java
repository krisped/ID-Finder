package com.krisped;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.game.ItemManager;
import net.runelite.client.callback.ClientThread;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

@Slf4j
@PluginDescriptor(
        name = "Finder Plugin",
        description = "A simple RuneLite plugin with a side panel",
        tags = {"finder", "database", "items"}
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

    private NavigationButton navButton;
    private FinderMainPanel mainPanel;

    @Override
    protected void startUp() throws Exception
    {
        mainPanel = new FinderMainPanel(this, itemManager, clientThread, client);

        BufferedImage icon = loadIcon();

        navButton = NavigationButton.builder()
                .tooltip("Finder Plugin")
                .icon(icon)
                .panel(mainPanel)
                .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception
    {
        clientToolbar.removeNavigation(navButton);
    }

    @Provides
    FinderConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(FinderConfig.class);
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
