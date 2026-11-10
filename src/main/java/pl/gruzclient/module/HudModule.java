package pl.gruzclient.module;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

import static pl.gruzclient.render.GruzDraw.*;

/**
 * Moduł rysowany na ekranie gry. Pozycja jest zapisana jako ułamek szerokości/wysokości ekranu,
 * więc układ zostaje taki sam przy każdej rozdzielczości.
 */
public abstract class HudModule extends Module {
    public float x, y;          // 0..1 – lewy górny róg elementu
    public float scale = 1f;
    private final float defX, defY;
    public int lastW = 40, lastH = 12; // rozmiar z ostatniego rysowania (dla edytora)
    /** Przesunięcie w dół (px), gdy nieprzesunięty moduł nachodzi na wyższy w tej samej kolumnie (Modules.layout) */
    public int pushY;
    /** Przesunięcie w bok (px): kolumna nie mieściła się na wysokość, więc moduł trafił do następnej, bliżej środka */
    public int pushX;

    protected final Setting.Bool background;
    protected final Setting.Color color;

    protected HudModule(String id, boolean enabledByDefault, float defX, float defY) {
        super(id, Category.HUD, enabledByDefault);
        this.x = this.defX = defX;
        this.y = this.defY = defY;
        background = add(new Setting.Bool("background", "setting.background", true));
        color = add(new Setting.Color("color", "setting.color", 0));
    }

    /** Rysuje element w punkcie (0,0) i zwraca jego rozmiar: {szerokość, wysokość} */
    protected abstract int[] draw(GuiGraphics c, boolean editing);

    public void render(GuiGraphics c, int screenW, int screenH, boolean editing) {
        var m = pl.gruzclient.compat.Pose2D.of(c);
        m.push();
        int px = screenX(screenW), py = screenY(screenH);
        m.translate(px, py);
        m.scale(scale, scale);
        int[] size = draw(c, editing);
        lastW = size[0];
        lastH = size[1];
        m.pop();
    }

    /** Wspólne tło "pastylki" HUD */
    protected void panel(GuiGraphics c, int w, int h) {
        if (!background.value) return;
        c.fill(0, 0, w, h, argb(0.55f, SLAB));
        c.fill(0, 0, 1, h, argb(0.9f, NEON));
    }

    public int textColor() { return 0xFF000000 | color.rgb(); }

    public void resetPosition() { x = defX; y = defY; scale = 1f; }

    /** Gracz nie przesuwał modułu – może go ułożyć automat */
    public boolean atDefault() { return x == defX && y == defY; }
    public float defaultY() { return defY; }
    /** Pozycja na ekranie; element nie wychodzi poza krawędź (mały ekran albo duża skala GUI – np. minimapa i mikstury po prawej) */
    public int screenX(int screenW) { return Math.max(0, Math.min(Math.round(x * screenW) + (atDefault() ? pushX : 0), screenW - Math.round(lastW * scale))); }
    public int screenY(int screenH) {
        return Math.max(0, Math.min(Math.round(y * screenH) + (atDefault() ? pushY : 0), screenH - Math.round(lastH * scale)));
    }

    @Override
    public void load(JsonObject o) {
        super.load(o);
        if (o.has("x")) x = o.get("x").getAsFloat();
        if (o.has("y")) y = o.get("y").getAsFloat();
        if (o.has("scale")) scale = o.get("scale").getAsFloat();
    }

    @Override
    public JsonObject save() {
        JsonObject o = super.save();
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("scale", scale);
        return o;
    }
}
