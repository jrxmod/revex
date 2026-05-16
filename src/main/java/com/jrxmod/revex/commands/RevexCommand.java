package com.jrxmod.revex.commands;

import com.jrxmod.revex.Revex;
import com.jrxmod.revex.manager.BanManager;
import com.jrxmod.revex.util.DurationParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class RevexCommand {

    private static final String LINE = "§8§m──────────────────────────§r";
    private static final String HEADER = "§8§m────§r §6§lREVEX§r §8§m────§r";
    private static final String BULLET = " §8» §r";

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(Commands.literal("revex")
                    .requires(source -> source.hasPermission(2))

                    // /revex status
                    .then(Commands.literal("status")
                            .executes(ctx -> {
                                CommandSourceStack source = ctx.getSource();
                                var cfg = Revex.getConfig();

                                source.sendSuccess(() -> Component.literal(HEADER), false);
                                source.sendSuccess(() -> Component.literal(
                                        BULLET + "§7Enabled: " + (cfg.enabled ? "§a✔ Yes" : "§c✘ No")), false);
                                source.sendSuccess(() -> Component.literal(
                                        BULLET + "§7Escalation ladder:"), false);

                                for (int i = 0; i < cfg.defaultEscalation.size(); i++) {
                                    int step = i + 1;
                                    String action = cfg.defaultEscalation.get(i);
                                    source.sendSuccess(() -> Component.literal(
                                            "   §8" + step + ". §f" + action), false);
                                }

                                source.sendSuccess(() -> Component.literal(
                                        BULLET + "§7Reset after: §e" + cfg.escalationResetTime), false);
                                source.sendSuccess(() -> Component.literal(
                                        BULLET + "§7Staff alerts: " + (cfg.staffAlerts ? "§a✔" : "§c✘")), false);
                                source.sendSuccess(() -> Component.literal(
                                        BULLET + "§7Discord: " + (cfg.discordWebhookEnabled ? "§a✔" : "§c✘")), false);
                                source.sendSuccess(() -> Component.literal(LINE), false);
                                return 1;
                            }))

                    // /revex bans
                    .then(Commands.literal("bans")
                            .executes(ctx -> {
                                CommandSourceStack source = ctx.getSource();
                                Map<String, BanManager.BanEntry> bans = Revex.getBanManager().getAllBans();

                                source.sendSuccess(() -> Component.literal(HEADER), false);
                                source.sendSuccess(() -> Component.literal(
                                        " §7Active bans §8(" + bans.size() + ")§7:"), false);
                                source.sendSuccess(() -> Component.literal(LINE), false);

                                if (bans.isEmpty()) {
                                    source.sendSuccess(() -> Component.literal(
                                            BULLET + "§aNo active bans."), false);
                                } else {
                                    bans.forEach((uuid, entry) -> {
                                        String remaining = entry.permanent ? "§cPermanent"
                                                : "§e" + DurationParser.format(entry.expiresAt - System.currentTimeMillis());
                                        source.sendSuccess(() -> Component.literal(
                                                BULLET + "§f" + entry.playerName + " §8— " +
                                                remaining + " §8— §7" + entry.reason), false);
                                    });
                                }
                                source.sendSuccess(() -> Component.literal(LINE), false);
                                return 1;
                            }))

                    // /revex ban <player> <duration> [reason]
                    .then(Commands.literal("ban")
                            .then(Commands.argument("player", StringArgumentType.word())
                                    .then(Commands.argument("duration", StringArgumentType.word())
                                            .executes(ctx -> executeBan(ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "player"),
                                                    StringArgumentType.getString(ctx, "duration"),
                                                    "Manual ban"))
                                            .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                    .executes(ctx -> executeBan(ctx.getSource(),
                                                            StringArgumentType.getString(ctx, "player"),
                                                            StringArgumentType.getString(ctx, "duration"),
                                                            StringArgumentType.getString(ctx, "reason")))))))

                    // /revex unban <player>
                    .then(Commands.literal("unban")
                            .then(Commands.argument("player", StringArgumentType.word())
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "player");
                                        CommandSourceStack source = ctx.getSource();
                                        ServerPlayer target = source.getServer()
                                                .getPlayerList().getPlayerByName(name);

                                        // Try online player first, then search bans by name
                                        if (target != null) {
                                            boolean removed = Revex.getBanManager().unban(target.getUUID());
                                            if (removed) {
                                                source.sendSuccess(() -> Component.literal(
                                                        "§6[REVEX] §e" + name + " §fhas been §aunbanned§f."), false);
                                            } else {
                                                source.sendFailure(Component.literal(
                                                        "§c[REVEX] §e" + name + " §fis not banned."));
                                            }
                                        } else {
                                            // Search by name in ban list
                                            Map<String, BanManager.BanEntry> bans = Revex.getBanManager().getAllBans();
                                            String foundUuid = null;
                                            for (var e : bans.entrySet()) {
                                                if (e.getValue().playerName.equalsIgnoreCase(name)) {
                                                    foundUuid = e.getKey();
                                                    break;
                                                }
                                            }
                                            if (foundUuid != null) {
                                                Revex.getBanManager().unban(java.util.UUID.fromString(foundUuid));
                                                source.sendSuccess(() -> Component.literal(
                                                        "§6[REVEX] §e" + name + " §fhas been §aunbanned§f."), false);
                                            } else {
                                                source.sendFailure(Component.literal(
                                                        "§c[REVEX] §e" + name + " §fnot found in ban list."));
                                            }
                                        }
                                        return 1;
                                    })))

                    // /revex reload
                    .then(Commands.literal("reload")
                            .executes(ctx -> {
                                CommandSourceStack source = ctx.getSource();
                                Revex.reloadConfig();
                                source.sendSuccess(() -> Component.literal(
                                        "§6[REVEX] §fConfig §areloaded §fsuccessfully!"), false);
                                return 1;
                            }))
            );
        });
    }

    private static int executeBan(CommandSourceStack source, String name, String duration, String reason) {
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(name);

        if (target == null) {
            source.sendFailure(Component.literal("§c[REVEX] §fPlayer not found: §e" + name));
            return 0;
        }

        if (duration.equalsIgnoreCase("perm") || duration.equalsIgnoreCase("permanent")) {
            Revex.getBanManager().permban(target.getUUID(), name, reason);
            BanManager.BanEntry entry = Revex.getBanManager().getBan(target.getUUID());
            target.connection.disconnect(Revex.getBanManager().buildBanScreen(entry));
            source.sendSuccess(() -> Component.literal(
                    "§6[REVEX] §e" + name + " §fpermanently banned. Reason: §7" + reason), false);
        } else {
            long durationMs = DurationParser.parseToMs(duration);
            if (durationMs <= 0) {
                source.sendFailure(Component.literal(
                        "§c[REVEX] §fInvalid duration: §e" + duration +
                        " §8(use: 15m, 1h, 7d, perm)"));
                return 0;
            }
            Revex.getBanManager().tempban(target.getUUID(), name, durationMs, reason);
            BanManager.BanEntry entry = Revex.getBanManager().getBan(target.getUUID());
            target.connection.disconnect(Revex.getBanManager().buildBanScreen(entry));
            source.sendSuccess(() -> Component.literal(
                    "§6[REVEX] §e" + name + " §fbanned for §e" + duration + "§f. Reason: §7" + reason), false);
        }
        return 1;
    }
}
