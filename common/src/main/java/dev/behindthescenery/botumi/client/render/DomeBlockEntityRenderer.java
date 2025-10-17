package dev.behindthescenery.botumi.client.render;

import dev.behindthescenery.botumi.blocks.entity.DomeBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class DomeBlockEntityRenderer implements BlockEntityRenderer<DomeBlockEntity> {
    private static final int DOME_COLOR = 0x6680D8FF;
    private static final int SEGMENTS = 96;
    private static final int RINGS = 24;
    private static final int MERIDIANS = 24;

    public DomeBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    private static void line(VertexConsumer vc, Matrix4f posMat, MatrixStack.Entry entry,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             int r, int g, int b, int a, int light) {
        float nx = 0f, ny = 1f, nz = 0f;

        vc.vertex(posMat, x1, y1, z1)
                .color(r, g, b, a)
                .normal(entry, nx, ny, nz)
                .light(light);

        vc.vertex(posMat, x2, y2, z2)
                .color(r, g, b, a)
                .normal(entry, nx, ny, nz)
                .light(light);
    }

    @Override
    public void render(DomeBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!entity.isEnabled()) return;

        double radius = entity.getRenderRadius();
        if (radius < 0.5) return;

        Vec3d center = entity.getDomeBaseCenter();

        matrices.push();

        matrices.translate(
                center.x - entity.getPos().getX(),
                center.y - entity.getPos().getY(),
                center.z - entity.getPos().getZ()
        );
        System.out.println(center.getX() + ", " + center.getY() + ", " + center.getZ());
        System.out.println(entity.getPos());

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f posMat = entry.getPositionMatrix();

        int a = (DOME_COLOR >>> 24) & 0xFF;
        int r = (DOME_COLOR >>> 16) & 0xFF;
        int g = (DOME_COLOR >>> 8) & 0xFF;
        int b = (DOME_COLOR) & 0xFF;

        for (int i = 0; i <= RINGS; i++) {
            double t = (double) i / (double) RINGS;
            double phi = t * (Math.PI / 2.0);
            double y = Math.sin(phi) * radius;
            double ringR = Math.cos(phi) * radius;

            double prevX = 0.0, prevZ = 0.0;
            boolean hasPrev = false;

            for (int s = 0; s <= SEGMENTS; s++) {
                double u = (double) s / (double) SEGMENTS;
                double ang = u * Math.PI * 2.0;

                double x = Math.cos(ang) * ringR;
                double z = Math.sin(ang) * ringR;

                if (hasPrev) {
                    line(vc, posMat, entry,
                            (float) prevX, (float) y, (float) prevZ,
                            (float) x, (float) y, (float) z,
                            r, g, b, a, light);
                }
                prevX = x;
                prevZ = z;
                hasPrev = true;
            }
        }

        for (int m = 0; m < MERIDIANS; m++) {
            double beta = (double) m / (double) MERIDIANS * Math.PI * 2.0;
            double cb = Math.cos(beta);
            double sb = Math.sin(beta);

            double prevX = 0.0, prevY = 0.0, prevZ = 0.0;
            boolean hasPrev = false;

            for (int i = 0; i <= RINGS; i++) {
                double t = (double) i / (double) RINGS;
                double phi = t * (Math.PI / 2.0);
                double y = Math.sin(phi) * radius;
                double ringR = Math.cos(phi) * radius;

                double x = cb * ringR;
                double z = sb * ringR;

                if (hasPrev) {
                    line(vc, posMat, entry,
                            (float) prevX, (float) prevY, (float) prevZ,
                            (float) x, (float) y, (float) z,
                            r, g, b, a, light);
                }
                prevX = x;
                prevY = y;
                prevZ = z;
                hasPrev = true;
            }
        }

        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(DomeBlockEntity blockEntity) {
        return true;
    }
}
