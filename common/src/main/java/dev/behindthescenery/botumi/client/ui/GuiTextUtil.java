package dev.behindthescenery.botumi.client.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public class GuiTextUtil {

    /**
     * Renders text wrapped to fit within maxWidth. If lines exceed maxHeight,
     * enables scissor clipping and renders a sleek scrollbar on the right.
     *
     * @param context DrawContext for rendering
     * @param textRenderer TextRenderer
     * @param text The text to render
     * @param x Left X coordinate of the content box
     * @param y Top Y coordinate of the content box
     * @param width Width of the content box
     * @param height Height of the content box
     * @param scrollOffset Current scroll offset in pixels (0.0 to maxScroll)
     * @param color Default text color
     * @param shadow Whether to render text shadow
     * @return Total height of the wrapped text in pixels
     */
    public static int drawScrollableText(
            DrawContext context,
            TextRenderer textRenderer,
            Text text,
            int x,
            int y,
            int width,
            int height,
            float scrollOffset,
            int color,
            boolean shadow
    ) {
        int contentWidth = width;
        List<OrderedText> lines = textRenderer.wrapLines(text, contentWidth);
        int lineHeight = textRenderer.fontHeight + 3;
        int totalHeight = lines.size() * lineHeight;

        boolean needsScroll = totalHeight > height;
        if (needsScroll) {
            contentWidth = width - 8; // Leave 8px for scrollbar
            lines = textRenderer.wrapLines(text, contentWidth);
            totalHeight = lines.size() * lineHeight;
        }

        context.enableScissor(x, y, x + width, y + height);

        int drawY = y - (int) scrollOffset;
        for (OrderedText line : lines) {
            if (drawY + lineHeight > y && drawY < y + height) {
                context.drawText(textRenderer, line, x, drawY, color, shadow);
            }
            drawY += lineHeight;
        }

        context.disableScissor();

        // Render sleek sci-fi scrollbar if needed
        if (needsScroll) {
            int scrollbarX = x + width - 4;
            int scrollbarW = 3;
            // Scrollbar track
            context.fill(scrollbarX, y, scrollbarX + scrollbarW, y + height, 0x3000ADB5);

            float maxScroll = Math.max(1, totalHeight - height);
            float ratio = (float) height / (float) totalHeight;
            int thumbH = Math.max(12, (int) (height * ratio));
            int thumbY = y + (int) ((height - thumbH) * MathHelper.clamp(scrollOffset / maxScroll, 0.0f, 1.0f));

            // Scrollbar thumb
            context.fill(scrollbarX, thumbY, scrollbarX + scrollbarW, thumbY + thumbH, 0xFF00ADB5);
        }

        return totalHeight;
    }

    /**
     * Helper to wrap and draw multi-line text without scrolling, guaranteed to fit inside maxWidth.
     */
    public static int drawWrappedText(
            DrawContext context,
            TextRenderer textRenderer,
            Text text,
            int x,
            int y,
            int maxWidth,
            int color,
            boolean shadow
    ) {
        List<OrderedText> lines = textRenderer.wrapLines(text, maxWidth);
        int lineHeight = textRenderer.fontHeight + 3;
        int currentY = y;
        for (OrderedText line : lines) {
            context.drawText(textRenderer, line, x, currentY, color, shadow);
            currentY += lineHeight;
        }
        return lines.size() * lineHeight;
    }
}
