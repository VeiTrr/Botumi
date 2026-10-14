package dev.behindthescenery.botumi.client.render;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.entity.DomeGuardianEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class DomeGuardianEntityRenderer extends BipedEntityRenderer<DomeGuardianEntity, BipedEntityModel<DomeGuardianEntity>> {
    private static final Identifier TEXTURE = Identifier.of(Botumi.MOD_ID, "textures/entity/bot_husk.png");

    public DomeGuardianEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public Identifier getTexture(DomeGuardianEntity entity) {
        return TEXTURE;
    }
}
