package pl.gruzclient.module;

import net.minecraft.client.gui.DrawContext;

import static pl.gruzclient.render.GruzDraw.*;

/** Prosty element HUD: wartość + mała etykieta, np. "144 FPS". */
public abstract class TextHudModule extends HudModule {
    protected TextHudModule(String id, boolean enabledByDefault, float defX, float defY) {
        super(id, enabledByDefault, defX, defY);
    }

    protected abstract String value(boolean editing);
    protected String label() { return ""; }

    @Override
    protected int[] draw(DrawContext c, boolean editing) {
        String v = value(editing);
        String l = label();
        var tr = mc.textRenderer;
        int pad = 5;
        int vw = tr.getWidth(v);
        int lw = l.isEmpty() ? 0 : tr.getWidth(l) + 4;
        int w = vw + lw + pad * 2, h = 16;
        panel(c, w, h);
        c.drawText(tr, v, pad, 4, textColor(), !background.value);
        if (!l.isEmpty()) c.drawText(tr, l, pad + vw + 4, 4, argb(1f, MUTE), !background.value);
        return new int[]{w, h};
    }
}
