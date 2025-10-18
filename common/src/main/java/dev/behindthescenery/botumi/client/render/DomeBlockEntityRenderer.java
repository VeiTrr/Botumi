package dev.behindthescenery.botumi.client.render;

import dev.behindthescenery.botumi.blocks.entity.DomeBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class DomeBlockEntityRenderer implements BlockEntityRenderer<DomeBlockEntity> {
    private static final int SEGMENTS = 256;
    private static final int RINGS = 48;

    @SuppressWarnings("unused")
    public DomeBlockEntityRenderer(BlockEntityRendererFactory.Context context) { }

    private static void vertex(VertexConsumer vc, Matrix4f posMat, MatrixStack.Entry entry,
                               float x, float y, float z,
                               float u, float v,
                               int r, int g, int b, int a,
                               int overlay, int light,
                               float nx, float ny, float nz) {
        vc.vertex(posMat, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(entry, nx, ny, nz);
    }

    public static double normPi(double a) {
        while (a <= -Math.PI) a += Math.PI * 2.0;
        while (a >  Math.PI)  a -= Math.PI * 2.0;
        return a;
    }

    private static boolean rangesOverlap(double a1, double a2, double b1, double b2) {
        return Math.max(a1, b1) <= Math.min(a2, b2);
    }

    private static boolean angleIntervalOverlapsSector(double th1, double th2, double ga, double half) {
        double a1 = normPi(th1 - ga);
        double a2 = normPi(th2 - ga);
        if (a2 < a1) a2 += Math.PI * 2.0;
        return rangesOverlap(a1, a2, -half, +half) ||
                rangesOverlap(a1 - Math.PI * 2.0, a2 - Math.PI * 2.0, -half, +half) ||
                rangesOverlap(a1 + Math.PI * 2.0, a2 + Math.PI * 2.0, -half, +half);
    }

    @Override
    public void render(DomeBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!entity.isEnabled()) return;

        double radius = entity.getRenderRadius();
        if (radius < 0.5) return;

        Vec3d center = entity.getDomeBaseCenter();

        float t = (float) (Util.getMeasuringTimeMs() % 6000L) / 6000.0f;
        float uScroll = t;
        float vScroll = -t;

        matrices.push();
        matrices.translate(
                center.x - entity.getPos().getX(),
                center.y - entity.getPos().getY(),
                center.z - entity.getPos().getZ()
        );

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(entity.getTextureId()));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f posMat = entry.getPositionMatrix();

        int color = entity.getDomeColor();
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = (color) & 0xFF;

        double tile = Math.max(0.25, entity.getTextureTileSize());
        double uScale = (2.0 * Math.PI * radius) / tile;
        double vScale = (0.5 * Math.PI * radius) / tile;


        final double gateHeightRatio = 0.01;
        final double gateYCenter = radius * gateHeightRatio;
        final double gateYMin = gateYCenter - 1.0;
        final double gateYMax = gateYCenter + 1.0;

        final double rGate = Math.max(1.0e-3, Math.sqrt(Math.max(0.0, radius * radius - gateYCenter * gateYCenter)));
        final double gateWidthBlocks = 2.0;
        final double halfAngleGate = (gateWidthBlocks * 0.5) / rGate;

        final double[] gateAngles = new double[] { 0.0, Math.PI / 2.0, Math.PI, -Math.PI / 2.0 };

        for (int i = 0; i < RINGS; i++) {
            double t1r = (double) i / (double) RINGS;
            double t2r = (double) (i + 1) / (double) RINGS;

            double phi1 = t1r * (Math.PI / 2.0);
            double phi2 = t2r * (Math.PI / 2.0);

            double y1 = Math.sin(phi1) * radius;
            double y2 = Math.sin(phi2) * radius;

            double r1h = Math.cos(phi1) * radius;
            double r2h = Math.cos(phi2) * radius;

            float v1 = (float) (t1r * vScale + vScroll);
            float v2 = (float) (t2r * vScale + vScroll);

            boolean ringHitsGateY = Math.max(y1, y2) >= gateYMin && Math.min(y1, y2) <= gateYMax;

            for (int s = 0; s < SEGMENTS; s++) {
                double u1 = (double) s / (double) SEGMENTS;
                double u2 = (double) (s + 1) / (double) SEGMENTS;

                double th1 = u1 * Math.PI * 2.0;
                double th2 = u2 * Math.PI * 2.0;

                boolean inGate = false;
                if (ringHitsGateY) {
                    for (double ga : gateAngles) {
                        if (angleIntervalOverlapsSector(th1, th2, ga, halfAngleGate)) {
                            inGate = true;
                            break;
                        }
                    }
                }
                if (inGate) continue;

                double x11 = Math.cos(th1) * r1h;
                double z11 = Math.sin(th1) * r1h;

                double x12 = Math.cos(th2) * r1h;
                double z12 = Math.sin(th2) * r1h;

                double x21 = Math.cos(th1) * r2h;
                double z21 = Math.sin(th1) * r2h;

                double x22 = Math.cos(th2) * r2h;
                double z22 = Math.sin(th2) * r2h;

                float n11x = (float) (x11 / radius);
                float n11y = (float) (y1  / radius);
                float n11z = (float) (z11 / radius);

                float n12x = (float) (x12 / radius);
                float n12y = (float) (y1  / radius);
                float n12z = (float) (z12 / radius);

                float n21x = (float) (x21 / radius);
                float n21y = (float) (y2  / radius);
                float n21z = (float) (z21 / radius);

                float n22x = (float) (x22 / radius);
                float n22y = (float) (y2  / radius);
                float n22z = (float) (z22 / radius);

                float uu1 = (float) (u1 * uScale + uScroll);
                float uu2 = (float) (u2 * uScale + uScroll);

                vertex(vc, posMat, entry,
                        (float) x11, (float) y1, (float) z11,
                        uu1, v1,
                        r, g, b, a, overlay, light,
                        n11x, n11y, n11z);

                vertex(vc, posMat, entry,
                        (float) x21, (float) y2, (float) z21,
                        uu1, v2,
                        r, g, b, a, overlay, light,
                        n21x, n21y, n21z);

                vertex(vc, posMat, entry,
                        (float) x22, (float) y2, (float) z22,
                        uu2, v2,
                        r, g, b, a, overlay, light,
                        n22x, n22y, n22z);

                vertex(vc, posMat, entry,
                        (float) x12, (float) y1, (float) z12,
                        uu2, v1,
                        r, g, b, a, overlay, light,
                        n12x, n12y, n12z);
            }
        }

        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(DomeBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 256;
    }
}
