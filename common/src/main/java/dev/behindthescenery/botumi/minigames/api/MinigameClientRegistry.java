package dev.behindthescenery.botumi.minigames.api;

import dev.behindthescenery.botumi.client.ui.MinigameScreen;
import dev.behindthescenery.botumi.minigames.client.DebugMinigameClient;
import dev.behindthescenery.botumi.minigames.client.WiresMinigameClient;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class MinigameClientRegistry {
    private static final Map<Identifier, Function<MinigameScreen, MinigameClient>> CLIENTS = new ConcurrentHashMap<>();

    private MinigameClientRegistry() {
    }

    public static void init() {
        register(Identifier.of("botumi", "minigame_debug"), DebugMinigameClient::new);
        register(Identifier.of("botumi", "minigame_wires"), WiresMinigameClient::new);
    }

    public static void register(Identifier id, Function<MinigameScreen, MinigameClient> factory) {
        CLIENTS.put(id, factory);
    }

    public static void registerClient(Identifier id, Function<MinigameScreen, MinigameClient> factory) {
        register(id, factory);
    }

    public static MinigameClient create(String idStr, MinigameScreen parent) {
        init();
        try {
            Identifier id = Identifier.tryParse(idStr);
            Function<MinigameScreen, MinigameClient> factory = CLIENTS.get(id);
            if (factory != null) {
                return factory.apply(parent);
            }
        } catch (Exception ignored) {
        }
        return new DebugMinigameClient(parent);
    }
}
