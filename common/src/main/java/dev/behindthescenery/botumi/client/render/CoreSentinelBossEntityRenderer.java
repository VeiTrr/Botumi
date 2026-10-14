package dev.behindthescenery.botumi.client.render;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.entity.CoreSentinelBossEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CoreSentinelBossEntityRenderer extends BipedEntityRenderer<CoreSentinelBossEntity, BipedEntityModel<CoreSentinelBossEntity>> {
    private static final Identifier TEXTURE = Identifier.of(Botumi.MOD_ID, "textures/entity/bot_husk2.png");

    public CoreSentinelBossEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)), 0.7F);
    }

    @Override
    protected void scale(CoreSentinelBossEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.35F, 1.35F, 1.35F);
        super.scale(entity, matrices, amount);
    }

    @Override
    public Identifier getTexture(CoreSentinelBossEntity entity) {
        return TEXTURE;
    }
}
