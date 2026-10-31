package pl.gruzclient.screen;

import pl.gruzclient.compat.Compat;
import pl.gruzclient.GruzClient;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.map.MapCache;
import pl.gruzclient.map.Waypoints;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static pl.gruzclient.render.GruzDraw.*;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * Pełna mapa odkrytego świata (klawisz M).
 * Przeciąganie = przesuwanie, kółko = przybliżenie, PPM = nowy waypoint, klik w punkt na liście = usuń.
 */
public class WorldMapScreen extends Screen {
    private static final ResourceLocation ID = Compat.id(GruzClient.MOD_ID, "worldmap");
    private static final int SIDE = 130;
    private final Screen parent;
    private DynamicTexture texture;
    private int texW, texH;
    private double cx, cz;          // środek widoku (bloki)
    private float zoom = 1f;        // pikseli GUI na blok
    private boolean dirty = true, dragging;
    private double lastMx, lastMy;

    public WorldMapScreen(Screen parent) {
        super(Component.literal("Map"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (minecraft.player != null && !dragging && texture == null) {
            cx = minecraft.player.getX();
            cz = minecraft.player.getZ();
        }
        int mapW = Math.max(16, width - SIDE), mapH = height;
        if (texture == null || texW != mapW || texH != mapH) {
            if (texture != null) texture.close();
            texW = mapW; texH = mapH;
            texture = Compat.texture("gruz_worldmap", texW, texH);
            minecraft.getTextureManager().register(ID, texture);
        }
        dirty = true;
        addRenderableWidget(new GruzButton(width - SIDE + 8, height - 28, SIDE - 16, 20, Component.literal(Lang.t("map.center")), b -> {
            if (minecraft.player != null) { cx = minecraft.player.getX(); cz = minecraft.player.getZ(); dirty = true; }
        }));
    }

    @Override
    public void removed() {
        if (texture != null) {
            minecraft.getTextureManager().release(ID);
            texture = null;
        }
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == pl.gruzclient.module.Modules.MINIMAP.mapKey.code) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private double blockX(double mx) { return cx + (mx - texW / 2.0) / zoom; }
    private double blockZ(double my) { return cz + (my - texH / 2.0) / zoom; }

    private void rebuild() {
        NativeImage img = texture.getPixels();
        if (img == null) return;
        for (int y = 0; y < texH; y++) {
            int bz = (int) Math.floor(blockZ(y + 0.5));
            for (int x = 0; x < texW; x++) {
                int bx = (int) Math.floor(blockX(x + 0.5));
                int col = MapCache.colorAt(bx, bz);
                if (col == 0) {
                    // nieodkryte: delikatna kratka chunków
                    boolean grid = ((bx >> 4) + (bz >> 4) & 1) == 0;
                    col = grid ? 0xFF0B0911 : 0xFF0E0B16;
                }
                int r = (col >> 16) & 255, g = (col >> 8) & 255, b = col & 255;
                Compat.setPixelABGR(img, x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        }
        texture.upload();
        dirty = false;
    }

    @Override
    public void renderBackground(GuiGraphics c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, width, height, 0xFF07050C);
    }

    private long lastAuto = 0;

    @Override
    public void render(GuiGraphics c, int mouseX, int mouseY, float delta) {
        renderBackground(c, mouseX, mouseY, delta);
        long now = System.currentTimeMillis();
        if (now - lastAuto > 1000) { dirty = true; lastAuto = now; } // dociągaj świeżo odkryte chunki
        if (dirty && texture != null) rebuild();
        Compat.blitWhole(c, ID, 0, 0, texW, texH, -1);

        // waypointy
        var tr = font;
        for (Waypoints.Point p : Waypoints.current()) {
            int sx = (int) Math.round((p.x + 0.5 - cx) * zoom + texW / 2.0), sy = (int) Math.round((p.z + 0.5 - cz) * zoom + texH / 2.0);
            if (sx < -20 || sy < -20 || sx > texW + 20 || sy > texH + 20) continue;
            c.fill(sx - 3, sy - 3, sx + 3, sy + 3, 0xFF000000);
            c.fill(sx - 2, sy - 2, sx + 2, sy + 2, 0xFF000000 | p.color);
            c.drawString(tr, p.name, sx - tr.width(p.name) / 2, sy - 13, 0xFF000000 | p.color, true);
        }
        // gracz
        if (minecraft.player != null) {
            float px = (float) ((minecraft.player.getX() - cx) * zoom + texW / 2.0), pz = (float) ((minecraft.player.getZ() - cz) * zoom + texH / 2.0);
            var m = pl.gruzclient.compat.Pose2D.of(c);
            m.push();
            m.translate(px, pz);
            m.rotateDeg(minecraft.player.getYRot() + 180f);
            c.fill(-1, -5, 1, 4, 0xFFFFFFFF);
            c.fill(-3, -1, 3, 1, argb(1f, NEON_HOT));
            m.pop();
        }

        // pasek boczny
        int sx = width - SIDE;
        c.fill(sx, 0, width, height, argb(0.92f, SLAB));
        c.fill(sx, 0, sx + 1, height, argb(0.9f, NEON));
        c.drawString(tr, Lang.t("map.title"), sx + 8, 8, argb(1f, NEON_HOT), false);
        int bx = (int) Math.floor(blockX(mouseX)), bz = (int) Math.floor(blockZ(mouseY));
        if (mouseX < texW) c.drawString(tr, "X " + bx + "  Z " + bz, sx + 8, 22, argb(1f, MUTE), false);
        c.drawString(tr, Lang.t("map.waypoints"), sx + 8, 40, argb(1f, TEXT), false);
        List<Waypoints.Point> pts = Waypoints.current();
        int y = 54;
        for (Waypoints.Point p : pts) {
            if (y > height - 50) break;
            boolean hover = mouseX >= sx + 4 && mouseX < width - 4 && mouseY >= y - 2 && mouseY < y + 20;
            if (hover) c.fill(sx + 4, y - 2, width - 4, y + 20, argb(0.25f, NEON));
            c.fill(sx + 8, y + 2, sx + 13, y + 7, 0xFF000000 | p.color);
            c.drawString(tr, tr.plainSubstrByWidth(p.name, SIDE - 40), sx + 18, y, argb(1f, TEXT), false);
            String sub = p.x + ", " + p.y + ", " + p.z;
            if (minecraft.player != null) sub += "  ·  " + Math.round(Math.sqrt(minecraft.player.distanceToSqr(p.x, p.y, p.z))) + " m";
            c.drawString(tr, tr.plainSubstrByWidth(sub, SIDE - 26), sx + 18, y + 10, argb(1f, MUTE), false);
            if (hover) c.drawString(tr, "✕", width - 14, y + 4, 0xFFFF5C8A, false);
            y += 24;
        }
        if (pts.isEmpty()) c.drawString(tr, Lang.t("map.empty"), sx + 8, 56, argb(1f, MUTE), false);
        c.drawString(tr, Lang.t("map.help"), 6, height - 12, argb(0.8f, MUTE), true);
        for (var child : this.children()) if (child instanceof net.minecraft.client.gui.components.Renderable d) d.render(c, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int sx = width - SIDE;
        if (mx >= sx) {
            int y = 54;
            for (Waypoints.Point p : List.copyOf(Waypoints.current())) {
                if (my >= y - 2 && my < y + 20) {
                    if (button == 0) {
                        Waypoints.remove(p);
                    } else {
                        cx = p.x; cz = p.z; dirty = true;
                    }
                    return true;
                }
                y += 24;
            }
            return false;
        }
        if (button == 1) {
            int bx = (int) Math.floor(blockX(mx)), bz = (int) Math.floor(blockZ(my));
            int by = minecraft.level != null ? minecraft.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, bx, bz) : 64;
            Waypoints.add(Lang.t("map.point") + " " + (Waypoints.current().size() + 1), bx, by, bz, false);
            return true;
        }
        if (button == 0) { dragging = true; lastMx = mx; lastMy = my; }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            cx -= (mx - lastMx) / zoom;
            cz -= (my - lastMy) / zoom;
            lastMx = mx; lastMy = my;
            dirty = true;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (mx >= width - SIDE) return false;
        double bx = blockX(mx), bz = blockZ(my);
        zoom = Math.max(0.125f, Math.min(8f, zoom * (v > 0 ? 1.25f : 0.8f)));
        // przybliżaj w stronę kursora
        cx = bx - (mx - texW / 2.0) / zoom;
        cz = bz - (my - texH / 2.0) / zoom;
        dirty = true;
        return true;
    }
}
