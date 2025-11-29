package org.Kloppie74.giftCards.config;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

/**
 * Loader for MineStore config and language files.
 * - Prioritizes exact language filename (e.g. en_US.yml) in plugins/MineStore/addons/GiftCards
 * - If missing, creates addons/<language>.yml with default keys (does not overwrite existing keys)
 * - Uses the addons file as the effective language config
 */
public class MineStoreConfigLoader {

    private final YamlConfiguration config;
    private final YamlConfiguration langConfig;
    private final String language;
    private final File loadedLangFile;

    private static final LinkedHashMap<String, String> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("only_players", "&cOnly players can use this command!");
        DEFAULTS.put("usage_giftcard", "&eUsage: /giftcard create %player% %amount%");
        DEFAULTS.put("invalid_amount", "&cInvalid amount!");
        DEFAULTS.put("api_config_missing", "&cAPI key or Store URL is not set in the config!");
        DEFAULTS.put("giftcard_created", "&aGiftcard created! Code: &e%s &a(€%s)");
        DEFAULTS.put("giftcard_received", "&aYou have received a new giftcard! Code: &e%s &a(€%s)");
        DEFAULTS.put("api_invalid", "&cAPI key invalid or API access disabled!");
        DEFAULTS.put("unknown_error", "&cUnknown error (%s)!");
        DEFAULTS.put("unknown_error_giftcard", "&cUnknown error while creating the giftcard.");
        DEFAULTS.put("no_giftcards", "&eYou have no giftcards.");
        DEFAULTS.put("your_giftcards", "&bYour giftcards:");
        DEFAULTS.put("giftcard_info_error", "&7- %s &c(Not found or error)");
        DEFAULTS.put("giftcard_info_invalid", "&7- %s &c(Invalid or empty)");
        DEFAULTS.put("giftcard_info", "&7- %s &6(%s %s)");
        DEFAULTS.put("player_not_found", "&cPlayer %s not found!");
        DEFAULTS.put("giftcard_api_error", "&cGiftcard API error: %s");
        DEFAULTS.put("giftcard_api_error_unknown", "&cUnknown error from giftcard API.");
    }

    public MineStoreConfigLoader() {
        this.config = loadConfig();
        this.language = resolveLanguage(this.config);
        FileHolder holder = ensureAddonsLangFile(this.language);
        this.langConfig = holder.config;
        this.loadedLangFile = holder.file;
        logLoadedLanguage();
    }

    private static class FileHolder {
        final YamlConfiguration config;
        final File file;
        FileHolder(YamlConfiguration c, File f) { this.config = c; this.file = f; }
    }

    private YamlConfiguration loadConfig() {
        org.bukkit.plugin.Plugin mineStore = Bukkit.getPluginManager().getPlugin("MineStore");
        File cfgFile;
        if (mineStore != null) {
            cfgFile = new File(mineStore.getDataFolder(), "config.yml");
        } else {
            cfgFile = new File("plugins" + File.separator + "MineStore", "config.yml");
        }
        if (!cfgFile.exists()) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(cfgFile);
    }

    private String resolveLanguage(YamlConfiguration cfg) {
        String lang = cfg.getString("language", "en");
        if (lang == null || lang.trim().isEmpty()) return "en";
        return lang.trim();
    }

    /**
     * Ensure the addons lang file exists under plugins/MineStore/addons/GiftCards/<language>.yml.
     * Prefer the exact filename (e.g. en_US.yml). Merge missing keys from DEFAULTS.
     */
    private FileHolder ensureAddonsLangFile(String lang) {
        String shortLang = lang.contains("_") ? lang.split("_")[0] : lang;
        File addonsDir = new File("plugins" + File.separator + "MineStore" + File.separator + "addons" + File.separator + "GiftCards");
        try {
            if (!addonsDir.exists()) Files.createDirectories(addonsDir.toPath());
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.WARNING, "[GiftCards lang] Failed to create addons folder: " + addonsDir.getAbsolutePath(), ex);
        }

        // Prefer exact file: <language>.yml (e.g. en_US.yml)
        File exactFile = new File(addonsDir, lang + ".yml");
        // Also consider common alternatives as fallbacks
        File langUnderscoreFile = new File(addonsDir, "lang_" + lang + ".yml");
        File shortFile = new File(addonsDir, shortLang + ".yml");
        File langShortFile = new File(addonsDir, "lang_" + shortLang + ".yml");

        // If any of the candidate files already exist, use the first existing in this priority:
        List<File> candidates = Arrays.asList(exactFile, langUnderscoreFile, shortFile, langShortFile);
        for (File f : candidates) {
            if (f.exists()) {
                try {
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                    // Merge missing keys if needed
                    boolean changed = mergeDefaultsIfMissing(cfg, f);
                    if (changed) {
                        try { cfg.save(f); } catch (IOException ioe) { Bukkit.getLogger().log(Level.WARNING, "[GiftCards lang] Failed to save merged lang file: " + f.getAbsolutePath(), ioe); }
                    }
                    return new FileHolder(cfg, f);
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.WARNING, "[GiftCards lang] Failed to load existing lang file: " + f.getAbsolutePath(), ex);
                }
            }
        }

        // None exist: create exactFile (<language>.yml) with defaults
        try {
            YamlConfiguration newCfg = new YamlConfiguration();
            for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
                newCfg.set(e.getKey(), e.getValue());
            }
            newCfg.save(exactFile);
            Bukkit.getLogger().info("[GiftCards lang] Created default lang file at " + exactFile.getAbsolutePath());
            return new FileHolder(newCfg, exactFile);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.WARNING, "[GiftCards lang] Failed to create default lang file at " + exactFile.getAbsolutePath(), ex);
        }

        // Last resort: try creating lang_<short>.yml
        try {
            YamlConfiguration fallbackCfg = new YamlConfiguration();
            for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
                fallbackCfg.set(e.getKey(), e.getValue());
            }
            fallbackCfg.save(langShortFile);
            Bukkit.getLogger().info("[GiftCards lang] Created fallback lang file at " + langShortFile.getAbsolutePath());
            return new FileHolder(fallbackCfg, langShortFile);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.WARNING, "[GiftCards lang] Failed to create fallback lang file: " + langShortFile.getAbsolutePath(), ex);
        }

        // Ultimate fallback: empty config with null file ref
        return new FileHolder(new YamlConfiguration(), null);
    }

    private boolean mergeDefaultsIfMissing(YamlConfiguration cfg, File f) {
        boolean changed = false;
        for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
            if (cfg.getString(e.getKey(), null) == null) {
                cfg.set(e.getKey(), e.getValue());
                changed = true;
                Bukkit.getLogger().info("[GiftCards lang] Added missing key '" + e.getKey() + "' to " + f.getAbsolutePath());
            }
        }
        return changed;
    }

    private void logLoadedLanguage() {
        String fileInfo = (loadedLangFile != null) ? loadedLangFile.getAbsolutePath() : "no file (empty config)";
        Bukkit.getLogger().info("[GiftCards lang] language=" + language + " effectiveLangFile=" + fileInfo);
    }

    public String getStoreUrl() {
        String url = config.getString("store-url", "").trim();
        if (!url.isEmpty() && !url.endsWith("/")) url += "/";
        return url;
    }

    public boolean isApiKeyEnabled() {
        if (config.contains("api.key-enabled")) {
            return config.getBoolean("api.key-enabled", true);
        }
        return config.getBoolean("api.key-enabled", true);
    }

    public String getApiKey() {
        return config.getString("api.key", "").trim();
    }

    public String buildCreateGiftCardUrl() {
        String storeUrl = getStoreUrl();
        boolean keyEnabled = isApiKeyEnabled();
        String key = getApiKey();
        String apiSegment = "api/";
        if (keyEnabled) {
            return storeUrl + apiSegment + (key.isEmpty() ? "" : key + "/") + "createGiftCard";
        } else {
            return storeUrl + apiSegment + "createGiftCard";
        }
    }

    /**
     * Localization helper:
     * - Replaces %player% if present (first arg)
     * - Otherwise uses String.format(msg, args)
     * - If key missing returns key name
     */
    public String t(String key, Object... args) {
        String raw = null;
        if (langConfig != null) {
            try {
                raw = langConfig.getString(key, null);
            } catch (Exception ignored) {
                raw = null;
            }
        }
        if (raw == null) {
            // fallback to key for visibility
            raw = key;
        }

        try {
            if (raw.contains("%player%") && args != null && args.length > 0 && args[0] != null) {
                raw = raw.replace("%player%", Objects.toString(args[0]));
                if (args.length == 1) return raw;
                Object[] rest = new Object[args.length - 1];
                System.arraycopy(args, 1, rest, 0, rest.length);
                try { return String.format(raw, rest); } catch (Exception ignored) { return raw; }
            }
            if (args != null && args.length > 0) {
                try { return String.format(raw, args); } catch (Exception ignored) { return raw; }
            }
            return raw;
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.WARNING, "[GiftCards lang] Error formatting key '" + key + "'", ex);
            return raw;
        }
    }

    public String getLanguage() {
        return language;
    }

    public File getLoadedLangFile() {
        return loadedLangFile;
    }
}