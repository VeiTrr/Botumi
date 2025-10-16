package dev.behindthescenery.botumi;


import dev.behindthescenery.botumi.config.BConfig;

public final class Botumi {
    public static final String MOD_ID = "botumi";

    public static void init() {
        BConfig.load();
    }
}
