package pl.gruzclient.module.impl;

import net.minecraft.client.gui.GuiGraphics;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

/** Pasek bossa i duże napisy (title): rozmiar, położenie, ukrywanie. */
public class HudTweaksModule extends Module {
    public final Setting.Bool hideBoss = add(new Setting.Bool("hideBoss", "setting.hideBossbar", false));
    public final Setting.Number bossSize = add(new Setting.Number("bossSize", "setting.bossSize", 1f, 0.5f, 1.5f, 0.05f, "x"));
    public final Setting.Bool hideTitle = add(new Setting.Bool("hideTitle", "setting.hideTitle", false));
    public final Setting.Number titleSize = add(new Setting.Number("titleSize", "setting.titleSize", 1f, 0.3f, 1.5f, 0.05f, "x"));
    public final Setting.Number titleY = add(new Setting.Number("titleY", "setting.titleY", 0f, -120f, 120f, 5f, " px"));

    private boolean bossPushed, titlePushed;
    /** przesunięcie paska bossa (przeciąganie w edytorze HUD) */
    public float bossX = 0, bossY = 0;
    public int bossCount = 0;

    public HudTweaksModule() { super("hudTweaks", Category.VISUAL, true); }

    public boolean hideBossbar() { return isEnabled() && hideBoss.value; }
    public boolean hideTitles() { return isEnabled() && hideTitle.value; }

    public void bossBegin(GuiGraphics c) {
        bossPushed = isEnabled() || pl.gruzclient.module.Modules.MINIMAP.compassShown();
        if (!bossPushed) return;
        var m = pl.gruzclient.compat.Pose2D.of(c);
        m.push();
        float cx = c.guiWidth() / 2f;
        float top = bossY + (pl.gruzclient.module.Modules.MINIMAP.compassShown() ? 16 : 0);
        m.translate(cx + bossX, top);
        float k = isEnabled() ? bossSize.value : 1f;
        m.scale(k, k);
        m.translate(-cx, 0);
    }

    /** Prostokąt paska bossa na ekranie {x, y, w, h} (dla edytora HUD) */
    public int[] bossRect(int w) {
        float k = isEnabled() ? bossSize.value : 1f;
        float top = bossY + (pl.gruzclient.module.Modules.MINIMAP.compassShown() ? 16 : 0);
        int n = Math.max(1, bossCount);
        int x = Math.round(w / 2f + bossX - 91 * k), y = Math.round(top + 2 * k);
        return new int[]{x, y, Math.round(182 * k), Math.round((19 * n - 2) * k)};
    }

    @Override
    public void load(com.google.gson.JsonObject o) {
        super.load(o);
        if (o.has("bossX")) bossX = o.get("bossX").getAsFloat();
        if (o.has("bossYoff")) bossY = o.get("bossYoff").getAsFloat();
    }

    @Override
    public com.google.gson.JsonObject save() {
        var o = super.save();
        o.addProperty("bossX", bossX);
        o.addProperty("bossYoff", bossY);
        return o;
    }

    public void bossEnd(GuiGraphics c) {
        if (bossPushed) pl.gruzclient.compat.Pose2D.of(c).pop();
        bossPushed = false;
    }

    public void titleBegin(GuiGraphics c) {
        titlePushed = isEnabled();
        if (!titlePushed) return;
        var m = pl.gruzclient.compat.Pose2D.of(c);
        m.push();
        float cx = c.guiWidth() / 2f, cy = c.guiHeight() / 2f;
        m.translate(cx, cy + titleY.value);
        m.scale(titleSize.value, titleSize.value);
        m.translate(-cx, -cy);
    }

    public void titleEnd(GuiGraphics c) {
        if (titlePushed) pl.gruzclient.compat.Pose2D.of(c).pop();
        titlePushed = false;
    }
}
