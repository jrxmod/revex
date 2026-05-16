package com.jrxmod.revex.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jrxmod.revex.Revex;
import com.jrxmod.revex.util.DurationParser;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public class EscalationManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ESCALATION_PATH = Paths.get("config", "revex-escalation.json");

    // UUID string -> EscalationData
    private final Map<String, EscalationData> escalationMap = new HashMap<>();

    public EscalationManager() {
        load();
    }

    // Get current escalation step for a player, resetting if enough time passed
    public int getStep(UUID uuid) {
        EscalationData data = escalationMap.get(uuid.toString());
        if (data == null) return 0;

        // Check if escalation should reset
        long resetMs = DurationParser.parseToMs(Revex.getConfig().escalationResetTime);
        if (resetMs > 0 && System.currentTimeMillis() - data.lastViolationTime >= resetMs) {
            data.step = 0;
        }
        return data.step;
    }

    // Advance to next step and return the action string for current step
    public String advanceAndGetAction(UUID uuid) {
        String key = uuid.toString();
        EscalationData data = escalationMap.computeIfAbsent(key, k -> new EscalationData());

        // Check reset
        long resetMs = DurationParser.parseToMs(Revex.getConfig().escalationResetTime);
        if (resetMs > 0 && System.currentTimeMillis() - data.lastViolationTime >= resetMs) {
            data.step = 0;
        }

        List<String> ladder = Revex.getConfig().defaultEscalation;
        int currentStep = Math.min(data.step, ladder.size() - 1);
        String action = ladder.get(currentStep);

        // Advance step for next time (cap at max)
        data.step = Math.min(data.step + 1, ladder.size() - 1);
        data.lastViolationTime = System.currentTimeMillis();

        save();
        return action;
    }

    // Get action from per-check ladder if configured, otherwise default
    public String advanceAndGetAction(UUID uuid, String checkName) {
        Map<String, List<String>> perCheck = Revex.getConfig().perCheckEscalation;
        if (perCheck != null && perCheck.containsKey(checkName)) {
            return advanceWithLadder(uuid, checkName, perCheck.get(checkName));
        }
        return advanceAndGetAction(uuid);
    }

    private String advanceWithLadder(UUID uuid, String checkName, List<String> ladder) {
        String key = uuid.toString();
        EscalationData data = escalationMap.computeIfAbsent(key, k -> new EscalationData());

        long resetMs = DurationParser.parseToMs(Revex.getConfig().escalationResetTime);
        if (resetMs > 0 && System.currentTimeMillis() - data.lastViolationTime >= resetMs) {
            data.step = 0;
        }

        int currentStep = Math.min(data.step, ladder.size() - 1);
        String action = ladder.get(currentStep);

        data.step = Math.min(data.step + 1, ladder.size() - 1);
        data.lastViolationTime = System.currentTimeMillis();

        save();
        return action;
    }

    public void resetPlayer(UUID uuid) {
        escalationMap.remove(uuid.toString());
        save();
    }

    private void load() {
        try {
            Files.createDirectories(ESCALATION_PATH.getParent());
            if (!Files.exists(ESCALATION_PATH)) {
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(ESCALATION_PATH)) {
                Type type = new TypeToken<Map<String, EscalationData>>() {}.getType();
                Map<String, EscalationData> loaded = GSON.fromJson(reader, type);
                if (loaded != null) escalationMap.putAll(loaded);
            }
            Revex.LOGGER.info("[REVEX] Escalation data loaded ({} players).", escalationMap.size());
        } catch (IOException e) {
            Revex.LOGGER.error("[REVEX] Failed to load escalation data.", e);
        }
    }

    private void save() {
        try (Writer writer = Files.newBufferedWriter(ESCALATION_PATH)) {
            GSON.toJson(escalationMap, writer);
        } catch (IOException e) {
            Revex.LOGGER.error("[REVEX] Failed to save escalation data.", e);
        }
    }

    public static class EscalationData {
        public int step = 0;
        public long lastViolationTime = 0;
    }
}
