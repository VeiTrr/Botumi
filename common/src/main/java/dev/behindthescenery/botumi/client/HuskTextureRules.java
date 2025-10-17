package dev.behindthescenery.botumi.client;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.config.BConfig;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

public final class HuskTextureRules {
    private static final Identifier TEX_BASE = Identifier.of(Botumi.MOD_ID, "textures/entity/bot_husk.png");

    private HuskTextureRules() {
    }

    public static Identifier selectTexture(ZombieEntity entity, Identifier TEX_DEFAULT) {
        if (entity.getPassengerList().stream().noneMatch(e -> e instanceof ArmorStandEntity)) {
            return TEX_DEFAULT;
        } else {
            ArmorStandEntity armorStand = (ArmorStandEntity) entity.getPassengerList().stream()
                    .filter(e -> e instanceof ArmorStandEntity)
                    .findFirst()
                    .orElse(null);
            if (armorStand == null) return TEX_DEFAULT;

            String name = armorStand.getCustomName() != null ? armorStand.getCustomName().getString() : "";
            String key = BConfig.INSTANCE.CustomHuskTag;
            if (key == null || key.isBlank()) return null;
            if (name.isBlank() || !name.contains(key)) return TEX_DEFAULT;
            String[] parts = name.split(":");
            if (parts.length < 2) return TEX_DEFAULT;
            int variant;
            try {
                variant = Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException e) {
                return TEX_DEFAULT;
            }
            if (variant == 0) return TEX_DEFAULT;
            if (variant == 1) return TEX_BASE;
            if (variant > 1) {
                String path = TEX_BASE.getPath();
                path = path.replace("bot_husk", "bot_husk" + (variant));
                return TEX_BASE.withPath(path);
            }
            return TEX_DEFAULT;
        }
    }
}
