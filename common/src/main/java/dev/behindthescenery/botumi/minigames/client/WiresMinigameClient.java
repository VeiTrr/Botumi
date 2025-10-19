package dev.behindthescenery.botumi.minigames.client;

import dev.behindthescenery.botumi.client.ui.MinigameScreen;
import dev.behindthescenery.botumi.minigames.api.MinigameClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WiresMinigameClient extends MinigameClient {
    private static final int SOCKET_RADIUS = 6;
    private static final int LINE_WIDTH = 2;

    private static final int WIRES = 5;
    private static final int[] BASE_COLORS = new int[]{
            Color.RED.getRGB(),
            Color.BLUE.getRGB(),
            Color.GREEN.getRGB(),
            Color.YELLOW.getRGB(),
            Color.CYAN.getRGB()
    };

    private final int[][] leftSockets = new int[WIRES][2];
    private final int[][] rightSockets = new int[WIRES][2];

    private final int[] leftColors = new int[WIRES];
    private final int[] rightColors = new int[WIRES];

    private final int[] connection = new int[WIRES];

    private int dragging = -1;
    private int dragX = 0, dragY = 0;

    private ButtonWidget finishButton;

    public WiresMinigameClient(MinigameScreen parent) {
        super(parent);
        Arrays.fill(connection, -1);
        System.arraycopy(BASE_COLORS, 0, leftColors, 0, WIRES);
        List<Integer> mix = new ArrayList<>(WIRES);
        for (int i = 0; i < WIRES; i++) mix.add(i);
        Collections.shuffle(mix);
        for (int i = 0; i < WIRES; i++) rightColors[i] = leftColors[mix.get(i)];
    }

    @Override
    protected void init() {
        super.init();
        if (handler.isCompletedClient()) {
            setSolved(true);

        } else {
            layout();
            int finishW = 80;
            int finishX = this.width / 2 - finishW / 2;
            int finishY = this.height - 30;

            int finishH = 20;
            if (finishButton == null) {
                finishButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.botumi.finish"), button -> {
                    if (handler.isCompletedClient()) {
                        if (MinecraftClient.getInstance().currentScreen != null) {
                            MinecraftClient.getInstance().currentScreen.close();
                        }
                    } else {
                        if (isSolved()) {
                            if (MinecraftClient.getInstance().interactionManager != null) {
                                MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 0);
                                setSolved(true);
                            }
                        } else {
                            if (MinecraftClient.getInstance().interactionManager != null) {
                                MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 2);
                                setSolved(false);
                            }
                            if (MinecraftClient.getInstance().currentScreen != null) {
                                MinecraftClient.getInstance().currentScreen.close();
                            }
                        }
                    }
                }).dimensions(finishX, finishY, finishW, finishH).build());
            } else {
                finishButton.setDimensionsAndPosition(finishX, finishY, finishW, finishH);
            }

            updateFinishVisibility();
        }
    }

    private void layout() {
        int leftX = this.width / 2 - 80;
        int rightX = this.width / 2 + 80;
        int top = 40;
        int spacing = Math.max(20, (this.height - 2 * top) / (WIRES + 1));
        for (int i = 0; i < WIRES; i++) {
            int y = top + (i + 1) * spacing;
            leftSockets[i][0] = leftX;
            leftSockets[i][1] = y;
            rightSockets[i][0] = rightX;
            rightSockets[i][1] = y;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        if (handler.isCompletedClient()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.botumi.minigame.completed"), this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
            if (finishButton != null) {
                finishButton.setMessage(Text.translatable("screen.botumi.close"));
            }
            return;
        }

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.getTitle(), this.width / 2, 12, 0xFFFFFFFF);

        for (int i = 0; i < WIRES; i++) {
            int j = connection[i];
            if (j >= 0) {
                int color = leftColors[i] | 0xFF000000;
                drawThickLine(ctx,
                        leftSockets[i][0], leftSockets[i][1],
                        rightSockets[j][0], rightSockets[j][1],
                        color);
            }
        }

        if (dragging >= 0) {
            int color = leftColors[dragging] | 0xFF000000;
            drawThickLine(ctx,
                    leftSockets[dragging][0], leftSockets[dragging][1],
                    dragX, dragY, color);
        }

        for (int i = 0; i < WIRES; i++) {
            drawSocket(ctx, leftSockets[i][0], leftSockets[i][1], leftColors[i]);
            drawSocket(ctx, rightSockets[i][0], rightSockets[i][1], rightColors[i]);
        }
    }

    private void drawSocket(DrawContext ctx, int cx, int cy, int color) {
        int r = SOCKET_RADIUS;
        ctx.fill(cx - r - 1, cy - r - 1, cx + r + 1, cy + r + 1, 0xFF000000);
        ctx.fill(cx - r, cy - r, cx + r, cy + r, color | 0xFF000000);
    }

    private void drawThickLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) {
            ctx.fill(x1 - WiresMinigameClient.LINE_WIDTH / 2, y1 - WiresMinigameClient.LINE_WIDTH / 2, x1 + WiresMinigameClient.LINE_WIDTH / 2 + 1, y1 + WiresMinigameClient.LINE_WIDTH / 2 + 1, color);
            return;
        }
        float sx = dx / (float) steps;
        float sy = dy / (float) steps;
        float fx = x1, fy = y1;
        for (int i = 0; i <= steps; i++) {
            int cx = Math.round(fx);
            int cy = Math.round(fy);
            ctx.fill(cx - WiresMinigameClient.LINE_WIDTH / 2, cy - WiresMinigameClient.LINE_WIDTH / 2, cx + (WiresMinigameClient.LINE_WIDTH + 1) / 2, cy + (WiresMinigameClient.LINE_WIDTH + 1) / 2, color);
            fx += sx;
            fy += sy;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int li = hitSocket((int) mouseX, (int) mouseY, true);
            if (li >= 0) {
                connection[li] = -1;
                dragging = li;
                dragX = (int) mouseX;
                dragY = (int) mouseY;
                updateFinishVisibility();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging >= 0 && button == 0) {
            dragX = (int) mouseX;
            dragY = (int) mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging >= 0 && button == 0) {
            int rj = hitSocket((int) mouseX, (int) mouseY, false);
            if (rj >= 0 && !isRightUsed(rj)) {
                connection[dragging] = rj;
            }
            dragging = -1;

            if (isSolved()) {
                setSolved(true);
            }
            updateFinishVisibility();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int hitSocket(int x, int y, boolean leftSide) {
        int[][] sockets = leftSide ? leftSockets : rightSockets;
        for (int i = 0; i < WIRES; i++) {
            int cx = sockets[i][0], cy = sockets[i][1];
            int dx = x - cx, dy = y - cy;
            if (dx * dx + dy * dy <= (SOCKET_RADIUS + 2) * (SOCKET_RADIUS + 2)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isRightUsed(int rightIndex) {
        for (int c : connection) if (c == rightIndex) return true;
        return false;
    }

    @Override
    public boolean isSolved() {
        for (int i = 0; i < WIRES; i++) {
            int j = connection[i];
            if (j < 0) return false;
            if (leftColors[i] != rightColors[j]) return false;
        }
        return true;
    }

    private void updateFinishVisibility() {
        boolean allConnected = true;
        for (int c : connection) {
            if (c < 0) {
                allConnected = false;
                break;
            }
        }
        if (finishButton != null) {
            finishButton.visible = allConnected;
            finishButton.active = allConnected;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (isSolved()) {
            setSolved(true);
        }
        updateFinishVisibility();
    }
}
