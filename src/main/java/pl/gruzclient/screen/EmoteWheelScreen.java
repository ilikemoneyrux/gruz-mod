package pl.gruzclient.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import pl.gruzclient.cosmetics.Emote;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.impl.CosmeticsModule;

import static pl.gruzclient.render.GruzDraw.*;

/** Koło emotek: najedź na wycinek i puść klawisz (albo kliknij). */
public class EmoteWheelScreen extends Screen {
    private final CosmeticsModule module;
    private final Emote[] emotes = Emote.values();
    private final long openedAt = System.currentTimeMillis();
    private int hovered = -1;

    public EmoteWheelScreen(CosmeticsModule module) {
        super(Text.literal("Emotki"));
        this.module = module;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, this.width, this.height, 0x66000000);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        this.renderBackground(c, mouseX, mouseY, delta);
        float r = outExpo(clamp01((System.currentTimeMillis() - openedAt) / 260f));
        int cx = this.width / 2, cy = this.height / 2;
        float radius = 70 * (0.85f + 0.15f * r);
        double ang = Math.atan2(mouseY - cy, mouseX - cx);
        double dist = Math.hypot(mouseX - cx, mouseY - cy);
        int n = emotes.length;
        hovered = dist > 18 ? (int) Math.floor(((ang + Math.PI / 2 + Math.PI * 2 + Math.PI / n) % (Math.PI * 2)) / (Math.PI * 2 / n)) : -1;

        // kropki-segmenty ułożone w okrąg
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2 + i * Math.PI * 2 / n;
            int x = cx + (int) (Math.cos(a) * radius), y = cy + (int) (Math.sin(a) * radius);
            boolean hov = i == hovered;
            int w = 64, h = 22;
            c.fill(x - w / 2, y - h / 2, x + w / 2, y + h / 2, argb(r * (hov ? 0.95f : 0.75f), hov ? 0x22143A : SLAB));
            c.drawBorder(x - w / 2, y - h / 2, w, h, argb(r, hov ? NEON : SEAM));
            String label = Lang.t("emote." + emotes[i].id);
            if (textVisible(r)) c.drawText(this.textRenderer, label, x - this.textRenderer.getWidth(label) / 2, y - 4, argb(r, hov ? NEON_HOT : TEXT), false);
        }
        // środek
        c.fill(cx - 3, cy - 3, cx + 3, cy + 3, argb(r, NEON));
        String hint = Lang.t("emote.hint");
        if (textVisible(r)) c.drawText(this.textRenderer, hint, cx - this.textRenderer.getWidth(hint) / 2, cy + (int) radius + 22, argb(r, MUTE), true);
        // linia od środka do kursora
        if (hovered >= 0) {
            int steps = (int) Math.min(dist, radius - 14) / 3;
            for (int s = 1; s < steps; s++) {
                int px = cx + (int) (Math.cos(ang) * s * 3), py = cy + (int) (Math.sin(ang) * s * 3);
                c.fill(px, py, px + 1, py + 1, argb(r * 0.6f, NEON_HOT));
            }
        }
    }

    private void choose() {
        if (hovered >= 0 && hovered < emotes.length) module.playEmote(emotes[hovered]);
        this.close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        choose();
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == module.emoteKey.code) { choose(); return true; }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // cyfry 1–8 wybierają emotkę od razu
        if (keyCode >= 49 && keyCode < 49 + emotes.length) {
            hovered = keyCode - 49;
            choose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

}
