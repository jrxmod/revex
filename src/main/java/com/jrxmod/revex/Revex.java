package com.jrxmod.revex;

import com.jrxmod.revex.commands.RevexCommand;
import com.jrxmod.revex.config.RevexConfig;
import com.jrxmod.revex.listener.PraxicListener;
import com.jrxmod.revex.manager.BanManager;
import com.jrxmod.revex.manager.EscalationManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Revex implements ModInitializer {

    public static final String MOD_ID = "revex";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static RevexConfig config;
    private static BanManager banManager;
    private static EscalationManager escalationManager;

    @Override
    public void onInitialize() {
        LOGGER.info("[REVEX] Initializing punishment system...");

        config = RevexConfig.load();
        banManager = new BanManager();
        escalationManager = new EscalationManager();

        RevexCommand.register();
        PraxicListener.register();

        // Block banned players on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            if (banManager.isBanned(player.getUUID())) {
                BanManager.BanEntry entry = banManager.getBan(player.getUUID());
                if (entry != null) {
                    player.connection.disconnect(banManager.buildBanScreen(entry));
                }
            }
        });

        LOGGER.info("[REVEX] Punishment system initialized. Listening to PRAXIC.");
    }

    public static RevexConfig getConfig() {
        return config;
    }

    public static void reloadConfig() {
        config = RevexConfig.load();
    }

    public static BanManager getBanManager() {
        return banManager;
    }

    public static EscalationManager getEscalationManager() {
        return escalationManager;
    }
}
