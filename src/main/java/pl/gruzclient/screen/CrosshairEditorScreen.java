package pl.gruzclient.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.impl.CrosshairModule;

import static pl.gruzclient.render.GruzDraw.*;

/**
 * Edytor celownika piksel po pikselu. Lewy przycisk rysuje, prawy ściera (można przeciągać).
 * Kod celownika można skopiować i wysłać znajomym, a ich kod wkleić.
 */
public class CrosshairEditorScreen extends Screen {
    private static final int N = CrosshairModule.GRID;
    private final Screen parent;
    private final CrosshairModule module;
    private boolean[] grid;
    private int cell, gx, gy;
    private Boolean painting = null; // true = rysuje, false = ściera
    private boolean mirror = true;
    private String status = "";
    private long statusAt = 0;

    public CrosshairEditorScreen(Screen parent, CrosshairModule module) {
        super(Text.literal("Crosshair"));
        this.parent = parent;
        this.module = module;
        this.grid = module.grid().clone();
    }

    @Override
    protected void init() {
        cell = Math.max(8, Math.min(18, (this.height - 110) / N));
        int size = cell * N;
        gx = this.width / 2 - size / 2 - 60;
        gy = 40;
        int bx = gx + size + 20, bw = 120, bh = 20;
        int y = gy;
        addDrawableChild(new GruzButton(bx, y, bw, bh, Text.literal(Lang.t("cross.mirror") + ": " + Lang.t(mirror ? "mods.on" : "mods.off")), b -> {
            mirror = !mirror;
            b.setMessage(Text.literal(Lang.t("cross.mirror") + ": " + Lang.t(mirror ? "mods.on" : "mods.off")));
        }));
        y += bh + 6;
        addDrawableChild(new GruzButton(bx, y, bw, bh, Text.literal(Lang.t("cross.clear")), b -> grid = new boolean[N * N]));
        y += bh + 6;
        addDrawableChild(new GruzButton(bx, y, bw, bh, Text.literal(Lang.t("cross.copy")), b -> {
            this.client.keyboard.setClipboard(CrosshairModule.encode(grid));
            flash(Lang.t("cross.copied"));
        }));
        y += bh + 6;
        addDrawableChild(new GruzButton(bx, y, bw, bh, Text.literal(Lang.t("cross.paste")), b -> {
            boolean[] g = CrosshairModule.decode(this.client.keyboard.getClipboard().trim());
            if (g == null) flash(Lang.t("cross.badCode"));
            else { grid = g; flash(Lang.t("cross.pasted")); }
        }));
        int by = this.height - 30;
        addDrawableChild(new GruzButton(this.width / 2 - 126, by, 120, 20, Text.literal(Lang.t("common.cancel")), b -> this.client.setScreen(parent)));
        addDrawableChild(new GruzButton(this.width / 2 + 6, by, 120, 20, Text.literal(Lang.t("cross.save")), b -> save()).primary());
    }

    private void flash(String s) { status = s; statusAt = System.currentTimeMillis(); }

    private void save() {
        module.pixels.value = CrosshairModule.encode(grid);
        module.style.index = 3; // przełącz na styl "piksele"
        if (!module.isEnabled()) module.setEnabled(true);
        Modules.save();
        this.client.setScreen(parent);
    }

    @Override
    public void close() { this.client.setScreen(parent); }

    @Override
    public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, this.width, this.height, this.client.world != null ? 0xD0000000 : 0xFF07050C);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        this.renderBackground(c, mouseX, mouseY, delta);
        var tr = this.textRenderer;
        c.drawText(tr, Lang.t("cross.title"), gx, 14, argb(1f, TEXT), false);
        c.drawText(tr, Lang.t("cross.hint"), gx, 25, argb(1f, MUTE), false);
        int size = cell * N;
        c.fill(gx - 1, gy - 1, gx + size + 1, gy + size + 1, argb(1f, SEAM));
        int col = 0xFF000000 | module.color.rgb();
        int hx = (mouseX - gx) / cell, hy = (mouseY - gy) / cell;
        boolean hovering = mouseX >= gx && mouseY >= gy && hx < N && hy < N;
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
            int px = gx + x * cell, py = gy + y * cell;
            boolean center = x == N / 2 && y == N / 2;
            int bg = center ? 0xFF1E1530 : ((x + y) % 2 == 0 ? 0xFF0C0912 : 0xFF110C19);
            c.fill(px, py, px + cell, py + cell, bg);
            if (grid[y * N + x]) c.fill(px + 1, py + 1, px + cell - 1, py + cell - 1, col);
            if (hovering && x == hx && y == hy) c.drawBorder(px, py, cell, cell, argb(1f, NEON_HOT));
        }
        // podgląd w prawdziwej skali na tle "nieba"
        int pvx = gx + size + 20, pvy = gy + 4 * 26 + 12, pv = 64;
        c.fillGradient(pvx, pvy, pvx + 120, pvy + pv, 0xFF6FA8DC, 0xFF3D6B4A);
        c.drawText(tr, Lang.t("cross.preview"), pvx, pvy + pv + 4, argb(1f, MUTE), false);
        int cx = pvx + 60, cy = pvy + pv / 2, half = N / 2;
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
            if (grid[y * N + x]) c.fill(cx + x - half, cy + y - half, cx + x - half + 1, cy + y - half + 1, col);
        }
        if (!status.isEmpty() && System.currentTimeMillis() - statusAt < 2500) {
            c.drawText(tr, status, pvx, pvy + pv + 18, argb(1f, NEON_HOT), false);
        }
        for (var child : this.children()) if (child instanceof GruzButton b) b.render(c, mouseX, mouseY, delta);
    }

    private boolean paintAt(double mx, double my) {
        int x = (int) ((mx - gx) / cell), y = (int) ((my - gy) / cell);
        if (mx < gx || my < gy || x >= N || y >= N || painting == null) return false;
        set(x, y, painting);
        if (mirror) { set(N - 1 - x, y, painting); set(x, N - 1 - y, painting); set(N - 1 - x, N - 1 - y, painting); }
        return true;
    }

    private void set(int x, int y, boolean v) { grid[y * N + x] = v; }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int x = (int) ((mx - gx) / cell), y = (int) ((my - gy) / cell);
        if (mx >= gx && my >= gy && x < N && y < N) {
            // lewy: rysuje (albo ściera, gdy zaczynasz na zapalonym pikselu), prawy: ściera
            painting = button == 0 ? !grid[y * N + x] : Boolean.FALSE;
            return paintAt(mx, my);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (painting != null) return paintAt(mx, my);
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        painting = null;
        return super.mouseReleased(mx, my, button);
    }
}
