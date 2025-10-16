package dev.behindthescenery.botumi.fabric;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.fabric.registry.BotumiFabricRegistry;
import net.fabricmc.api.ModInitializer;

public final class BotumiFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Botumi.init();
        BotumiFabricRegistry.register();
    }
}
