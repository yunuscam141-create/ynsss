package dev.yns;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class YnsExtrasScreen extends Screen {
    private static final int OLIVE = 0xFF66752B;
    private static final int OLIVE_DARK = 0xFF252B12;
    private static final int OLIVE_LIGHT = 0xFF9DAF4B;
    private static final int PANEL = 0xF014180B;
    private static final int TEXT = 0xFFF2F4E5;
    private static final int MUTED = 0xFFBFC6A1;

    private final Screen parent;
    private final YnsStore store;
    private int x, y, w, h;

    public YnsExtrasScreen(Screen parent, YnsStore store) {
        super(Text.literal("YNS Styles - Extras"));
        this.parent = parent;
        this.store = store;
    }

    @Override
    protected void init() {
        w = Math.min(620, width - 30);
        h = Math.min(360, height - 30);
        x = (width - w) / 2;
        y = (height - h) / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("📁 Style-Ordner öffnen"),
                b -> YnsStore.openStylesFolder())
                .dimensions(x + 30, y + 105, w - 60, 28).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("🖼 Eigene Texturen öffnen"),
                b -> YnsStore.openTexturesFolder())
                .dimensions(x + 30, y + 142, w - 60, 28).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Aktualisieren"),
                b -> YnsLibrary.scanLocal(YnsStore.STYLES_DIR))
                .dimensions(x + 30, y + 179, (w - 67) / 2, 28).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"),
                b -> close())
                .dimensions(x + 37 + (w - 67) / 2, y + 179, (w - 67) / 2, 28).build());
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(x, y, x + w, y + h, PANEL);
        ctx.fill(x, y, x + w, y + 54, OLIVE_DARK);
        ctx.fill(x, y + 52, x + w, y + 55, OLIVE);
        ctx.fill(x, y, x + 4, y + h, OLIVE_LIGHT);

        ctx.drawTextWithShadow(textRenderer, "YNS Styles  •  Extras", x + 24, y + 13, TEXT);
        ctx.drawTextWithShadow(textRenderer, "Eigene Packs und Einzeltexturen verwalten", x + 24, y + 31, MUTED);

        ctx.drawTextWithShadow(textRenderer, "Style-Ordner:", x + 30, y + 72, OLIVE_LIGHT);
        ctx.drawTextWithShadow(textRenderer, ".minecraft/yns/styles/", x + 145, y + 72, TEXT);

        ctx.drawTextWithShadow(textRenderer, "Einzeltexturen:", x + 30, y + 88, OLIVE_LIGHT);
        ctx.drawTextWithShadow(textRenderer, ".minecraft/yns/textures/", x + 145, y + 88, TEXT);

        ctx.drawTextWithShadow(textRenderer,
                "Du kannst mehrere Packs mischen: pro Kategorie eine eigene Quelle.",
                x + 30, y + 225, TEXT);
        ctx.drawTextWithShadow(textRenderer,
                "PNG-Dateien im Textur-Ordner erscheinen als „Eigene Texturen“.",
                x + 30, y + 242, MUTED);
        ctx.drawTextWithShadow(textRenderer,
                "Beim Anwenden wird daraus automatisch YNS-Active gebaut.",
                x + 30, y + 259, MUTED);

        super.render(ctx, mouseX, mouseY, delta);
    }
}
