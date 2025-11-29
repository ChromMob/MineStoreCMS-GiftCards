package org.Kloppie74.giftCards;

import me.chrommob.minestore.api.generic.MineStoreAddon;
import org.Kloppie74.giftCards.commands.GiftCardCommand;
import org.Kloppie74.giftCards.commands.MyGiftCardsCommand;
import org.Kloppie74.giftCards.config.MineStoreConfigLoader;
import org.Kloppie74.giftCards.internal.Database;
import org.Kloppie74.giftCards.service.GiftCardService;
import org.Kloppie74.giftCards.util.ServiceRegistry;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Main addon class — wires up singletons in ServiceRegistry so command instances
 * (which may be constructed by MineStore's command framework) always have access
 * to dependencies.
 */
@SuppressWarnings("unused")
public class GiftCardsAddon extends MineStoreAddon {

    private Database database;
    private MineStoreConfigLoader configLoader;
    private GiftCardService giftCardService;

    @Override
    public void onEnable() {
        database = new Database(getDataFolderSafe());
        configLoader = new MineStoreConfigLoader();
        giftCardService = new GiftCardService();

        ServiceRegistry.setDatabase(database);
        ServiceRegistry.setConfigLoader(configLoader);
        ServiceRegistry.setGiftCardService(giftCardService);
    }

    @Override
    public String getName() {
        return "GiftCards";
    }

    @Override
    public List<Object> getCommands() {
        List<Object> commands = new ArrayList<>();
        commands.add(new GiftCardCommand());
        commands.add(new MyGiftCardsCommand());
        return commands;
    }

    @Override
    public List<me.chrommob.minestore.libs.me.chrommob.config.ConfigManager.ConfigKey<?>> getConfigKeys() {
        return new ArrayList<>();
    }

    private File getDataFolderSafe() {
        org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("GiftCards");
        if (plugin != null) {
            return plugin.getDataFolder();
        }
        return new File("plugins" + File.separator + "MineStore" + File.separator + "addons" + File.separator + "GiftCards");
    }
}