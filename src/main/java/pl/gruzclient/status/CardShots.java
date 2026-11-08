//#only MC >= 26.3
package pl.gruzclient.status;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import pl.gruzclient.GruzClient;

/**
 * Screeny na karty wersji w launcherze (tylko lokalnie: GRUZ_CARDS=1 ./gradlew :26.3:runClient, świat run/saves/GruzTest).
 * Dla każdego motywu szuka biomu, przenosi gracza (tryb widza) nad niego w południe, chowa HUD i robi zrzut ekranu.
 */
public final class CardShots {
    private static final boolean ON = Boolean.getBoolean("gruz.cardshots");
    /** karta w launcherze -> biom z tej aktualizacji */
    private static final String[] CARD = {"26", "1.21", "1.20"};
    private static final ResourceKey<Biome>[] BIOME = cast(Biomes.DAPPLED_FOREST, Biomes.PALE_GARDEN, Biomes.CHERRY_GROVE);
    private static final int STEP = 420; // ticki na jeden motyw (wczytanie chunków)
    private static boolean opened;
    private static int t = -1;
    private static int tx, tz;

    private CardShots() {}

    @SafeVarargs
    private static ResourceKey<Biome>[] cast(ResourceKey<Biome>... k) { return k; }

    private static void log(String s) { GruzClient.LOG.info("GRUZ_CARD " + s); }

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
        if (mc.screen != null) mc.setScreen(null);
        t++;
        String name = mc.player.getScoreboardName();
        if (t == 20) { cmd("gamemode spectator " + name); cmd("weather clear 1000000"); cmd("gamerule doDaylightCycle false"); }
        int i = (t - 40) / STEP, k = (t - 40) % STEP;
        if (t < 40) return;
        if (i >= CARD.length) { if (k == 20) { log("DONE"); mc.stop(); } return; }
        if (k == 0) {
            var srv = mc.getSingleplayerServer();
            if (srv == null) return;
            int idx = i;
            srv.execute(() -> {
                var lvl = srv.overworld();
                var found = lvl.findClosestBiome3d(h -> h.is(BIOME[idx]), BlockPos.ZERO, 6400, 32, 64);
                if (found == null) { log("brak biomu " + BIOME[idx]); return; }
                BlockPos p = found.getFirst();
                tx = p.getX(); tz = p.getZ();
                log(CARD[idx] + " -> " + BIOME[idx] + " " + tx + " " + tz);
                // najpierw wysoko – teren pod spodem jeszcze nie istnieje (wysokość wyszłaby -64)
                srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack(), "tp " + name + " " + tx + " 220 " + tz + " 35 22");
                srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack(), "time set " + (idx == 1 ? "13000" : "1000"));
            });
        }
        if (k == 200) {
            // chunki już wczytane u klienta – schodzimy 8 bloków nad teren
            int y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, tx, tz);
            log(CARD[i] + " teren y=" + y);
            cmd("tp " + name + " " + tx + " " + (y + 8) + " " + tz + " 35 22");
        }
        if (k == STEP - 20) hideHud(true);
        if (k == STEP - 10) {
            Screenshot.grab(mc.gameDirectory, "card-" + CARD[i] + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> log("shot " + CARD[i]));
        }
        if (k == STEP - 2) hideHud(false);
    }
}
