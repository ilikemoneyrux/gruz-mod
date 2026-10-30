package pl.gruzclient.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.Setting;
import pl.gruzclient.render.Ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static pl.gruzclient.render.GruzDraw.*;

/**
 * Lista modów Gruz w nowym stylu: duży panel z kategoriami po lewej, wyszukiwarką u góry
 * i kartami modułów (ikona, nazwa, opis, przycisk WŁ./WYŁ., ⚙ ustawienia).
 * Ustawienia modułu otwierają się w tym samym panelu.
 */
public class GruzModsScreen extends Screen {
    private final Screen parent;
    private final long openedAt = System.currentTimeMillis();
    private static Module.Category category = null; // null = wszystkie
    private Module settingsOf = null;
    private Setting.Key capturing = null;
    private Setting.Number dragging = null;
    private int dragX, dragW;
    private float scroll = 0, scrollTarget = 0;
    private long last = 0;
    private TextFieldWidget search;

    // układ
    private int px, py, pw, ph, sideW = 96, contentX, contentY, contentW, contentH;
    private int cols, cardW, cardH = 74;
    private static final int GAP = 6;

    public GruzModsScreen(Screen parent) { this(parent, (Module.Category) null); }

    public GruzModsScreen(Screen parent, Module.Category cat) {
        super(Text.literal("Gruz Mods"));
        this.parent = parent;
        if (cat != null) category = cat;
    }

    /** Otwiera od razu ustawienia modułu (np. ⚙ w edytorze HUD) */
    public GruzModsScreen(Screen parent, Module open) {
        this(parent, (Module.Category) null);
        this.settingsOf = open;
    }

    private static final Map<String, Ui.Icon> ICON = Map.ofEntries(
            Map.entry("minimap", Ui.Icon.MAP), Map.entry("zoom", Ui.Icon.SEARCH), Map.entry("freelook", Ui.Icon.EYE),
            Map.entry("freecam", Ui.Icon.CAMERA), Map.entry("cosmetics", Ui.Icon.SHIRT), Map.entry("keystrokes", Ui.Icon.GRID),
            Map.entry("targethud", Ui.Icon.USER), Map.entry("combo", Ui.Icon.TROPHY), Map.entry("fight", Ui.Icon.TROPHY),
            Map.entry("coords", Ui.Icon.MOVE), Map.entry("menu", Ui.Icon.LAYOUT), Map.entry("autogg", Ui.Icon.CHAT),
            Map.entry("betterchat", Ui.Icon.CHAT), Map.entry("tab", Ui.Icon.USERS), Map.entry("clip", Ui.Icon.CAMERA),
            Map.entry("recording", Ui.Icon.CAMERA), Map.entry("crosshair", Ui.Icon.PLUS), Map.entry("clock", Ui.Icon.GLOBE),
            Map.entry("hitboxes", Ui.Icon.EYE), Map.entry("nametags", Ui.Icon.USER), Map.entry("scoreboard", Ui.Icon.LAYOUT),
            Map.entry("hudTweaks", Ui.Icon.LAYOUT));

    private static Ui.Icon iconOf(Module m) {
        Ui.Icon i = ICON.get(m.id);
        if (i != null) return i;
        return switch (m.category) {
            case HUD -> Ui.Icon.LAYOUT;
            case PVP -> Ui.Icon.TROPHY;
            case VISUAL -> Ui.Icon.EYE;
            case UTILITY -> Ui.Icon.GEAR;
            case COSMETICS -> Ui.Icon.SHIRT;
        };
    }

    private static Ui.Icon catIcon(Module.Category c) {
        if (c == null) return Ui.Icon.GRID;
        return switch (c) {
            case HUD -> Ui.Icon.LAYOUT;
            case PVP -> Ui.Icon.TROPHY;
            case VISUAL -> Ui.Icon.EYE;
            case UTILITY -> Ui.Icon.GEAR;
            case COSMETICS -> Ui.Icon.SHIRT;
        };
    }

    @Override
    protected void init() {
        pw = Math.min(600, this.width - 30);
        ph = Math.min(340, this.height - 30);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        contentX = px + sideW + 10;
        contentY = py + 44;
        contentW = pw - sideW - 20;
        contentH = ph - 54;
        cols = Math.max(1, Math.min(4, (contentW + GAP) / (128 + GAP)));
        cardW = (contentW - (cols - 1) * GAP) / cols;

        String prev = search != null ? search.getText() : "";
        search = new TextFieldWidget(this.textRenderer, contentX + 22, py + 16, Math.min(200, contentW - 140), 12, Text.literal(""));
        search.setDrawsBackground(false);
        search.setMaxLength(40);
        search.setText(prev);
        search.setPlaceholder(Ui.text(Lang.t("mods.search")).withColor(MUTE));
        search.setChangedListener(s -> { scroll = scrollTarget = 0; settingsOf = null; });
        this.addDrawableChild(search);

        this.addDrawableChild(new UiButton(px + pw - 10 - 20, py + 10, 20, 20, Ui.Icon.CLOSE, null, b -> close()).kind(UiButton.Kind.ICON));
        this.addDrawableChild(new UiButton(px + pw - 36 - 92, py + 10, 92, 20, Ui.Icon.MOVE, Lang.t("mods.editHud"),
                b -> this.client.setScreen(new HudEditorScreen(this))));
    }

    @Override
    public void close() {
        Modules.save();
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return this.client.world == null || this.client.isInSingleplayer(); }

    private List<Module> mods() {
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : Modules.ALL) {
            if (!q.isEmpty()) {
                if (m.name().toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
            } else if (category == null || m.category == category) out.add(m);
        }
        return out;
    }

    private float reveal(long delay) { return outExpo(clamp01((System.currentTimeMillis() - openedAt - delay) / 500f)); }

    // ---------------- rysowanie ----------------
    @Override
    public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) {
        if (this.client.world != null) {
            this.applyBlur(delta);
            c.fill(0, 0, this.width, this.height, 0x66000000);
        } else {
            this.renderPanoramaBackground(c, delta);
            this.applyBlur(delta);
            c.fill(0, 0, this.width, this.height, 0x99000000);
        }
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        scroll = damp(scroll, scrollTarget, 16f, dt);
        this.renderBackground(c, mouseX, mouseY, delta);
        float r = reveal(0);
        int lift = Math.round((1f - r) * 10);

        // główny panel
        Ui.round(c, px, py + lift, pw, ph, 6, argb(r * 0.9f, 0x09070E));
        Ui.roundBorder(c, px, py + lift, pw, ph, 6, argb(r * 0.12f, 0xFFFFFF));
        // pasek boczny
        Ui.round(c, px + 4, py + 4 + lift, sideW, ph - 8, 5, argb(r * 0.7f, 0x110D19));
        sprite(c, LOGO, 256, px + 12, py + 11 + lift, 18, 18, r, false, 0f);
        Ui.draw(c, Ui.bold("GRUZ"), px + 34, py + 12 + lift, argb(r, 0xFFFFFF));
        Ui.draw(c, Ui.text("Mods"), px + 34, py + 21 + lift, argb(r, NEON_HOT));

        Module.Category[] cats = {null, Module.Category.HUD, Module.Category.PVP, Module.Category.VISUAL, Module.Category.UTILITY, Module.Category.COSMETICS};
        for (int i = 0; i < cats.length; i++) {
            int cy = py + 44 + i * 24 + lift;
            boolean on = settingsOf == null && search.getText().isBlank() && cats[i] == category;
            boolean hot = Ui.inside(mouseX, mouseY, px + 8, cy, sideW - 8, 20);
            if (on || hot) Ui.round(c, px + 8, cy, sideW - 8, 20, 4, argb(r * (on ? 0.95f : 0.5f), on ? 0x24163A : 0x1A1326));
            if (on) c.fill(px + 8, cy + 5, px + 10, cy + 15, argb(r, NEON));
            Ui.icon(c, catIcon(cats[i]), px + 15, cy + 5, 10, argb(r, on ? NEON_HOT : 0xB8AECB));
            String label = cats[i] == null ? Lang.t("mods.cat.all") : Lang.t("mods.cat." + cats[i].name().toLowerCase(Locale.ROOT));
            Ui.draw(c, Ui.text(label), px + 31, cy + 6, argb(r, on ? 0xFFFFFF : 0xD6CCE6));
        }
        long on = Modules.ALL.stream().filter(Module::isEnabled).count();
        Ui.draw(c, Ui.text(Lang.t("mods.enabledCount").replace("{n}", String.valueOf(on)).replace("{all}", String.valueOf(Modules.ALL.size()))),
                px + 12, py + ph - 18 + lift, argb(r * 0.8f, MUTE));

        // wyszukiwarka
        int sw = Math.min(200, contentW - 140) + 26;
        Ui.round(c, contentX, py + 10 + lift, sw, 20, 4, argb(r * 0.9f, 0x16111F));
        Ui.roundBorder(c, contentX, py + 10 + lift, sw, 20, 4, argb(r * (search.isFocused() ? 0.9f : 0.12f), search.isFocused() ? NEON : 0xFFFFFF));
        Ui.icon(c, Ui.Icon.SEARCH, contentX + 7, py + 15 + lift, 10, argb(r, MUTE));

        // zawartość
        c.enableScissor(contentX - 2, contentY, contentX + contentW + 2, contentY + contentH);
        if (settingsOf != null) drawSettings(c, mouseX, mouseY, r);
        else drawCards(c, mouseX, mouseY, r);
        c.disableScissor();

        for (var child : this.children()) if (child instanceof UiButton b) b.render(c, mouseX, mouseY, delta);
        search.render(c, mouseX, mouseY, delta);
    }

    private void drawCards(DrawContext c, int mouseX, int mouseY, float r) {
        List<Module> list = mods();
        if (list.isEmpty()) {
            Ui.drawCentered(c, Ui.text(Lang.t("mods.nothing")), contentX + contentW / 2, contentY + 40, argb(r, MUTE));
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            float a = clamp01(reveal(40 + Math.min(i, 16) * 25L));
            int x = contentX + (i % cols) * (cardW + GAP);
            int y = contentY + (i / cols) * (cardH + GAP) - Math.round(scroll) + Math.round((1f - a) * 6);
            if (y + cardH < contentY || y > contentY + contentH) continue;
            boolean hot = Ui.inside(mouseX, mouseY, x, y, cardW, cardH) && mouseY >= contentY && mouseY < contentY + contentH;
            boolean en = m.isEnabled();
            Ui.round(c, x, y, cardW, cardH, 5, argb(a * 0.95f, hot ? 0x1A1426 : 0x130F1B));
            Ui.roundBorder(c, x, y, cardW, cardH, 5, argb(a * (hot ? 0.6f : 0.1f), hot ? NEON : 0xFFFFFF));
            // ikona w kółku
            Ui.round(c, x + 8, y + 8, 20, 20, 10, argb(a, en ? mix(NEON, 0x000000, 0.55f) : 0x221A2E));
            Ui.icon(c, iconOf(m), x + 13, y + 13, 10, argb(a, en ? NEON_HOT : 0x9488A8));
            // ⚙
            if (!m.settings().isEmpty()) {
                boolean gh = Ui.inside(mouseX, mouseY, x + cardW - 20, y + 6, 14, 14);
                Ui.icon(c, Ui.Icon.GEAR, x + cardW - 18, y + 8, 10, argb(a, gh ? NEON_HOT : 0x8A7FA0));
            }
            Ui.draw(c, Ui.fit(m.name(), cardW - 44, true), x + 34, y + 9, argb(a, 0xFFFFFF));
            Ui.draw(c, Ui.fit(Lang.t("mods.cat." + m.category.name().toLowerCase(Locale.ROOT)), cardW - 44, false), x + 34, y + 19, argb(a * 0.8f, MUTE));
            List<OrderedText> desc = Ui.tr().wrapLines(Ui.text(m.description()), cardW - 16);
            for (int k = 0; k < Math.min(2, desc.size()); k++) c.drawText(Ui.tr(), desc.get(k), x + 8, y + 33 + k * 9, argb(a * 0.85f, 0xB8AECB), false);
            // przycisk WŁ./WYŁ.
            int by = y + cardH - 18;
            boolean bh = Ui.inside(mouseX, mouseY, x + 6, by, cardW - 12, 13);
            if (en) Ui.hGradient(c, x + 6, by, cardW - 12, 13, bh ? NEON_HOT : NEON, NEON_DEEP, a, 3);
            else Ui.round(c, x + 6, by, cardW - 12, 13, 3, argb(a, bh ? 0x2C2238 : 0x211A2B));
            Text st = Ui.bold(Lang.t(en ? "mods.enabled" : "mods.disabled"));
            Ui.drawCentered(c, st, x + cardW / 2, by + 3, argb(a, en ? 0xFFFFFF : 0x9488A8));
        }
    }

    // ---------------- ustawienia modułu ----------------
    private int rowsTop() { return contentY + 42; }

    private void drawSettings(DrawContext c, int mouseX, int mouseY, float r) {
        Module m = settingsOf;
        int x = contentX, y = contentY - Math.round(scroll);
        boolean backHot = Ui.inside(mouseX, mouseY, x, y, 16, 16);
        Ui.round(c, x, y, 16, 16, 4, argb(r, backHot ? 0x24163A : 0x16111F));
        Ui.icon(c, Ui.Icon.BACK, x + 4, y + 4, 8, argb(r, backHot ? NEON_HOT : 0xD6CCE6));
        Ui.icon(c, iconOf(m), x + 24, y + 3, 10, argb(r, NEON_HOT));
        Ui.draw(c, Ui.bold(m.name()), x + 38, y + 4, argb(r, 0xFFFFFF));
        // przełącznik modułu obok tytułu
        drawSwitch(c, x + contentW - 26, y + 3, m.isEnabled(), r, Ui.inside(mouseX, mouseY, x + contentW - 26, y + 3, 22, 11));
        List<OrderedText> desc = Ui.tr().wrapLines(Ui.text(m.description()), contentW - 10);
        if (!desc.isEmpty()) c.drawText(Ui.tr(), desc.get(0), x, y + 22, argb(r * 0.85f, MUTE), false);

        List<Setting> list = m.settings();
        int rowY = rowsTop() - Math.round(scroll);
        if (list.isEmpty()) {
            Ui.draw(c, Ui.text(Lang.t("mods.noSettings")), x, rowY, argb(r, MUTE));
            return;
        }
        int ctrlW = Math.min(130, contentW / 2), ctrlX = x + contentW - ctrlW - 8;
        for (Setting s : list) {
            boolean hot = Ui.inside(mouseX, mouseY, x, rowY, contentW, 22);
            Ui.round(c, x, rowY, contentW, 22, 4, argb(r * (hot ? 0.95f : 0.75f), hot ? 0x1A1426 : 0x130F1B));
            Ui.draw(c, Ui.fit(s.label(), ctrlX - x - 16, false), x + 10, rowY + 7, argb(r, 0xE6E0F0));
            int cy = rowY + 5;
            if (s instanceof Setting.Bool b) {
                drawSwitch(c, ctrlX + ctrlW - 22, cy + 1, b.value, r, hot);
            } else if (s instanceof Setting.Number n) {
                Text v = Ui.bold(n.display());
                int vw = Ui.width(v), bw = ctrlW - vw - 10;
                c.fill(ctrlX, cy + 5, ctrlX + bw, cy + 7, argb(r, 0x2B2139));
                int fx = ctrlX + Math.round(bw * n.fraction());
                Ui.hGradient(c, ctrlX, cy + 5, Math.max(1, fx - ctrlX), 2, NEON_DEEP, NEON, r, 0);
                Ui.round(c, fx - 3, cy + 3, 6, 6, 3, argb(r, 0xFFFFFF));
                Ui.draw(c, v, ctrlX + ctrlW - vw, cy + 2, argb(r, 0xFFFFFF));
            } else if (s instanceof Setting.Key k) {
                boolean cap = capturing == k;
                Ui.round(c, ctrlX, cy - 1, ctrlW, 14, 3, argb(r, cap ? 0x2A1446 : 0x1C1626));
                Ui.roundBorder(c, ctrlX, cy - 1, ctrlW, 14, 3, argb(r * (cap ? 1f : 0.15f), cap ? NEON : 0xFFFFFF));
                Ui.drawCentered(c, Ui.fit(cap ? Lang.t("mods.pressKey") : k.display(), ctrlW - 8, true), ctrlX + ctrlW / 2, cy + 2, argb(r, cap ? NEON_HOT : 0xFFFFFF));
            } else if (s instanceof Setting.Action) {
                Ui.hGradient(c, ctrlX, cy - 1, ctrlW, 14, NEON, NEON_DEEP, r, 3);
                Ui.drawCentered(c, Ui.bold(Lang.t("mods.open")), ctrlX + ctrlW / 2, cy + 2, argb(r, 0xFFFFFF));
            } else if (s instanceof Setting.Text tx) {
                Ui.round(c, ctrlX, cy - 1, ctrlW, 14, 3, argb(r, 0x1C1626));
                Ui.icon(c, Ui.Icon.PENCIL, ctrlX + ctrlW - 12, cy + 2, 8, argb(r, MUTE));
                Ui.draw(c, Ui.fit(tx.display().isEmpty() ? "—" : tx.display(), ctrlW - 20, false), ctrlX + 6, cy + 2, argb(r, 0xFFFFFF));
            } else if (s instanceof Setting.Choice ch) {
                Ui.round(c, ctrlX, cy - 1, ctrlW, 14, 3, argb(r, 0x1C1626));
                Ui.icon(c, Ui.Icon.BACK, ctrlX + 4, cy + 2, 7, argb(r, NEON_HOT));
                c.getMatrices().push();
                c.getMatrices().translate(ctrlX + ctrlW - 4, cy + 9, 0);
                c.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180));
                Ui.icon(c, Ui.Icon.BACK, 0, 0, 7, argb(r, NEON_HOT));
                c.getMatrices().pop();
                Text v = Ui.fit(ch.display(), ctrlW - 34, true);
                int sx = ctrlX + (ctrlW - Ui.width(v)) / 2;
                if (s instanceof Setting.Color col) {
                    Ui.round(c, sx - 10, cy + 2, 7, 7, 2, 0xFF000000 | col.rgb());
                    sx += 4;
                }
                Ui.draw(c, v, sx, cy + 2, argb(r, 0xFFFFFF));
            }
            rowY += 26;
        }
    }

    private void drawSwitch(DrawContext c, int x, int y, boolean on, float a, boolean hot) {
        Ui.round(c, x, y, 22, 11, 5, argb(a, on ? (hot ? NEON_HOT : NEON) : (hot ? 0x3A3048 : 0x2B2139)));
        int kx = on ? x + 12 : x + 1;
        Ui.round(c, kx, y + 1, 9, 9, 4, argb(a, 0xFFFFFF));
    }

    // ---------------- wejście ----------------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        capturing = null;
        // kategorie
        Module.Category[] cats = {null, Module.Category.HUD, Module.Category.PVP, Module.Category.VISUAL, Module.Category.UTILITY, Module.Category.COSMETICS};
        for (int i = 0; i < cats.length; i++) {
            if (Ui.inside(mx, my, px + 8, py + 44 + i * 24, sideW - 8, 20)) {
                category = cats[i];
                settingsOf = null;
                search.setText("");
                scroll = scrollTarget = 0;
                return true;
            }
        }
        if (!Ui.inside(mx, my, contentX, contentY, contentW, contentH)) return false;

        if (settingsOf != null) return clickSettings(mx, my);

        List<Module> list = mods();
        for (int i = 0; i < list.size(); i++) {
            int x = contentX + (i % cols) * (cardW + GAP);
            int y = contentY + (i / cols) * (cardH + GAP) - Math.round(scroll);
            if (!Ui.inside(mx, my, x, y, cardW, cardH)) continue;
            Module m = list.get(i);
            if (Ui.inside(mx, my, x + 6, y + cardH - 18, cardW - 12, 13)) { m.toggle(); Modules.save(); return true; }
            if (!m.settings().isEmpty() && (Ui.inside(mx, my, x + cardW - 20, y + 6, 14, 14) || button == 1)) { openSettings(m); return true; }
            if (!m.settings().isEmpty()) openSettings(m); else { m.toggle(); Modules.save(); }
            return true;
        }
        return false;
    }

    private void openSettings(Module m) {
        settingsOf = m;
        scroll = scrollTarget = 0;
    }

    private boolean clickSettings(double mx, double my) {
        Module m = settingsOf;
        int x = contentX, y = contentY - Math.round(scroll);
        if (Ui.inside(mx, my, x, y, 16, 16)) { settingsOf = null; scroll = scrollTarget = 0; return true; }
        if (Ui.inside(mx, my, x + contentW - 26, y + 3, 22, 11)) { m.toggle(); Modules.save(); return true; }
        int rowY = rowsTop() - Math.round(scroll);
        int ctrlW = Math.min(130, contentW / 2), ctrlX = x + contentW - ctrlW - 8;
        for (Setting s : m.settings()) {
            if (Ui.inside(mx, my, x, rowY, contentW, 22)) {
                if (s instanceof Setting.Action a) { Modules.save(); a.action.run(); return true; }
                if (s instanceof Setting.Text tx) { this.client.setScreen(new TextInputScreen(this, tx)); return true; }
                if (s instanceof Setting.Bool b) b.toggle();
                else if (s instanceof Setting.Key k) capturing = k;
                else if (s instanceof Setting.Number n) {
                    int vw = Ui.width(Ui.bold(n.display()));
                    dragging = n; dragX = ctrlX; dragW = ctrlW - vw - 10;
                    n.set(n.min + (n.max - n.min) * clamp01((float) (mx - dragX) / dragW));
                } else if (s instanceof Setting.Choice ch) ch.next(mx < ctrlX + ctrlW / 2f ? -1 : 1);
                Modules.save();
                return true;
            }
            rowY += 26;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            dragging.set(dragging.min + (dragging.max - dragging.min) * clamp01((float) (mx - dragX) / dragW));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) { dragging = null; Modules.save(); }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int content;
        if (settingsOf != null) content = rowsTop() - contentY + settingsOf.settings().size() * 26;
        else content = ((mods().size() + cols - 1) / cols) * (cardH + GAP);
        int max = Math.max(0, content - contentH);
        scrollTarget = Math.max(0, Math.min(max, scrollTarget - (float) v * 28f));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturing != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) capturing.code = -1;
            else if (keyCode != GLFW.GLFW_KEY_ESCAPE) capturing.code = keyCode;
            capturing = null;
            Modules.save();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && settingsOf != null) { settingsOf = null; scroll = scrollTarget = 0; return true; }
        // pisanie od razu trafia do wyszukiwarki
        if (!search.isFocused() && keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z && (modifiers & GLFW.GLFW_MOD_CONTROL) == 0) {
            this.setFocused(search);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
