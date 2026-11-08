//#swap MC >= 26.2 | (pl.gruzclient.mixin.InGameHudInvoker) this.minecraft.gui) => (pl.gruzclient.mixin.InGameHudInvoker) this.minecraft.gui.hud)
package pl.gruzclient.screen;

import com.mojang.blaze3d.platform.InputConstants;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.HudModule;
import pl.gruzclient.module.HudPresets;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;
import pl.gruzclient.render.Ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static pl.gruzclient.render.GruzDraw.*;

/**
 * Centrum modów Gruz (prawy Shift albo ESC → Gruz Mods).
 * Gra jest rozmyta, wszystkie elementy na ekranie są podpisane i da się je przeciągać:
 * moduły HUD Gruz, tablica wyników i pasek bossa. Przy każdym są ⚙ (ustawienia) i ⊖ (wyłącz).
 * Kółko myszy nad elementem zmienia rozmiar. Na środku logo, "Edytuj mody Gruz" i szybkie akcje.
 */
public class HudEditorScreen extends pl.gruzclient.compat.GruzScreen {
    private static final int SNAP = 4;
    /** true, gdy edytor sam rysuje tablicę wyników i pasek bossa (ponad rozmyciem) */
    public static boolean drawingVanilla = false;
    private static boolean snapOn = true;
    private static int presetIndex = 0;

    private final Screen parent;
    private final long openedAt = System.currentTimeMillis();
    private Element dragging, hovered;
    private double offX, offY;
    private boolean snapX, snapY;
    private int hubY;
    private String toast = "";
    private long toastAt = 0;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("Gruz Mods"));
        this.parent = parent;
    }

    // ---------------- elementy do przeciągania ----------------
    /** Wspólny widok na moduł HUD i element gry (tablica wyników, pasek bossa) */
    private interface Element {
        String name();
        int[] rect(int w, int h);                 // {x, y, w, h}
        void moveTo(float x, float y, int w, int h);
        void scaleBy(float d);
        Module settings();
        void hide();
        void render(GuiGraphics c, int w, int h);
    }

    private static final class HudEl implements Element {
        final HudModule m;
        HudEl(HudModule m) { this.m = m; }
        public String name() { return m.name(); }
        public int[] rect(int w, int h) {
            return new int[]{Math.round(m.x * w), Math.round(m.y * h), Math.max(8, Math.round(m.lastW * m.scale)), Math.max(8, Math.round(m.lastH * m.scale))};
        }
        public void moveTo(float x, float y, int w, int h) { m.x = x / w; m.y = y / h; }
        public void scaleBy(float d) { m.scale = Math.max(0.5f, Math.min(2.5f, Math.round((m.scale + d) * 10f) / 10f)); }
        public Module settings() { return m; }
        public void hide() { m.setEnabled(false); }
        public void render(GuiGraphics c, int w, int h) { m.render(c, w, h, true); }
    }

    private static final class ScoreboardEl implements Element {
        public String name() { return Lang.t("hud.el.scoreboard"); }
        public int[] rect(int w, int h) { return Modules.SCOREBOARD.screenRect(w, h); }
        public void moveTo(float x, float y, int w, int h) {
            int[] r = rect(w, h);
            Modules.SCOREBOARD.offX += x - r[0];
            Modules.SCOREBOARD.offY += y - r[1];
        }
        public void scaleBy(float d) { var s = Modules.SCOREBOARD.size; s.set(s.value + d); }
        public Module settings() { return Modules.SCOREBOARD; }
        public void hide() { Modules.SCOREBOARD.hide.value = true; }
        public void render(GuiGraphics c, int w, int h) {
            if (Modules.SCOREBOARD.live()) return; // prawdziwą tablicę gra rysuje sama
            int[] r = rect(w, h);
            c.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], argb(0.3f, 0x000000));
            String[] sample = {"GRUZ.PL", "", "Kille: 12", "Ranga: VIP", "", "gruz.pl"};
            for (int i = 0; i < sample.length; i++) {
                int ty = r[1] + 3 + Math.round(i * 9 * Modules.SCOREBOARD.size.value);
                if (ty + 8 > r[1] + r[3]) break;
                c.drawString(Ui.tr(), sample[i], r[0] + 4, ty, i == 0 ? 0xFFFFD35C : 0xFFFFFFFF, false);
            }
        }
    }

    private static final class BossEl implements Element {
        public String name() { return Lang.t("hud.el.bossbar"); }
        public int[] rect(int w, int h) { return Modules.HUD_TWEAKS.bossRect(w); }
        public void moveTo(float x, float y, int w, int h) {
            int[] r = rect(w, h);
            Modules.HUD_TWEAKS.bossX += x - r[0];
            Modules.HUD_TWEAKS.bossY += y - r[1];
        }
        public void scaleBy(float d) { var s = Modules.HUD_TWEAKS.bossSize; s.set(s.value + d); }
        public Module settings() { return Modules.HUD_TWEAKS; }
        public void hide() { Modules.HUD_TWEAKS.hideBoss.value = true; }
        public void render(GuiGraphics c, int w, int h) {
            if (Modules.HUD_TWEAKS.bossCount > 0) return;
            int[] r = rect(w, h);
            Component t = Ui.bold(Lang.t("hud.el.bossSample"));
            c.drawString(Ui.tr(), t, r[0] + (r[2] - Ui.width(t)) / 2, r[1], 0xFFFFFFFF, true);
            c.fill(r[0], r[1] + r[3] - 5, r[0] + r[2], r[1] + r[3], argb(0.6f, 0x2A0F33));
            c.fill(r[0], r[1] + r[3] - 5, r[0] + Math.round(r[2] * 0.7f), r[1] + r[3], argb(1f, 0xE040C8));
        }
    }

    private List<Element> elements() {
        List<Element> list = new ArrayList<>();
        for (HudModule m : Modules.hud()) if (m.isEnabled()) list.add(new HudEl(m));
        if (Modules.SCOREBOARD.isEnabled() && !Modules.SCOREBOARD.hide.value) list.add(new ScoreboardEl());
        if (!Modules.HUD_TWEAKS.hideBossbar()) list.add(new BossEl());
        return list;
    }

    // ---------------- układ ----------------
    @Override
    protected void init() {
        int cx = this.width / 2;
        hubY = this.height / 2 - 30;
        int bw = 150;
        this.addRenderableWidget(new UiButton(cx - bw / 2, hubY + 24, bw, 22, Ui.Icon.GRID, Lang.t("hud.editMods"),
                b -> this.minecraft.setScreen(new GruzModsScreen(this))).kind(UiButton.Kind.ACCENT).big());
        Object[][] row = {
                {Ui.Icon.SHIRT, "hud.act.cosmetics"}, {Ui.Icon.SMILE, "hud.act.emotes"}, {Ui.Icon.LAYOUT, "hud.act.preset"},
                {Ui.Icon.MAGNET, "hud.act.snap"}, {Ui.Icon.MOVE, "hud.act.reset"},
        };
        int s = 22, gap = 6, total = row.length * s + (row.length - 1) * gap;
        for (int i = 0; i < row.length; i++) {
            final String key = (String) row[i][1];
            UiButton b = new UiButton(cx - total / 2 + i * (s + gap), hubY + 52, s, s, (Ui.Icon) row[i][0], null, x -> action(key)).kind(UiButton.Kind.ICON);
            b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(Lang.t(key))));
            this.addRenderableWidget(b);
        }
        this.addRenderableWidget(new UiButton(this.width - 10 - 22, 10, 22, 22, Ui.Icon.CLOSE, null, b -> onClose()).kind(UiButton.Kind.ICON));
    }

    private void action(String key) {
        switch (key) {
            case "hud.act.cosmetics" -> this.minecraft.setScreen(new GruzModsScreen(this, Module.Category.COSMETICS));
            case "hud.act.emotes" -> { if (this.minecraft.player != null) this.minecraft.setScreen(new EmoteWheelScreen(Modules.COSMETICS)); }
            case "hud.act.preset" -> {
                String name = HudPresets.NAMES[presetIndex++ % HudPresets.NAMES.length];
                HudPresets.apply(name);
                say(Lang.t("hud.presetApplied").replace("{name}", Lang.t("hud.preset." + name)));
            }
            case "hud.act.snap" -> { snapOn = !snapOn; say(Lang.t(snapOn ? "hud.snapOn" : "hud.snapOff")); }
            case "hud.act.reset" -> {
                for (HudModule m : Modules.hud()) m.resetPosition();
                Modules.SCOREBOARD.offX = Modules.SCOREBOARD.offY = 0;
                Modules.HUD_TWEAKS.bossX = Modules.HUD_TWEAKS.bossY = 0;
                Modules.save();
                say(Lang.t("hud.resetDone"));
            }
            default -> { }
        }
    }

    private void say(String s) { toast = s; toastAt = System.currentTimeMillis(); }

    @Override
    public void onClose() {
        Modules.save();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics c, int mouseX, int mouseY, float delta) {
        if (this.minecraft.level != null) {
            //#if MC >= 1.21.6
            // od 1.21.6 rozmycie robi sam Minecraft (i tylko raz na klatkę)
            //#elif MC >= 1.21.2
            this.renderBlurredBackground();
            //#elif MC >= 1.20.5
            this.renderBlurredBackground(delta);
            //#endif
            c.fill(0, 0, this.width, this.height, 0x40000000);
        } else {
            c.fill(0, 0, this.width, this.height, 0xFF07050C);
        }
    }

    @Override
    public void render(GuiGraphics c, int mouseX, int mouseY, float delta) {
        this.renderBackground(c, mouseX, mouseY, delta);
        int w = this.width, h = this.height;
        float r0 = outExpo(clamp01((System.currentTimeMillis() - openedAt) / 500f));

        // siatka przy przeciąganiu z przyciąganiem
        if (dragging != null && snapOn) {
            for (int gx = 0; gx < w; gx += 20) c.fill(gx, 0, gx + 1, h, 0x0CFFFFFF);
            for (int gy = 0; gy < h; gy += 20) c.fill(0, gy, w, gy + 1, 0x0CFFFFFF);
            int cx = w / 2, cy = h / 2;
            if (snapX) c.fill(cx, 0, cx + 1, h, argb(0.8f, NEON));
            if (snapY) c.fill(0, cy, w, cy + 1, argb(0.8f, NEON));
        }

        // tablica wyników i pasek bossa rysujemy jeszcze raz, ostro, ponad rozmyciem
        if (this.minecraft.level != null && this.minecraft.player != null) {
            drawingVanilla = true;
            try {
                //#if MC >= 1.21.2
                ((pl.gruzclient.mixin.InGameHudInvoker) this.minecraft.gui).gruz$renderScoreboard(c, this.minecraft.getDeltaTracker());
                //#elif MC >= 1.21
                ((pl.gruzclient.mixin.InGameHudInvoker) this.minecraft.gui).gruz$renderScoreboard(c, this.minecraft.getTimer());
                //#endif
                this.minecraft.gui.getBossOverlay().render(c);
            } catch (Exception ignored) {
                // brak tablicy itp.
            } finally {
                drawingVanilla = false;
            }
        }

        List<Element> els = elements();
        hovered = dragging != null ? dragging : at(els, mouseX, mouseY);
        for (Element e : els) {
            e.render(c, w, h);
            int[] r = e.rect(w, h);
            boolean hot = e == hovered;
            // ramka elementu
            Ui.roundBorder(c, r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4, 2, argb(r0 * (hot ? 0.95f : 0.28f), hot ? NEON : 0xFFFFFF));
            if (hot) c.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, argb(0.12f, NEON));
            // podpis pod elementem
            Component label = Ui.text(e.name());
            int ly = r[1] + r[3] + 4 > h - 12 ? r[1] - 13 : r[1] + r[3] + 4;
            int lx = Math.max(2, Math.min(w - Ui.width(label) - 2, r[0] + (r[2] - Ui.width(label)) / 2));
            // podpis, który wchodziłby na inny element, pokazujemy tylko po najechaniu
            boolean clash = false;
            for (Element o : els) {
                if (o == e) continue;
                int[] q = o.rect(w, h);
                if (lx < q[0] + q[2] && lx + Ui.width(label) > q[0] && ly < q[1] + q[3] && ly + 9 > q[1]) { clash = true; break; }
            }
            if (!clash || hot) c.drawString(Ui.tr(), label, lx, ly, argb(r0 * (hot ? 1f : 0.8f), hot ? NEON_HOT : 0xFFFFFF), true);
            // ⚙ i ⊖ w prawym górnym rogu (przy najechaniu)
            if (hot && dragging == null) {
                int bx = r[0] + r[2] - 4, by = r[1] - 6;
                smallButton(c, bx - 12, by, Ui.Icon.GEAR, 0x4D7CFF, Ui.inside(mouseX, mouseY, bx - 12, by, 10, 10));
                smallButton(c, bx, by, Ui.Icon.MINUS, 0xFF4D6D, Ui.inside(mouseX, mouseY, bx, by, 10, 10));
            }
        }

        // centrum: logo + przyciski
        float pulse = 0.5f + 0.5f * (float) Math.sin(seconds() * 1.5f);
        sprite(c, GLOW, 256, w / 2f - 40, hubY - 34, 80, 80, r0 * (0.25f + 0.1f * pulse), true, 0f);
        sprite(c, LOGO, 256, w / 2f - 14, hubY - 8, 28, 28, r0, false, (float) Math.sin(seconds() * 0.6f) * 0.04f);
        for (var child : this.children()) if (child instanceof UiButton b) b.render(c, mouseX, mouseY, delta);

        // podpowiedź na dole
        Component hint = Ui.text(Lang.t("hud.hint"));
        int hw = Ui.width(hint) + 16;
        Ui.round(c, (w - hw) / 2, h - 24, hw, 16, 4, argb(r0 * 0.7f, 0x0A0810));
        Ui.drawCentered(c, hint, w / 2, h - 20, argb(r0, 0xD6CCE6));

        // krótki komunikat
        long since = System.currentTimeMillis() - toastAt;
        if (since < 2200 && !toast.isEmpty()) {
            float ta = clamp01(Math.min(since / 150f, (2200 - since) / 300f));
            Component tt = Ui.bold(toast);
            int tw = Ui.width(tt) + 20;
            Ui.round(c, (w - tw) / 2, hubY + 82, tw, 18, 4, argb(ta * 0.9f, 0x1A0F2A));
            Ui.roundBorder(c, (w - tw) / 2, hubY + 82, tw, 18, 4, argb(ta, NEON));
            Ui.drawCentered(c, tt, w / 2, hubY + 87, argb(ta, 0xFFFFFF));
        }
    }

    private void smallButton(GuiGraphics c, int x, int y, Ui.Icon icon, int color, boolean hot) {
        Ui.round(c, x, y, 10, 10, 5, argb(1f, hot ? GruzDraw2.lighten(color) : color));
        Ui.icon(c, icon, x + 2, y + 2, 6, 0xFFFFFFFF);
    }

    private Element at(List<Element> els, double mx, double my) {
        for (int i = els.size() - 1; i >= 0; i--) {
            int[] r = els.get(i).rect(this.width, this.height);
            if (Ui.inside(mx, my, r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4)) return els.get(i);
        }
        return null;
    }

    // ---------------- mysz ----------------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // przyciski ⚙ / ⊖ mają pierwszeństwo
        if (hovered != null && dragging == null) {
            int[] r = hovered.rect(this.width, this.height);
            int bx = r[0] + r[2] - 4, by = r[1] - 6;
            if (Ui.inside(mx, my, bx - 12, by, 10, 10)) { this.minecraft.setScreen(new GruzModsScreen(this, hovered.settings())); return true; }
            if (Ui.inside(mx, my, bx, by, 10, 10)) { hovered.hide(); say(Lang.t("hud.hidden").replace("{name}", hovered.name())); Modules.save(); return true; }
        }
        if (super.mouseClicked(mx, my, button)) return true;
        Element e = at(elements(), mx, my);
        if (e == null) return false;
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) { this.minecraft.setScreen(new GruzModsScreen(this, e.settings())); return true; }
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            int[] r = e.rect(this.width, this.height);
            dragging = e;
            offX = mx - r[0];
            offY = my - r[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging == null) return super.mouseDragged(mx, my, button, dx, dy);
        int[] r = dragging.rect(this.width, this.height);
        int w = r[2], h = r[3];
        double nx = mx - offX, ny = my - offY;
        snapX = snapY = false;
        if (snapOn && !pl.gruzclient.compat.GruzScreen.alt()) {
            double centerX = this.width / 2.0 - w / 2.0, centerY = this.height / 2.0 - h / 2.0;
            if (Math.abs(nx - centerX) < SNAP) { nx = centerX; snapX = true; }
            if (Math.abs(ny - centerY) < SNAP) { ny = centerY; snapY = true; }
            if (Math.abs(nx - 2) < SNAP) nx = 2;
            if (Math.abs(ny - 2) < SNAP) ny = 2;
            if (Math.abs(nx + w - (this.width - 2)) < SNAP) nx = this.width - 2 - w;
            if (Math.abs(ny + h - (this.height - 2)) < SNAP) ny = this.height - 2 - h;
            // przyciąganie do krawędzi innych elementów
            for (Element o : elements()) {
                if (o == dragging || (o instanceof HudEl a && dragging instanceof HudEl b && a.m == b.m)) continue;
                int[] q = o.rect(this.width, this.height);
                if (Math.abs(nx - q[0]) < SNAP) nx = q[0];
                if (Math.abs(nx - (q[0] + q[2] + 2)) < SNAP) nx = q[0] + q[2] + 2;
                if (Math.abs(ny - (q[1] + q[3] + 2)) < SNAP) ny = q[1] + q[3] + 2;
                if (Math.abs(ny - q[1]) < SNAP) ny = q[1];
            }
        }
        nx = Math.max(0, Math.min(this.width - w, nx));
        ny = Math.max(0, Math.min(this.height - h, ny));
        dragging.moveTo((float) nx, (float) ny, this.width, this.height);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) { dragging = null; snapX = snapY = false; Modules.save(); }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        Element e = at(elements(), mx, my);
        if (e == null) return false;
        e.scaleBy(v > 0 ? 0.1f : -0.1f);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == Modules.HUB_KEY && this.minecraft.level != null) { onClose(); return true; }
        // strzałki przesuwają element pod kursorem o 1 px
        if (hovered != null && keyCode >= InputConstants.KEY_RIGHT && keyCode <= InputConstants.KEY_UP) {
            int[] r = hovered.rect(this.width, this.height);
            int dx = keyCode == InputConstants.KEY_RIGHT ? 1 : keyCode == InputConstants.KEY_LEFT ? -1 : 0;
            int dy = keyCode == InputConstants.KEY_DOWN ? 1 : keyCode == InputConstants.KEY_UP ? -1 : 0;
            hovered.moveTo(r[0] + dx, r[1] + dy, this.width, this.height);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Drobne pomocnicze kolory */
    private static final class GruzDraw2 {
        static int lighten(int c) { return mix(c, 0xFFFFFF, 0.25f); }
    }
}
