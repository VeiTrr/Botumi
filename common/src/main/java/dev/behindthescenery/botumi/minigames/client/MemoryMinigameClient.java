package dev.behindthescenery.botumi.minigames.client;

import dev.behindthescenery.botumi.client.ui.MinigameScreen;
import dev.behindthescenery.botumi.minigames.api.MinigameClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MemoryMinigameClient extends MinigameClient {
    private static final int GRID_SIZE = 3;
    private static final int TILE_SIZE = 42;
    private static final int TILE_SPACING = 6;
    private static final int TOTAL_STAGES = 3;

    private final List<Integer> sequence = new ArrayList<>();
    private final List<Integer> playerInput = new ArrayList<>();
    private final Random random = new Random();

    private int currentStage = 1;
    private boolean showingSequence = true;
    private long lastStepTime = 0L;
    private int sequenceDisplayIndex = 0;
    private int activeTile = -1;

    private ButtonWidget closeButton;
    private ButtonWidget startButton;

    public MemoryMinigameClient(MinigameScreen parent) {
        super(parent);
    }

    @Override
    protected void init() {
        super.init();
        if (handler.isCompletedClient()) {
            setSolved(true);
        } else {
            generateStageSequence();
            showingSequence = true;
            sequenceDisplayIndex = 0;
            lastStepTime = System.currentTimeMillis() + 500L;
        }

        int btnW = 80;
        int btnH = 20;
        int btnX = this.width / 2 - btnW / 2;
        int btnY = this.height - 32;

        closeButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.botumi.close"), btn -> {
            if (MinecraftClient.getInstance().currentScreen != null) {
                MinecraftClient.getInstance().currentScreen.close();
            }
        }).dimensions(btnX, btnY, btnW, btnH).build());

        if (handler.isCompletedClient()) {
            closeButton.visible = true;
        } else {
            closeButton.visible = false;
        }
    }

    private void generateStageSequence() {
        sequence.clear();
        playerInput.clear();
        int length = 2 + currentStage;
        for (int i = 0; i < length; i++) {
            sequence.add(random.nextInt(GRID_SIZE * GRID_SIZE));
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Draw Terminal Frame
        int panelW = 200;
        int panelH = 210;
        int panelX = centerX - panelW / 2;
        int panelY = centerY - panelH / 2 - 10;

        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xD012151C);
        ctx.drawBorder(panelX, panelY, panelW, panelH, 0xFF00ADB5);

        // Header Title
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("◆ MEMORY MATRIX ◆").formatted(Formatting.AQUA, Formatting.BOLD), centerX, panelY + 10, 0xFF00ADB5);

        if (handler.isCompletedClient()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("ACCESS GRANTED").formatted(Formatting.GREEN, Formatting.BOLD), centerX, centerY - 15, 0xFF55FF55);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.botumi.minigame.completed"), centerX, centerY + 5, 0xFFAAAAAA);
            closeButton.visible = true;
            return;
        }

        // Stage & Status bar
        Text statusText = showingSequence ?
                Text.literal("MEMORIZING... (" + (sequenceDisplayIndex) + "/" + sequence.size() + ")").formatted(Formatting.YELLOW) :
                Text.literal("YOUR TURN (" + playerInput.size() + "/" + sequence.size() + ")").formatted(Formatting.AQUA);

        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("STAGE " + currentStage + " / " + TOTAL_STAGES), centerX, panelY + 24, 0xFFEEEEEE);
        ctx.drawCenteredTextWithShadow(this.textRenderer, statusText, centerX, panelY + 36, 0xFFFFFFFF);

        // Update Sequence Animation Timer
        long now = System.currentTimeMillis();
        if (showingSequence) {
            if (now - lastStepTime >= 650L) {
                lastStepTime = now;
                if (sequenceDisplayIndex < sequence.size()) {
                    activeTile = sequence.get(sequenceDisplayIndex);
                    sequenceDisplayIndex++;
                    playTileSound();
                } else {
                    activeTile = -1;
                    showingSequence = false;
                    playerInput.clear();
                }
            } else if (now - lastStepTime >= 450L) {
                activeTile = -1;
            }
        }

        // Render 3x3 Tile Grid
        int gridTotal = GRID_SIZE * TILE_SIZE + (GRID_SIZE - 1) * TILE_SPACING;
        int gridStartX = centerX - gridTotal / 2;
        int gridStartY = centerY - gridTotal / 2 + 16;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                int index = r * GRID_SIZE + c;
                int tx = gridStartX + c * (TILE_SIZE + TILE_SPACING);
                int ty = gridStartY + r * (TILE_SIZE + TILE_SPACING);

                boolean isHovered = !showingSequence && mouseX >= tx && mouseX <= tx + TILE_SIZE && mouseY >= ty && mouseY <= ty + TILE_SIZE;
                boolean isLit = (activeTile == index);

                int bgColor;
                int borderColor;

                if (isLit) {
                    bgColor = 0xFF00ADB5;
                    borderColor = 0xFFEEEEEE;
                } else if (isHovered) {
                    bgColor = 0xFF2A394A;
                    borderColor = 0xFF00ADB5;
                } else {
                    bgColor = 0xFF1B2430;
                    borderColor = 0xFF394867;
                }

                ctx.fill(tx, ty, tx + TILE_SIZE, ty + TILE_SIZE, bgColor);
                ctx.drawBorder(tx, ty, TILE_SIZE, TILE_SIZE, borderColor);

                // Node center icon
                int markerColor = isLit ? 0xFFFFFFFF : 0xFF51627B;
                ctx.fill(tx + TILE_SIZE / 2 - 2, ty + TILE_SIZE / 2 - 2, tx + TILE_SIZE / 2 + 2, ty + TILE_SIZE / 2 + 2, markerColor);
            }
        }
    }

    private void playTileSound() {
        var mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 0.8F, 1.0F + (float) activeTile * 0.1F);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showingSequence || isSolved() || handler.isCompletedClient()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int gridTotal = GRID_SIZE * TILE_SIZE + (GRID_SIZE - 1) * TILE_SPACING;
        int gridStartX = centerX - gridTotal / 2;
        int gridStartY = centerY - gridTotal / 2 + 16;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                int index = r * GRID_SIZE + c;
                int tx = gridStartX + c * (TILE_SIZE + TILE_SPACING);
                int ty = gridStartY + r * (TILE_SIZE + TILE_SPACING);

                if (mouseX >= tx && mouseX <= tx + TILE_SIZE && mouseY >= ty && mouseY <= ty + TILE_SIZE) {
                    handleTilePress(index);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleTilePress(int index) {
        playerInput.add(index);
        activeTile = index;
        playTileSound();

        int currentStep = playerInput.size() - 1;
        if (sequence.get(currentStep) != index) {
            // Failure!
            handleFailure();
            return;
        }

        if (playerInput.size() == sequence.size()) {
            // Stage Complete!
            if (currentStage >= TOTAL_STAGES) {
                // Game Solved!
                setSolved(true);
                var mc = MinecraftClient.getInstance();
                if (mc.interactionManager != null) {
                    mc.interactionManager.clickButton(this.handler.syncId, 0);
                }
                if (mc.player != null) {
                    mc.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.2F);
                }
                closeButton.visible = true;
            } else {
                currentStage++;
                generateStageSequence();
                showingSequence = true;
                sequenceDisplayIndex = 0;
                lastStepTime = System.currentTimeMillis() + 600L;
            }
        }
    }

    private void handleFailure() {
        var mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.6F);
        }
        if (mc.interactionManager != null) {
            mc.interactionManager.clickButton(this.handler.syncId, 2);
        }
        // Reset current stage sequence
        playerInput.clear();
        showingSequence = true;
        sequenceDisplayIndex = 0;
        lastStepTime = System.currentTimeMillis() + 800L;
    }
}
