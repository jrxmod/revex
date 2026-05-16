package com.jrxmod.revex.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jrxmod.revex.Revex;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RevexConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "revex.json");

    // Master toggle
    public boolean enabled = true;

    // Default escalation ladder: each step is an action string
    // "warn", "tempban <duration>", "ban" (permanent)
    public List<String> defaultEscalation = List.of(
            "warn",
            "tempban 15m",
            "tempban 1h",
            "tempban 1d",
            "ban"
    );

    // Per-check override ladders (optional, key = check name)
    public Map<String, List<String>> perCheckEscalation = new HashMap<>();

    // Time without violations to reset escalation step back to 0
    public String escalationResetTime = "24h";

    // Staff alerts for REVEX actions
    public boolean staffAlerts = true;

    // Discord webhook for ban/unban events only
    public boolean discordWebhookEnabled = false;
    public String discordWebhookUrl = "YOUR_WEBHOOK_URL_HERE";

    public static RevexConfig load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    RevexConfig config = GSON.fromJson(reader, RevexConfig.class);
                    Revex.LOGGER.info("[REVEX] Config loaded.");
                    return config;
                }
            } else {
                RevexConfig config = new RevexConfig();
                config.save();
                Revex.LOGGER.info("[REVEX] Default config created.");
                return config;
            }
        } catch (IOException e) {
            Revex.LOGGER.error("[REVEX] Failed to load config, using defaults.", e);
            return new RevexConfig();
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            Revex.LOGGER.error("[REVEX] Failed to save config.", e);
        }
    }
}
