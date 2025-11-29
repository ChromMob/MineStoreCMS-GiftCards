package org.Kloppie74.giftCards.commands;

import me.chrommob.minestore.api.Registries;
import me.chrommob.minestore.api.interfaces.commands.CommonConsoleUser;
import me.chrommob.minestore.api.interfaces.user.AbstractUser;
import me.chrommob.minestore.api.scheduler.MineStoreScheduledTask;
import me.chrommob.minestore.api.web.WebApiAccessor;
import me.chrommob.minestore.api.web.giftcard.GiftCardManager;
import net.kyori.adventure.text.Component;
import org.Kloppie74.giftCards.GiftCardsAddon;
import org.Kloppie74.giftCards.internal.Database;
import org.Kloppie74.giftCards.internal.MSG;
import org.Kloppie74.giftCards.config.MineStoreConfigLoader;
import org.Kloppie74.giftCards.util.CodeGenerator;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;

import java.time.LocalDateTime;
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

/*
    private Database getDatabase() { return ServiceRegistry.getDatabase(); }
    private MineStoreConfigLoader getConfig() { return ServiceRegistry.getConfigLoader(); }
    private GiftCardService getService() { return ServiceRegistry.getGiftCardService(); }
*/

    private final Database db;
    public GiftCardCommand(Database database) {
        this.db = database;
    }

    @Permission("MineStore.Giftcards.create")
    @Command("giftcard create <target> <amount>")
    public void createGiftCard(AbstractUser sender, @Argument("target") String targetName, @Argument("amount") int amount) {
        Locale locale = Locale.ENGLISH;
        AbstractUser target = Registries.USER_GETTER.get().get(targetName);
        if (target == null || target.commonUser() instanceof CommonConsoleUser) {
            sender.commonUser().sendMessage(MineStoreConfigLoader.replaceHelper(MineStoreConfigLoader.PLAYER_NOT_FOUND.getValue(), "{player}", targetName));
            return;
        }

        if (amount <= 0) {
            sender.commonUser().sendMessage(MineStoreConfigLoader.INVALID_AMOUNT.getValue());
            return;
        }

        String note = "Created by " + sender.commonUser().getName();

        String code = CodeGenerator.generateCode(12);
        LocalDateTime expireDate = LocalDateTime.now().plusDays(30);

        Registries.MINESTORE_SCHEDULER.get().runDelayed(
                new MineStoreScheduledTask("saveToDb", () -> {
                    GiftCardManager.CreateGiftCardResponse res = WebApiAccessor.giftCardManager().createGiftCard(code, note, amount, expireDate, targetName);
                    if (!res.isSuccess()) {
                        sender.commonUser().sendMessage(MineStoreConfigLoader.replaceHelper(MineStoreConfigLoader.ERROR_GIFTCARD.getValue(), "{error}", res.message()));
                        return;
                    }
                    UUID targetUuid = target.commonUser().getUUID();
                    if (db != null && targetUuid != null) {
                        db.addGiftCard(targetUuid, code);
                    }

                    Component creatorMsg = MineStoreConfigLoader.replaceHelper(
                            MineStoreConfigLoader.replaceHelper(MineStoreConfigLoader.GIFTCARD_CREATE.getValue(), "{code}", code)
                    , "{amount}", String.valueOf(amount));
                    Component targetMsg = MineStoreConfigLoader.replaceHelper(
                            MineStoreConfigLoader.replaceHelper(MineStoreConfigLoader.GIFTCARD_RECEIVED.getValue(), "{code}", code)
                            , "{amount}", String.valueOf(amount));
                    sender.commonUser().sendMessage(creatorMsg);
                    target.commonUser().sendMessage(targetMsg);
                }, 0)
        );
    }
}