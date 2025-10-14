package dev.behindthescenery.botumi.mixin.Render;

import dev.behindthescenery.botumi.registry.BotumiRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RenderLayers.class)
public class TransculentBlock {
    @Shadow
    @Final
    private static Map<Block, RenderLayer> BLOCKS;

    @Inject(method = "<clinit>*", at = @At("RETURN"))
    private static void onInitialize(CallbackInfo info) {
        BLOCKS.put(BotumiRegistry.MINIGAMES_BLOCK, RenderLayer.getTranslucent());
    }
}
