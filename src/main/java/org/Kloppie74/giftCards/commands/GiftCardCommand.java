package org.Kloppie74.giftCards.commands;

import me.chrommob.minestore.api.Registries;
import me.chrommob.minestore.api.interfaces.commands.CommonConsoleUser;
import me.chrommob.minestore.api.interfaces.user.AbstractUser;
import org.Kloppie74.giftCards.internal.Database;
import org.Kloppie74.giftCards.internal.MSG;
import org.Kloppie74.giftCards.service.GiftCardService;
import org.Kloppie74.giftCards.config.MineStoreConfigLoader;
import org.Kloppie74.giftCards.util.ServiceRegistry;
import org.Kloppie74.giftCards.util.CodeGenerator;
import org.Kloppie74.giftCards.util.DateUtil;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

/**
 * Command to create giftcards.
 * Players need permission "MineStore.Giftcards.create". Console allowed.
 *
 * Uses MineStoreConfigLoader.t(...) to obtain messages from the MineStore language file.
 */
@SuppressWarnings("unused")
public class GiftCardCommand {

    private Database getDatabase() { return ServiceRegistry.getDatabase(); }
    private MineStoreConfigLoader getConfig() { return ServiceRegistry.getConfigLoader(); }
    private GiftCardService getService() { return ServiceRegistry.getGiftCardService(); }

    @Command("giftcard create <target> <amount>")
    public void createGiftCard(AbstractUser sender, @Argument("target") String targetName, @Argument("amount") double amount) {
        Locale locale = Locale.ENGLISH;

        MineStoreConfigLoader cfg = getConfig();

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.commonUser().sendMessage(MSG.chatColors(cfg.t("player_not_found", targetName)));
            return;
        }

        // Permission check for players
        if (sender.platformObject() instanceof Player) {
            Player p = (Player) sender.platformObject();
            if (!p.hasPermission("MineStore.Giftcards.create")) {
                sender.commonUser().sendMessage(MSG.chatColors(cfg.t("no_permission_create", "You do not have permission to create giftcards.")));
                return;
            }
        }

        if (amount <= 0) {
            sender.commonUser().sendMessage(MSG.chatColors(cfg.t("invalid_amount", "Invalid amount")));
            return;
        }

        String note;
        if (!(sender.commonUser() instanceof CommonConsoleUser) && sender.platformObject() instanceof Player) {
            Player creator = (Player) sender.platformObject();
            note = "Created by " + creator.getName();
        } else {
            note = "Created by Console";
        }

        String username = target.getName();
        String code = CodeGenerator.generateCode(12);
        String expireDate = DateUtil.expireDaysFromNow(30);

        if (cfg == null) {
            sender.commonUser().sendMessage(MSG.chatColors(cfg.t("config_missing", "Configuration missing")));
            return;
        }

        String storeUrl = cfg.getStoreUrl();
        boolean apiKeyEnabled = cfg.isApiKeyEnabled();
        String apiKey = cfg.getApiKey();

        if (storeUrl.isEmpty() || (apiKeyEnabled && (apiKey == null || apiKey.isEmpty()))) {
            sender.commonUser().sendMessage(MSG.chatColors(cfg.t("api_config_missing", "API configuration missing")));
            return;
        }

        final String apiUrl = cfg.buildCreateGiftCardUrl();

        new Thread(() -> {
            boolean success = getService().createGiftCard(apiUrl, code, amount, expireDate, note, username);
            if (success) {
                UUID targetUuid = target.getUniqueId();
                Database db = getDatabase();
                if (db != null && targetUuid != null) {
                    db.addGiftCard(targetUuid, code);
                }

                String creatorMsg = MSG.chatColors(cfg.t("giftcard_created", code, amount));
                String targetMsg = MSG.chatColors(cfg.t("giftcard_received", code, amount));

                sender.commonUser().sendMessage(creatorMsg);

                try {
                    if (Registries.USER_GETTER.get() != null) {
                        me.chrommob.minestore.api.interfaces.user.AbstractUser au = Registries.USER_GETTER.get().get(target.getName());
                        if (au != null) {
                            au.commonUser().sendMessage(targetMsg);
                            return;
                        }
                    }
                } catch (Exception ignored) {}

                try {
                    target.sendMessage(targetMsg);
                } catch (Exception ignored) {}
            } else {
                sender.commonUser().sendMessage(MSG.chatColors(cfg.t("unknown_error_giftcard", "An unknown error occurred")));
            }
        }).start();
    }
}