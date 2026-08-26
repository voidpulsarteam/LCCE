package dev.voidpulsar.lc_claim_economy.client.gui;

import dev.voidpulsar.lc_claim_economy.client.ClientLedger;
import dev.voidpulsar.lc_claim_economy.data.LcClaimEconomySavedData;
import dev.voidpulsar.lc_claim_economy.network.LedgerLine;
import dev.voidpulsar.lc_claim_economy.network.RequestLedgerPayload;
import dev.voidpulsar.lc_claim_economy.util.MoneyMessageUtil;
import dev.voidpulsar.lc_claim_economy.util.MoneyUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Read-only scrollable view of the viewing player's own account ledger (see
 * {@link LcClaimEconomySavedData.LedgerEntry}) - claim purchases, unclaim
 * refunds, upkeep charges/misses, pioneer bonus, market sales/purchases.
 * Built with manual scissor-clipped scrolling rather than {@code
 * ObjectSelectionList}, matching {@link BankDashboardScreen}'s hand-rolled,
 * ftblibrary-free rendering so this screen works on either backend too.
 */
public final class TransactionHistoryScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_MIN_MARGIN = 20;
    private static final int HEADER_HEIGHT = 24;
    private static final int ROW_HEIGHT = 16;
    private static final int CONTENT_PAD = 10;
    private static final int BUTTON_HEIGHT = 20;
    private static final int FOOTER_HEIGHT = BUTTON_HEIGHT + CONTENT_PAD * 2;

    private static final int COLOR_PANEL_BG = 0xE62E3440;
    private static final int COLOR_HEADER_BG = 0xF03B4252;
    private static final int COLOR_DIVIDER = 0xFF434C5E;
    private static final int COLOR_TITLE = 0xFFECEFF4;
    private static final int COLOR_DETAIL = 0xFFD8DEE9;
    private static final int COLOR_TIME = 0xFF6C7893;
    private static final int COLOR_POSITIVE = 0xFFA3BE8C;
    private static final int COLOR_NEGATIVE = 0xFFBF616A;
    private static final int COLOR_NEUTRAL = 0xFFEBCB8B;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, HH:mm");

    private int panelX;
    private int panelY;
    private int panelHeight;
    private int contentTop;
    private int contentHeight;
    private int scrollOffset;

    public TransactionHistoryScreen() {
        super(Component.translatable("gui.lc_claim_economy.history.title"));
    }

    @Override
    protected void init() {
        PacketDistributor.sendToServer(new RequestLedgerPayload());

        panelHeight = height - PANEL_MIN_MARGIN;
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - panelHeight) / 2;
        contentTop = panelY + HEADER_HEIGHT + CONTENT_PAD;
        contentHeight = panelHeight - HEADER_HEIGHT - FOOTER_HEIGHT - CONTENT_PAD;
        scrollOffset = 0;

        int buttonY = panelY + panelHeight - FOOTER_HEIGHT + CONTENT_PAD;
        int buttonWidth = 90;
        int gap = 8;
        int totalWidth = buttonWidth * 2 + gap;
        int buttonsX = panelX + (PANEL_WIDTH - totalWidth) / 2;

        addRenderableWidget(Button.builder(
                Component.translatable("gui.lc_claim_economy.dashboard.refresh"),
                button -> PacketDistributor.sendToServer(new RequestLedgerPayload())
        ).bounds(buttonsX, buttonY, buttonWidth, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> onClose()
        ).bounds(buttonsX + buttonWidth + gap, buttonY, buttonWidth, BUTTON_HEIGHT).build());
    }

    public static void refreshIfOpen() {
        // No-op beyond re-rendering: entries() is read fresh every frame, and
        // scroll clamping happens in render() too - there's no cached row
        // list here that would otherwise go stale on a new sync.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);

        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, COLOR_PANEL_BG);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + HEADER_HEIGHT, COLOR_HEADER_BG);
        graphics.fill(panelX, panelY + HEADER_HEIGHT, panelX + PANEL_WIDTH, panelY + HEADER_HEIGHT + 1, COLOR_DIVIDER);
        graphics.drawCenteredString(font, title, panelX + PANEL_WIDTH / 2, panelY + 8, COLOR_TITLE);

        List<LedgerLine> entries = ClientLedger.entries();
        int maxScroll = Math.max(0, entries.size() * ROW_HEIGHT - contentHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        if (entries.isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("gui.lc_claim_economy.history.empty"),
                    panelX + PANEL_WIDTH / 2, contentTop, COLOR_TIME);
        } else {
            graphics.enableScissor(panelX, contentTop, panelX + PANEL_WIDTH, contentTop + contentHeight);
            int rowY = contentTop - scrollOffset;
            for (LedgerLine entry : entries) {
                if (rowY + ROW_HEIGHT >= contentTop && rowY <= contentTop + contentHeight) {
                    drawEntry(graphics, entry, panelX + CONTENT_PAD, rowY, PANEL_WIDTH - CONTENT_PAD * 2);
                }
                rowY += ROW_HEIGHT;
            }
            graphics.disableScissor();
        }

        int footerY = panelY + panelHeight - FOOTER_HEIGHT;
        graphics.fill(panelX, footerY, panelX + PANEL_WIDTH, footerY + 1, COLOR_DIVIDER);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawEntry(GuiGraphics graphics, LedgerLine entry, int x, int y, int width) {
        Component detail = Component.translatable(entry.detailKey());
        graphics.drawString(font, detail, x, y, COLOR_DETAIL, false);

        Component timeText = Component.literal(dateFormat.format(new Date(entry.timestamp())));
        graphics.drawString(font, timeText, x, y + font.lineHeight, COLOR_TIME, false);

        if (entry.copperDelta() != 0L) {
            boolean positive = entry.copperDelta() > 0L;
            Component amount = Component.literal((positive ? "+" : "-"))
                    .append(MoneyMessageUtil.formatValue(MoneyUtil.fromCopper(Math.abs(entry.copperDelta()))));
            int color = positive ? COLOR_POSITIVE : COLOR_NEGATIVE;
            int amountWidth = font.width(amount);
            graphics.drawString(font, amount, x + width - amountWidth, y, color, false);
        } else if (entry.kind() == LcClaimEconomySavedData.LedgerKind.UPKEEP_MISSED) {
            Component missed = Component.translatable("gui.lc_claim_economy.history.missed").withStyle(ChatFormatting.ITALIC);
            int missedWidth = font.width(missed);
            graphics.drawString(font, missed, x + width - missedWidth, y, COLOR_NEUTRAL, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= (int) (scrollY * ROW_HEIGHT * 2);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
