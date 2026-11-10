//#only MC >= 26.3
package pl.gruzclient.status;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import pl.gruzclient.GruzClient;
import pl.gruzclient.module.Modules;

/**
 * Screeny z gry na stronę (tylko lokalnie: GRUZ_PROMO=1 ./gradlew :26.3:runClient, świat run/saves/GruzTest):
 * HUD w pierwszej osobie, menu modów i kosmetyki w trzeciej osobie – po 4 ujęcia (obrót co 90°), wybiera się najlepsze.
 */
public final class PromoShots {
    private static final boolean ON = Boolean.getBoolean("gruz.promoshots");
    private static boolean opened;
    private static int t = -1;
    private static int tx, ty, tz;

    private PromoShots() {}

    private static void log(String s) { GruzClient.LOG.info("GRUZ_PROMO " + s); }

    private static void cmd(String c) {
        var srv = Minecraft.getInstance().getSingleplayerServer();
        if (srv != null) srv.execute(() -> srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack(), c));
    }

    private static void hideHud(boolean hide) {
        try {
            var f = Minecraft.getInstance().gui.hud.getClass().getDeclaredField("isHidden");
            f.setAccessible(true);
            f.setBoolean(Minecraft.getInstance().gui.hud, hide);
        } catch (Exception e) { log("hud " + e); }
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "promo-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> log("shot " + name));
    }

    public static void tick() {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (!opened) {
            if (mc.getOverlay() != null || mc.screen == null) return;
            opened = true;
            mc.createWorldOpenFlows().openWorld("GruzTest", () -> log("open cancelled"));
            return;
        }
        if (mc.player == null || mc.level == null) return;
        t++;
        String n = mc.player.getScoreboardName();
        if (t == 20) {
            cmd("gamemode spectator " + n); cmd("weather clear 1000000"); cmd("gamerule doDaylightCycle false"); cmd("time set 1000");
            var srv = mc.getSingleplayerServer();
            srv.execute(() -> {
                var found = srv.overworld().findClosestBiome3d(h -> h.is(Biomes.CHERRY_GROVE), BlockPos.ZERO, 6400, 32, 64);
                if (found == null) { log("brak biomu"); return; }
                tx = found.getFirst().getX(); tz = found.getFirst().getZ();
                srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack(), "tp " + n + " " + tx + " 220 " + tz + " 0 8");
            });
        }
        if (t == 220) {
            ty = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, tx, tz) + 1;
            cmd("gamemode survival " + n);
            cmd("tp " + n + " " + tx + " " + ty + " " + tz + " 0 8");
            for (String[] s : new String[][]{{"armor.head", "diamond_helmet"}, {"armor.chest", "diamond_chestplate"}, {"armor.legs", "diamond_leggings"},
                    {"armor.feet", "diamond_boots"}, {"hotbar.0", "diamond_sword"}, {"hotbar.1", "golden_apple 16"}, {"hotbar.2", "ender_pearl 16"}, {"hotbar.3", "oak_planks 64"}}) {
                cmd("item replace entity " + n + " " + s[0] + " with " + s[1]);
            }
            cmd("effect give " + n + " speed 3000 1 true"); cmd("effect give " + n + " strength 3000 0 true"); cmd("effect give " + n + " saturation 5 5");
            for (var m : new pl.gruzclient.module.Module[]{Modules.FPS, Modules.CPS, Modules.KEYSTROKES, Modules.ARMOR, Modules.POTIONS, Modules.COORDS, Modules.MINIMAP}) m.setEnabled(true);
        }
        int k = t - 520; // komunikaty komend na czacie zdążą zniknąć
        if (k < 0) return;
        int j = k / 40, r = k % 40; // 0–3: HUD, 4–7: kosmetyki od tyłu, 8: menu modów
        if (j < 8) {
            if (r == 0) {
                cmd("tp " + n + " " + tx + " " + ty + " " + tz + " " + (j % 4) * 90 + " " + (j < 4 ? 8 : 12));
                mc.options.setCameraType(j < 4 ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
                hideHud(j >= 4);
                // kosmetyki pokazujemy bez zbroi (zasłania pelerynę i skrzydła)
                if (j == 4) for (String slot : new String[]{"armor.head", "armor.chest", "armor.legs", "armor.feet"}) cmd("item replace entity " + n + " " + slot + " with air");
            }
            if (r == 30) shot((j < 4 ? "hud-" : "cos-") + j % 4);
        } else if (j == 8) {
            if (r == 0) { mc.options.setCameraType(CameraType.FIRST_PERSON); hideHud(false); mc.setScreen(new pl.gruzclient.screen.GruzModsScreen(null)); }
            if (r == 30) shot("menu");
        } else if (r == 10) { log("DONE"); mc.stop(); }
    }
}
