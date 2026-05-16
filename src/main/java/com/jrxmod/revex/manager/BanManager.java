package com.jrxmod.revex.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jrxmod.revex.Revex;
import com.jrxmod.revex.util.DurationParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BanManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path BANS_PATH = Paths.get("config", "revex-bans.json");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Map<String, BanEntry> bans = new ConcurrentHashMap<>();

    public BanManager() {
        load();
    }

    public void tempban(UUID uuid, String playerName, long durationMs, String reason) {
        long now = System.currentTimeMillis();
        BanEntry entry = new BanEntry();
        entry.playerName = playerName;
        entry.reason = reason;
        entry.bannedAt = FORMATTER.format(Instant.ofEpochMilli(now));
        entry.expiresAt = now + durationMs;
        entry.permanent = false;
        bans.put(uuid.toString(), entry);
        save();
    }

    public void permban(UUID uuid, String playerName, String reason) {
        long now = System.currentTimeMillis();
        BanEntry entry = new BanEntry();
        entry.playerName = playerName;
        entry.reason = reason;
        entry.bannedAt = FORMATTER.format(Instant.ofEpochMilli(now));
        entry.expiresAt = -1;
        entry.permanent = true;
        bans.put(uuid.toString(), entry);
        save();
    }

    public boolean unban(UUID uuid) {
        boolean removed = bans.remove(uuid.toString()) != null;
        if (removed) save();
        return removed;
    }

    public boolean isBanned(UUID uuid) {
        BanEntry entry = bans.get(uuid.toString());
        if (entry == null) return false;

        if (!entry.permanent && System.currentTimeMillis() >= entry.expiresAt) {
            bans.remove(uuid.toString());
            save();
            return false;
        }
        return true;
    }

    public BanEntry getBan(UUID uuid) {
        return bans.get(uuid.toString());
    }

    public Map<String, BanEntry> getAllBans() {
        // Clean expired bans before returning
        boolean changed = false;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, BanEntry>> it = bans.entrySet().iterator();
        while (it.hasNext()) {
            BanEntry entry = it.next().getValue();
            if (!entry.permanent && now >= entry.expiresAt) {
                it.remove();
                changed = true;
            }
        }
        if (changed) save();
        return Collections.unmodifiableMap(bans);
    }

    // Build disconnect screen for banned player
    public Component buildBanScreen(BanEntry entry) {
        String remaining = entry.permanent ? "§cPermanent"
                : DurationParser.format(entry.expiresAt - System.currentTimeMillis());

        return Component.literal(
                "§6§lREVEX §8§m──────────────§r\n\n" +
                "§cYou are " + (entry.permanent ? "§l§cpermanently banned" : "§l§ctemporarily banned") + "§r§c.\n\n" +
                "§7Reason: §f" + entry.reason + "\n" +
                "§7Duration: " + remaining + "\n" +
                "§7Banned at: §f" + entry.bannedAt + "\n\n" +
                "§8If you think this is a mistake,\n" +
                "§8contact server administration."
        );
    }

    private void load() {
        try {
            Files.createDirectories(BANS_PATH.getParent());
            if (!Files.exists(BANS_PATH)) {
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(BANS_PATH)) {
                Type type = new TypeToken<Map<String, BanEntry>>() {}.getType();
                Map<String, BanEntry> loaded = GSON.fromJson(reader, type);
                if (loaded != null) bans.putAll(loaded);
            }
            Revex.LOGGER.info("[REVEX] Ban list loaded ({} entries).", bans.size());
        } catch (IOException e) {
            Revex.LOGGER.error("[REVEX] Failed to load ban list.", e);
        }
    }

    private void save() {
        try (Writer writer = Files.newBufferedWriter(BANS_PATH)) {
            GSON.toJson(bans, writer);
        } catch (IOException e) {
            Revex.LOGGER.error("[REVEX] Failed to save ban list.", e);
        }
    }

    public static class BanEntry {
        public String playerName;
        public String reason;
        public String bannedAt;
        public long expiresAt;
        public boolean permanent;
    }
}
