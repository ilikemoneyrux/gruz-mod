package pl.gruzclient.module.impl;

import pl.gruzclient.compat.Compat;
import org.lwjgl.glfw.GLFW;
import pl.gruzclient.GruzClient;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.map.MapCache;
import pl.gruzclient.map.Waypoints;
import pl.gruzclient.module.HudModule;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.Setting;
import pl.gruzclient.screen.WorldMapScreen;

import static pl.gruzclient.render.GruzDraw.*;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Minimapa w rogu ekranu (kolory terenu z góry), waypointy, miejsce śmierci,
 * kompas na górze ekranu i pełna mapa świata pod M.
 */
public class MinimapModule extends HudModule {
    public final Setting.Number size = add(new Setting.Number("size", "setting.mapSize", 96f, 64f, 160f, 8f, " px"));
    public final Setting.Number zoom = add(new Setting.Number("zoom", "setting.mapZoom", 1f, 0.5f, 4f, 0.5f, "x"));
    public final Setting.Bool rotate = add(new Setting.Bool("rotate", "setting.mapRotate", false));
    public final Setting.Bool compass = add(new Setting.Bool("compass", "setting.compass", true));
    public final Setting.Bool deathPoint = add(new Setting.Bool("death", "setting.deathPoint", true));
    public final Setting.Key mapKey = add(new Setting.Key("mapKey", "setting.mapKey", GLFW.GLFW_KEY_M));
    public final Setting.Key addKey = add(new Setting.Key("addKey", "setting.waypointKey", GLFW.GLFW_KEY_N));

    private static final int TEX = 160;
    private static final ResourceLocation ID = Compat.id(GruzClient.MOD_ID, "minimap");
    private DynamicTexture texture;
    private boolean wasMap, wasAdd, wasDead;
    private int saveTicks = 0;

    public MinimapModule() { super("minimap", false, 0.86f, 0.02f); }

    @Override
    public void tick() {
        if (mc.player == null) return;
        MapCache.tick(4);
        if (++saveTicks % 1200 == 0) MapCache.save(); // co minutę
        boolean m = Modules.keyDown(mapKey.code), a = Modules.keyDown(addKey.code);
        if (m && !wasMap) mc.setScreen(new WorldMapScreen(null));
        if (a && !wasAdd) {
            var p = Waypoints.add(Lang.t("map.point") + " " + (Waypoints.current().size() + 1),
                    mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(), false);
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§d[Gruz] §f" + Lang.t("map.added").replace("{name}", p.name)), true);
        }
        wasMap = m; wasAdd = a;
        boolean dead = mc.player.isDeadOrDying();
        if (dead && !wasDead && deathPoint.value) {
            Waypoints.add(Lang.t("map.death"), mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(), true);
        }
        wasDead = dead;
    }

    @Override
    protected void onDisable() { MapCache.save(); }

    private void ensureTexture() {
        if (texture != null) return;
        texture = Compat.texture("gruz_minimap", TEX, TEX);
        mc.getTextureManager().register(ID, texture);
    }

    @Override
    protected int[] draw(GuiGraphics c, boolean editing) {
        int s = Math.round(size.value);
        if (mc.player == null) {
            c.fill(0, 0, s, s, argb(0.7f, SLAB));
            return new int[]{s, s};
        }
        ensureTexture();
        double px = mc.player.getX(), pz = mc.player.getZ();
        float bpp = 1f / zoom.value; // bloków na piksel
        int half = TEX / 2;
        long now = System.currentTimeMillis();
        // tekstura odświeżana 5 razy na sekundę albo po ruchu, żeby nie zjadać FPS
        if (now - lastRefresh > 200 || Math.abs(px - lastPx) > bpp || Math.abs(pz - lastPz) > bpp) {
            lastRefresh = now; lastPx = px; lastPz = pz;
            refresh(px, pz, bpp, half);
        }

        // ramka i mapa (przycięta do kwadratu)
        c.fill(-2, -2, s + 2, s + 2, argb(0.85f, SLAB));
        c.renderOutline(-2, -2, s + 4, s + 4, argb(0.9f, NEON));
        var m = pl.gruzclient.compat.Pose2D.of(c);
        c.enableScissor(Math.round(screenX()), Math.round(screenY()), Math.round(screenX() + s * scale), Math.round(screenY() + s * scale));
        m.push();
        m.translate(s / 2f, s / 2f);
        if (rotate.value) m.rotateDeg(180f - mc.player.getYRot());
        float k = (float) s / TEX * 1.42f; // zapas na obrót
        if (!rotate.value) k = (float) s / TEX;
        m.scale(k, k);
        Compat.blitWhole(c, ID, -half, -half, TEX, TEX, -1);
        // waypointy
        for (Waypoints.Point p : Waypoints.current()) {
            float dx = (float) ((p.x + 0.5 - px) / bpp), dz = (float) ((p.z + 0.5 - pz) / bpp);
            float lim = half - 3;
            dx = Math.max(-lim, Math.min(lim, dx));
            dz = Math.max(-lim, Math.min(lim, dz));
            int d = Math.max(2, Math.round(2.5f / k));
            c.fill(Math.round(dx) - d, Math.round(dz) - d, Math.round(dx) + d, Math.round(dz) + d, 0xFF000000);
            c.fill(Math.round(dx) - d + 1, Math.round(dz) - d + 1, Math.round(dx) + d - 1, Math.round(dz) + d - 1, 0xFF000000 | p.color);
        }
        m.pop();
        c.disableScissor();

        // strzałka gracza
        m.push();
        m.translate(s / 2f, s / 2f);
        if (!rotate.value) m.rotateDeg(mc.player.getYRot() + 180f);
        c.fill(-1, -4, 1, 3, 0xFFFFFFFF);
        c.fill(-3, -1, 3, 1, argb(1f, NEON_HOT));
        c.fill(-1, -5, 1, -3, argb(1f, NEON));
        m.pop();

        // koordynaty pod mapą
        String xyz = mc.player.getBlockX() + ", " + mc.player.getBlockY() + ", " + mc.player.getBlockZ();
        c.drawString(mc.font, xyz, (s - mc.font.width(xyz)) / 2, s + 4, textColor(), true);
        if (rotate.value) c.drawString(mc.font, "N", s / 2 - 2, 1, argb(1f, NEON_HOT), true);
        return new int[]{s, s + 14};
    }

    private double lastPx = Double.NaN, lastPz = Double.NaN;
    private long lastRefresh = 0;

    private void refresh(double px, double pz, float bpp, int half) {
        NativeImage img = texture.getPixels();
        if (img == null) return;
        for (int ty = 0; ty < TEX; ty++) {
            for (int tx = 0; tx < TEX; tx++) {
                int bx = (int) Math.floor(px + (tx - half) * bpp), bz = (int) Math.floor(pz + (ty - half) * bpp);
                int col = MapCache.colorAt(bx, bz);
                if (col == 0) col = 0xFF0C0912;
                int r = (col >> 16) & 255, g = (col >> 8) & 255, b = col & 255;
                Compat.setPixelABGR(img, tx, ty, 0xFF000000 | (b << 16) | (g << 8) | r); // NativeImage = ABGR
            }
        }
        texture.upload();
    }

    private float screenX() { return x * mc.getWindow().getGuiScaledWidth(); }
    private float screenY() { return y * mc.getWindow().getGuiScaledHeight(); }

    public boolean compassShown() { return isEnabled() && compass.value && mc.player != null && !mc.options.hideGui; }

    /** Kompas na górze ekranu z kierunkami i waypointami */
    public void renderCompass(GuiGraphics c) {
        if (!compassShown()) return;
        int w = c.guiWidth();
        int cw = Math.min(260, w / 2), cx = w / 2, y = 4;
        c.fill(cx - cw / 2, y, cx + cw / 2, y + 12, argb(0.45f, SLAB));
        float yaw = ((mc.player.getYRot() % 360) + 360) % 360; // 0 = południe
        String[] dirs = {Lang.t("dir.s.short"), Lang.t("dir.w.short"), Lang.t("dir.n.short"), Lang.t("dir.e.short")};
        for (int i = 0; i < 4; i++) {
            float diff = wrap(i * 90f - yaw);
            if (Math.abs(diff) > 70) continue;
            int x = cx + Math.round(diff / 70f * cw / 2);
            c.drawString(mc.font, dirs[i], x - mc.font.width(dirs[i]) / 2, y + 2, i == 2 ? argb(1f, NEON_HOT) : argb(1f, TEXT), true);
        }
        for (Waypoints.Point p : Waypoints.current()) {
            double dx = p.x + 0.5 - mc.player.getX(), dz = p.z + 0.5 - mc.player.getZ();
            float ang = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float diff = wrap(ang - yaw);
            if (Math.abs(diff) > 70) continue;
            int x = cx + Math.round(diff / 70f * cw / 2);
            c.fill(x - 2, y + 9, x + 2, y + 13, 0xFF000000 | p.color);
            if (Math.abs(diff) < 6) {
                String label = p.name + " · " + Math.round(Math.sqrt(dx * dx + dz * dz)) + " m";
                c.drawString(mc.font, label, x - mc.font.width(label) / 2, y + 15, 0xFF000000 | p.color, true);
            }
        }
        c.fill(cx, y, cx + 1, y + 12, argb(0.9f, NEON));
    }

    private static float wrap(float a) {
        a %= 360f;
        if (a > 180) a -= 360;
        if (a < -180) a += 360;
        return a;
    }
}
