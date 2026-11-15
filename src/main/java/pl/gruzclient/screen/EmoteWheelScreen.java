package pl.gruzclient.screen;

import pl.gruzclient.GruzClient;
import pl.gruzclient.compat.Compat;
import pl.gruzclient.cosmetics.Emote;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.impl.CosmeticsModule;
import pl.gruzclient.render.Ui;

import static pl.gruzclient.render.GruzDraw.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Koło emotek: ciemny pierścień z 8 miejscami na stronę, w każdym ludzik w pozie emotki (najechana gra w pętli).
 * Najedź i puść klawisz albo kliknij. Prawy klik = ulubiona, kółko myszy / strzałki = strona, cyfry 1–8 = miejsce na stronie.
 */
public class EmoteWheelScreen extends pl.gruzclient.compat.GruzScreen {
    private static final ResourceLocation RING = Compat.id(GruzClient.MOD_ID, "textures/gui/emote_ring.png");
    private static final ResourceLocation WEDGE = Compat.id(GruzClient.MOD_ID, "textures/gui/emote_wedge.png");
    private static final int SLOTS = 8;
    private static final float INNER = 0.40f; // jak w emote_ring.png

    private final CosmeticsModule module;
    private Emote[] emotes;
    private int page;
    private Emote previewed;
    private long hoverSince = 0;
    private boolean chosen;
    private final long openedAt = System.currentTimeMillis();
    private int hovered = -1; // miejsce na stronie
    private float hoverAnim;
    private int lastHover = -1;
    private long lastFrame = System.nanoTime();
    private UiButton eye;

    public EmoteWheelScreen(CosmeticsModule module) {
        super(Component.literal("Emotki"));
        this.module = module;
        order();
    }

    private java.util.List<String> favs() {
        java.util.List<String> l = new java.util.ArrayList<>();
        for (String s : module.favEmotes.value.split(",")) if (!s.isBlank()) l.add(s.trim());
        return l;
    }

    /** Ulubione najpierw (w kolejności dodania), potem reszta */
    private void order() {
        java.util.List<String> f = favs();
        java.util.List<Emote> l = new java.util.ArrayList<>();
        for (String id : f) for (Emote e : Emote.values()) if (e.id.equals(id)) l.add(e);
        for (Emote e : Emote.values()) if (!l.contains(e)) l.add(e);
        emotes = l.toArray(new Emote[0]);
    }

    private void toggleFav(Emote e) {
        java.util.List<String> f = favs();
        if (!f.remove(e.id)) f.add(e.id);
        module.favEmotes.value = String.join(",", f);
        Modules.save();
        order();
    }

    private int pages() { return (emotes.length + SLOTS - 1) / SLOTS; }

    private Emote at(int slot) {
        int i = page * SLOTS + slot;
        return slot >= 0 && slot < SLOTS && i < emotes.length ? emotes[i] : null;
    }

    private void turn(int d) {
        int p = Math.floorMod(page + d, pages());
        if (p != page) { page = p; hovered = -1; }
    }

    // ---------------- układ ----------------
    private int cx() { return this.width / 2; }
    private int cy() { return this.height / 2 - 6; }
    private float radius() { return Math.max(60, Math.min(this.height * 0.34f, 120)); }
    private int pillY() { return (int) (cy() - radius()) - 26; }
    private boolean onPrev(double mx, double my) { return my >= pillY() && my < pillY() + 18 && mx >= cx() - 50 && mx < cx() - 32; }
    private boolean onNext(double mx, double my) { return my >= pillY() && my < pillY() + 18 && mx >= cx() + 32 && mx < cx() + 50; }

    @Override
    protected void init() {
        int y = (int) (cy() + radius()) + 12, w = 130;
        eye = new UiButton(cx() - w / 2 - 26, y, 20, 20, Ui.Icon.EYE, null, b -> {
            module.emotePreview.value = !module.emotePreview.value;
            Modules.save();
            if (!module.emotePreview.value && previewed != null) { module.stopPreview(); previewed = null; }
            eyeKind();
        });
        eyeKind();
        this.addRenderableWidget(eye);
        this.addRenderableWidget(new UiButton(cx() - w / 2 + 2, y, w, 20, Ui.Icon.GEAR, Lang.t("emote.settings"),
                b -> this.minecraft.setScreen(new GruzModsScreen(null, module))));
    }

    private void eyeKind() { eye.kind(module.emotePreview.value ? UiButton.Kind.ACCENT : UiButton.Kind.ICON); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, this.width, this.height, 0x66000000);
    }

    private boolean overUi(int mx, int my) {
        for (var ch : this.children()) if (ch.isMouseOver(mx, my)) return true;
        return onPrev(mx, my) || onNext(mx, my);
    }

    @Override
    public void render(GuiGraphics c, int mouseX, int mouseY, float delta) {
        this.renderBackground(c, mouseX, mouseY, delta);
        float r = outExpo(clamp01((System.currentTimeMillis() - openedAt) / 260f));
        int cx = cx(), cy = cy();
        float R = radius() * (0.9f + 0.1f * r), in = R * INNER;
        double ang = Math.atan2(mouseY - cy, mouseX - cx);
        double dist = Math.hypot(mouseX - cx, mouseY - cy);
        int hv = -1;
        if (dist > in * 0.5 && dist < R * 1.3 && !overUi(mouseX, mouseY)) {
            hv = (int) Math.floor((((ang + Math.PI / 2) / (Math.PI * 2 / SLOTS)) + 0.5 + SLOTS) % SLOTS);
            if (at(hv) == null) hv = -1;
        }
        if (hv != hovered) { hovered = hv; hoverSince = System.currentTimeMillis(); }
        Emote cur = at(hovered);
        // podgląd: po chwili najechania emotka gra na Twojej postaci (tylko u Ciebie)
        if (module.emotePreview.value && cur != null && cur != previewed && System.currentTimeMillis() - hoverSince > 700 && !cur.duo) {
            previewed = cur;
            module.previewEmote(cur);
        }
        if (hovered >= 0) lastHover = hovered;
        long now = System.nanoTime();
        hoverAnim = damp(hoverAnim, hovered >= 0 ? 1f : 0f, 18f, Math.min(0.1f, (now - lastFrame) / 1e9f));
        lastFrame = now;

        // pierścień i podświetlony wycinek
        sprite(c, RING, 256, cx - R, cy - R, R * 2, R * 2, r * 0.94f, false, 0, SLAB);
        if (lastHover >= 0) sprite(c, WEDGE, 256, cx - R, cy - R, R * 2, R * 2, r * 0.30f * hoverAnim, false, (float) (lastHover * Math.PI * 2 / SLOTS), NEON);

        java.util.List<String> fav = favs();
        float mid = (R + in) / 2, s = (R - in) * 0.68f / 32f;
        for (int i = 0; i < SLOTS; i++) {
            Emote e = at(i);
            if (e == null) continue;
            double a = -Math.PI / 2 + i * Math.PI * 2 / SLOTS;
            float x = cx + (float) Math.cos(a) * mid, y = cy + (float) Math.sin(a) * mid;
            boolean hov = i == hovered;
            EmoteDoll.draw(c, e, x, y, s * (hov ? 1.08f : 1f), hov, hov ? NEON_HOT : 0x8C8499, r);
            // numer (klawisz) przy zewnętrznej krawędzi, przed ludzikiem
            double na = a - Math.PI / SLOTS * 0.62;
            String num = (i + 1) + (fav.contains(e.id) ? "★" : "") + (e.duo ? "♥" : "");
            int nx = cx + (int) (Math.cos(na) * R * 0.86), ny = cy + (int) (Math.sin(na) * R * 0.86);
            if (textVisible(r)) Ui.drawCentered(c, Ui.text(num), nx, ny - 4, argb(r * (hov ? 1f : 0.55f), hov ? NEON_HOT : MUTE));
        }

        // środek: logo, a przy najechaniu nazwa emotki
        float ls = in * 1.05f;
        sprite(c, LOGO, 256, cx - ls / 2, cy - ls / 2, ls, ls, r * (1f - 0.85f * hoverAnim), false, 0);
        if (cur != null && textVisible(r)) {
            Component name = Ui.fit(Lang.t("emote." + cur.id), (int) (in * 1.7f), true);
            Ui.drawCentered(c, name, cx, cy - 4, argb(r * hoverAnim, TEXT));
        }

        // strona u góry
        int py = pillY(), pw = 100;
        c.fill(cx - pw / 2, py, cx + pw / 2, py + 18, argb(r * 0.94f, SLAB));
        c.renderOutline(cx - pw / 2, py, pw, 18, argb(r, SEAM));
        if (textVisible(r)) {
            Ui.drawCentered(c, Ui.bold(Lang.t("emote.page") + " " + (page + 1) + "/" + pages()), cx, py + 5, argb(r, TEXT));
            boolean more = pages() > 1;
            Ui.drawCentered(c, Ui.text("‹"), cx - 41, py + 5, argb(r * (more ? 1f : 0.3f), onPrev(mouseX, mouseY) && more ? NEON_HOT : MUTE));
            Ui.drawCentered(c, Ui.text("›"), cx + 41, py + 5, argb(r * (more ? 1f : 0.3f), onNext(mouseX, mouseY) && more ? NEON_HOT : MUTE));
            String hint = Lang.t("emote.hint");
            Ui.drawCentered(c, Ui.text(hint), cx, this.height - 14, argb(r * 0.8f, MUTE));
        }
        for (var ch : this.children()) if (ch instanceof UiButton b) b.render(c, mouseX, mouseY, delta);
    }

    private void choose() {
        Emote e = at(hovered);
        if (e != null) { chosen = true; module.playEmote(e); }
        this.onClose();
    }

    @Override
    public void removed() {
        if (!chosen && previewed != null) module.stopPreview(); // zamknięte bez wyboru: podgląd znika
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (onPrev(mouseX, mouseY)) { turn(-1); return true; }
        if (onNext(mouseX, mouseY)) { turn(1); return true; }
        if (button == 1) { Emote e = at(hovered); if (e != null) toggleFav(e); return true; }
        choose(); // klik obok koła zamyka
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (v != 0) turn(v > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // puszczony klawisz nad emotką = wybór; puszczony w środku = koło zostaje (strony, przyciski), Esc zamyka
        if (keyCode == module.emoteKey.code) { if (at(hovered) != null) choose(); return true; }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // cyfry 1–8 wybierają miejsce na bieżącej stronie
        int k = keyCode - com.mojang.blaze3d.platform.InputConstants.KEY_1;
        if (k >= 0 && k < SLOTS && at(k) != null) {
            hovered = k;
            choose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
