package org.Kloppie74.giftCards.util;

import org.Kloppie74.giftCards.config.MineStoreConfigLoader;
import org.Kloppie74.giftCards.internal.Database;
import org.Kloppie74.giftCards.service.GiftCardService;

public final class ServiceRegistry {

    private static Database database;
    private static MineStoreConfigLoader configLoader;
    private static GiftCardService giftCardService;

    private ServiceRegistry() {}

    public static Database getDatabase() { return database; }
    public static void setDatabase(Database db) { database = db; }

    public static MineStoreConfigLoader getConfigLoader() { return configLoader; }
    public static void setConfigLoader(MineStoreConfigLoader loader) { configLoader = loader; }

    public static GiftCardService getGiftCardService() { return giftCardService; }
    public static void setGiftCardService(GiftCardService svc) { giftCardService = svc; }
}