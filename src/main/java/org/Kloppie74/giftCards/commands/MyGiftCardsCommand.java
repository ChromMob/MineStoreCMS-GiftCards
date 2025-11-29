package org.Kloppie74.giftCards.commands;

import me.chrommob.minestore.api.interfaces.commands.CommonConsoleUser;
import me.chrommob.minestore.api.interfaces.user.AbstractUser;
import org.Kloppie74.giftCards.internal.Database;
import org.Kloppie74.giftCards.internal.MSG;
import org.Kloppie74.giftCards.service.GiftCardService;
import org.Kloppie74.giftCards.config.MineStoreConfigLoader;
import org.Kloppie74.giftCards.util.ServiceRegistry;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * /mygiftcards [player]
 *
 * Robust variant that resolves the command sender to a Bukkit Player even when
 * AbstractUser.platformObject() is not a Player (tries reflection-based fallbacks).
 */
@SuppressWarnings("unused")
public class MyGiftCardsCommand {

    private Database getDatabase() { return ServiceRegistry.getDatabase(); }
    private MineStoreConfigLoader getConfig() { return ServiceRegistry.getConfigLoader(); }
    private GiftCardService getService() { return ServiceRegistry.getGiftCardService(); }

    @Command("mygiftcards [player]")
    public void myGiftCards(AbstractUser sender, @Argument(value = "player") String playerName) {
        Locale locale = Locale.ENGLISH;
        Player targetPlayer = null;
        boolean viewingOther = false;

        MineStoreConfigLoader cfg = getConfig();
        if (cfg == null) {
            sender.commonUser().sendMessage(MSG.chatColors("&cConfiguration loader not available."));
            return;
        }

        // If no argument: try to resolve sender to a Player
        if (playerName == null || playerName.isEmpty()) {
            targetPlayer = resolvePlayerFromSender(sender);
            if (targetPlayer == null) {
                // sender isn't a player (or couldn't be resolved) -> show usage
                sender.commonUser().sendMessage(MSG.chatColors(cfg.t("usage_mygiftcards", "Usage: /mygiftcards <player>")));
                return;
            }
            viewingOther = false;
        } else {
            // argument provided -> viewing other's cards
            viewingOther = true;

            // If sender is a player (or can be resolved to one), check permission
            Player senderPlayer = resolvePlayerFromSender(sender);
            if (senderPlayer != null) {
                if (!senderPlayer.hasPermission("MineStore.Giftcards.others")) {
                    sender.commonUser().sendMessage(MSG.chatColors(cfg.t("no_permission_others", "You do not have permission to view other players' giftcards.")));
                    return;
                }
            } // if senderPlayer == null and sender is console, console is allowed

            // Resolve target via Bukkit (must be online as requested)
            targetPlayer = Bukkit.getPlayerExact(playerName);
            if (targetPlayer == null) {
                sender.commonUser().sendMessage(MSG.chatColors(cfg.t("player_not_found", playerName)));
                return;
            }
        }

        // Now we have targetPlayer
        UUID uuid = targetPlayer.getUniqueId();
        Database db = getDatabase();
        if (db == null) {
            sender.commonUser().sendMessage(MSG.chatColors(cfg.t("db_not_initialized", "Database is not initialized.")));
            return;
        }

        List<String> cards = db.getGiftCards(uuid);
        if (cards.isEmpty()) {
            sender.commonUser().sendMessage(MSG.chatColors(cfg.t("no_giftcards", "No giftcards found.")));
            return;
        }

        if (viewingOther) {
            sender.commonUser().sendMessage(MSG.chatColors(cfg.t("your_giftcards", "Your giftcards") + " &7(" + targetPlayer.getName() + ")"));
        } else {
            sender.commonUser().sendMessage(MSG.chatColors(cfg.t("your_giftcards", "Your giftcards")));
        }

        final String storeUrl = cfg.getStoreUrl();
        final AbstractUser commandSender = sender;

        for (final String code : cards) {
            new Thread(() -> {
                GiftCardService.GiftCardInfo info = getService().fetchGiftCardInfo(code, storeUrl);
                String message;
                if (info == null) {
                    message = MSG.chatColors(cfg.t("giftcard_info_error", code));
                } else if (!info.isValid()) {
                    message = MSG.chatColors(cfg.t("giftcard_info_invalid", code));
                } else {
                    message = MSG.chatColors(cfg.t("giftcard_info", code, info.getEndBalance(), info.getCurrency()));
                }

                try {
                    commandSender.commonUser().sendMessage(message);
                } catch (Exception ex) {
                    try {
                        if (commandSender.platformObject() instanceof Player) {
                            ((Player) commandSender.platformObject()).sendMessage(message);
                        }
                    } catch (Exception ignored) {}
                }
            }).start();
        }
    }

    /**
     * Try multiple ways to find a Bukkit Player that represents the given AbstractUser.
     * Returns null if no online Player can be resolved.
     */
    private Player resolvePlayerFromSender(AbstractUser sender) {
        // 1) Direct platformObject()
        try {
            Object po = sender.platformObject();
            if (po instanceof Player) {
                return (Player) po;
            }
        } catch (Throwable ignored) {}

        // 2) Try methods on AbstractUser: username(), getName(), name()
        String name = tryInvokeNameMethods(sender);
        if (name != null) {
            Player p = Bukkit.getPlayerExact(name);
            if (p != null) return p;
        }

        // 3) Try methods on commonUser() object as fallback
        try {
            Object common = sender.commonUser();
            if (common != null) {
                String commonName = tryInvokeNameMethods(common);
                if (commonName != null) {
                    Player p = Bukkit.getPlayerExact(commonName);
                    if (p != null) return p;
                }
            }
        } catch (Throwable ignored) {}

        // 4) last resort: try sender.toString() as a name (may not be reliable)
        try {
            String ts = sender.toString();
            if (ts != null && !ts.isEmpty()) {
                Player p = Bukkit.getPlayerExact(ts);
                if (p != null) return p;
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private String tryInvokeNameMethods(Object obj) {
        if (obj == null) return null;
        String[] methodNames = new String[] {"username", "getName", "name"};
        for (String mname : methodNames) {
            try {
                Method m = obj.getClass().getMethod(mname);
                Object res = m.invoke(obj);
                if (res instanceof String) {
                    String s = ((String) res).trim();
                    if (!s.isEmpty()) return s;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}