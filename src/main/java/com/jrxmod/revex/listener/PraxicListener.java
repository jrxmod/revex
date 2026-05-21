package com.jrxmod.revex.listener;

import com.jrxmod.praxic.api.PraxicViolationEvent;
import com.jrxmod.revex.Revex;
import com.jrxmod.revex.manager.BanManager;
import com.jrxmod.revex.manager.EscalationManager;
import com.jrxmod.revex.util.DurationParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PraxicListener {

    public static void register() {
        PraxicViolationEvent.EVENT.register((player, checkName, violations, details, action) -> {
            if (!Revex.getConfig().enabled) return false;

            // Only intercept when PRAXIC resolves a punishable action (not just "flag")
            // Return false so PRAXIC handles flags normally
            if (action.equals("flag")) return false;

            // REVEX takes over — return true to cancel PRAXIC's built-in punishment
            handleViolation(player, checkName, violations, details);
            return true;
        });

        Revex.LOGGER.info("[REVEX] Listening to PRAXIC violation events.");
    }

    private static void handleViolation(ServerPlayer player, String checkName, int violations, String details) {
        EscalationManager escalation = Revex.getEscalationManager();
        BanManager banManager = Revex.getBanManager();

        String escalationAction = escalation.advanceAndGetAction(player.getUUID(), checkName);

        if (escalationAction.equals("warn")) {
            player.sendSystemMessage(Component.literal(
                    "§6[REVEX] §eWarning! §7Suspicious activity detected.\n" +
                    "§8» §7Check: §f" + checkName + " §8| §7Next offense will result in a temporary ban."
            ));
            sendStaffAlert(player, checkName, "warn");

        } else if (escalationAction.startsWith("tempban ")) {
            String durationStr = escalationAction.substring(8).trim();
            long durationMs = DurationParser.parseToMs(durationStr);

            if (durationMs <= 0) {
                Revex.LOGGER.error("[REVEX] Invalid tempban duration: {}", durationStr);
                return;
            }

            banManager.tempban(player.getUUID(), player.getName().getString(), durationMs, checkName);
            BanManager.BanEntry entry = banManager.getBan(player.getUUID());
            player.connection.disconnect(banManager.buildBanScreen(entry));

            sendStaffAlert(player, checkName, "tempban " + durationStr);
            Revex.LOGGER.warn("[REVEX] Player {} temp-banned for {} by {}.",
                    player.getName().getString(), durationStr, checkName);

        } else if (escalationAction.equals("ban")) {
            banManager.permban(player.getUUID(), player.getName().getString(), checkName);
            BanManager.BanEntry entry = banManager.getBan(player.getUUID());
            player.connection.disconnect(banManager.buildBanScreen(entry));

            sendStaffAlert(player, checkName, "permanent ban");
            Revex.LOGGER.warn("[REVEX] Player {} permanently banned by {}.",
                    player.getName().getString(), checkName);
        }
    }

    private static void sendStaffAlert(ServerPlayer player, String checkName, String action) {
        if (!Revex.getConfig().staffAlerts) return;
        if (player.getServer() == null) return;

        Component alert = Component.literal(
                "§6[REVEX] §bEnforcement §8» §e" + player.getName().getString() +
                " §8— §b" + checkName + " §8→ §f" + action
        );

        player.getServer().getPlayerList().getPlayers().stream()
                .filter(p -> p.hasPermissions(2))
                .forEach(p -> p.sendSystemMessage(alert));
    }
}
