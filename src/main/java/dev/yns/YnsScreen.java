package dev.yns;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class YnsScreen extends Screen {
    private static final int OLIVE = 0xFF66752B;
    private static final int OLIVE_DARK = 0xFF252B12;
    private static final int OLIVE_LIGHT = 0xFF9DAF4B;
    private static final int PANEL = 0xF014180B;
    private static final int PANEL_2 = 0xE61C220E;
    private static final int TEXT = 0xFFF2F4E5;
    private static final int MUTED = 0xFFBFC6A1;

    private static final int PER_PAGE = 8;
    private static String currentCat = Category.ALL.get(0).id();
    private static int page = 0;

    private final Screen parent;
    private final YnsStore store;
    private int winX, winY, winW, winH;
    private volatile String status = "";

    private record Entry(String key, String label) {}

    public YnsScreen(Screen parent, YnsStore store) {
        super(Text.literal("YNS Styles"));
        this.parent = parent;
        this.store = store;
        refreshLibrary();
    }

    private void refreshLibrary() {
        status = "Texturen werden gesucht ...";
        Thread t = new Thread(() -> {
            YnsLibrary.scanLocal(YnsStore.STYLES_DIR);
            YnsLibrary.loadCloud(store.cloudBaseUrl);
            status = "";
            MinecraftClientAccess.execute(() -> {
                if (MinecraftClient.getInstance().currentScreen == this) clearAndInit();
            });
        }, "YNS-Scan");
        t.setDaemon(true);
        t.start();
    }

    private List<Entry> entries() {
        List<Entry> l = new ArrayList<>();
        l.add(new Entry("", "Standard (Vanilla)"));
        if (YnsLibrary.ownCats.contains(currentCat)) {
            l.add(new Entry("own:", "Eigene Texturen"));
        }
        for (String f : YnsLibrary.local.getOrDefault(currentCat, List.of())) {
            l.add(new Entry("local:" + f, "Pack: " + cut(f.replaceAll("(?i)\\.zip$", ""), 34)));
        }
        for (YnsCloud.Style s : YnsLibrary.cloud) {
            if (!s.category().equals(currentCat)) continue;
            String by = s.by().isEmpty() ? "" : " (" + s.by() + ")";
            l.add(new Entry("cloud:" + s.id(), "Cloud: " + cut(s.name() + by, 34)));
        }
        return l;
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    @Override
    protected void init() {
        winW = Math.min(900, this.width - 20);
        winH = Math.min(500, this.height - 20);
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;

        int gx = winX + 18, gy = winY + 70, cw = 142, ch = 21, step = 24;
        for (int i = 0; i < Category.ALL.size(); i++) {
            Category c = Category.ALL.get(i);
            boolean active = c.id().equals(currentCat);
            boolean has = !store.selected.getOrDefault(c.id(), "").isBlank();
            String label = (active ? "● " : "") + c.name() + (has ? "  ✓" : "");
            int x = gx + (i % 2) * (cw + 5);
            int y = gy + (i / 2) * step;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                currentCat = c.id();
                page = 0;
                clearAndInit();
            }).dimensions(x, y, cw, ch).build());
        }

        // Extras: eigener Bereich für Ordner, Aktualisieren und Reset
        addDrawableChild(ButtonWidget.builder(Text.literal("EXTRAS"), b ->
                client.setScreen(new YnsExtrasScreen(this, store)))
                .dimensions(winX + 18, winY + winH - 35, 88, 24).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Aktualisieren"), b -> refreshLibrary())
                .dimensions(winX + 112, winY + winH - 35, 108, 24).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Zurücksetzen"), b -> resetAll())
                .dimensions(winX + 226, winY + winH - 35, 108, 24).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Schließen"), b -> close())
                .dimensions(winX + winW - 108, winY + winH - 35, 90, 24).build());

        int rx = winX + 20 + 2 * (cw + 5) + 28;
        int rw = winX + winW - 18 - rx;
        List<Entry> list = entries();

        if ("totem".equals(currentCat)) {
            initTotemGrid(rx, rw, list);
        } else {
            int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
            page = Math.max(0, Math.min(page, pages - 1));
            int ry = winY + 92;
            String activeKey = store.selected.getOrDefault(currentCat, "");
            for (int i = page * PER_PAGE; i < Math.min(list.size(), (page + 1) * PER_PAGE); i++) {
                Entry e = list.get(i);
                boolean on = e.key().equals(activeKey);
                addDrawableChild(ButtonWidget.builder(Text.literal((on ? "● " : "") + e.label()),
                                b -> choose(e.key()))
                        .dimensions(rx, ry, rw, 23).build());
                ry += 27;
            }
            int py = winY + 92 + PER_PAGE * 27 + 4;
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page--; clearAndInit(); })
                    .dimensions(rx, py, 32, 20).build()).active = page > 0;
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page++; clearAndInit(); })
                    .dimensions(rx + 38, py, 32, 20).build()).active = page < pages - 1;
        }
    }

    private void initTotemGrid(int rx, int rw, List<Entry> list) {
        int columns = 3;
        int gap = 7;
        int bw = Math.max(70, (rw - gap * (columns - 1)) / columns);
        int y = winY + 95;
        String activeKey = store.selected.getOrDefault("totem", "");
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            int col = i % columns;
            int row = i / columns;
            int x = rx + col * (bw + gap);
            int yy = y + row * 42;
            boolean on = e.key().equals(activeKey);
            addDrawableChild(ButtonWidget.builder(Text.literal((on ? "● " : "") + cut(e.label(), 16)),
                            b -> choose(e.key()))
                    .dimensions(x, yy, bw, 34).build());
        }
    }

    private void choose(String key) {
        if (key.isEmpty()) store.selected.remove(currentCat);
        else store.selected.put(currentCat, key);
        store.save();
        YnsApplier.apply(store, s -> status = s);
        clearAndInit();
    }

    private void resetAll() {
        store.selected.clear();
        store.save();
        YnsApplier.apply(store, s -> status = s);
        clearAndInit();
    }

    private void clearAndInit() {
        clearChildren();
        init();
    }

    @Override
    public void close() {
        store.save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);

        // YNS olive UI
        ctx.fill(winX, winY, winX + winW, winY + winH, PANEL);
        ctx.fill(winX, winY, winX + winW, winY + 54, OLIVE_DARK);
        ctx.fill(winX, winY + 52, winX + winW, winY + 55, OLIVE);
        ctx.fill(winX, winY, winX + 4, winY + winH, OLIVE_LIGHT);

        ctx.drawTextWithShadow(textRenderer, "YNS Styles", winX + 22, winY + 11, TEXT);
        ctx.drawTextWithShadow(textRenderer, "Your textures. Your style.", winX + 22, winY + 29, MUTED);

        String badge = store.selected.size() + " aktiv";
        ctx.drawTextWithShadow(textRenderer, badge,
                winX + winW - 20 - textRenderer.getWidth(badge), winY + 20, OLIVE_LIGHT);

        int rx = winX + 20 + 2 * (142 + 5) + 28;
        ctx.fill(rx - 10, winY + 60, winX + winW - 12, winY + winH - 48, PANEL_2);
        ctx.fill(rx - 10, winY + 60, rx - 8, winY + winH - 48, OLIVE);

        Category c = Category.byId(currentCat);
        ctx.drawTextWithShadow(textRenderer, "KATEGORIEN", winX + 20, winY + 59, OLIVE_LIGHT);
        ctx.drawTextWithShadow(textRenderer,
                "STYLES  •  " + (c == null ? "" : c.name()),
                rx, winY + 70, OLIVE_LIGHT);

        if ("totem".equals(currentCat)) {
            ctx.drawTextWithShadow(textRenderer, "TOTEM-RASTER", rx, winY + 84, TEXT);
        }

        String st = status;
        if (st != null && !st.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, st, winX + 20, winY + winH - 55, TEXT);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }
}
