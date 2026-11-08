package pl.gruzclient.legacy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotUtils;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelInfo;

/**
 * Test w grze (tylko lokalnie: -Dgruz.featuretest=true): nowy świat kreatywny, HUD, freelook, zoom, menu – screeny
 * ft-*.png w run/screenshots i wpisy GRUZ_FT w logu. Potem zamyka grę.
 */
public final class FeatureTest {
    private FeatureTest() {}

    private static final boolean ON = Boolean.getBoolean("gruz.featuretest");
    private static int t = -1;
    private static boolean started;

    private static void log(String s) { GruzLegacy.LOG.info("GRUZ_FT " + s); }

    private static void shot(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ScreenshotUtils.saveScreenshot(mc.runDirectory, "ft-" + name + ".png", mc.width, mc.height, mc.getFramebuffer());
        log("shot " + name);
    }

    public static void tick() {
        if (!ON) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!started) {
            if (mc.currentScreen == null || mc.world != null) return;
            started = true;
            log("start world");
            mc.startIntegratedServer("GruzTest", "GruzTest", new LevelInfo(12345L, LevelInfo.GameMode.CREATIVE, true, false, LevelGeneratorType.DEFAULT));
            return;
        }
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null && t < 190) mc.setScreen(null);
        t++;
        switch (t) {
            case 100:
                Mods.cfg.coords = true;
                mc.player.pitch = 10f;
                shot("00-hud");
                break;
            case 120:
                Mods.forceFreelook = true; // jak przytrzymany lewy Alt
                break;
            case 125:
                Mods.lookYaw = mc.player.yaw + 120f;
                Mods.lookPitch = 20f;
                break;
            case 140:
                shot("01-freelook");
                log("freelook player yaw=" + mc.player.yaw + " camera yaw=" + Mods.lookYaw);
                Mods.forceFreelook = false;
                break;
            case 160:
                shot("02-normal");
                break;
            case 200:
                mc.setScreen(new GruzMenuScreen());
                break;
            case 230:
                shot("03-menu");
                break;
            case 260:
                log("DONE");
                mc.scheduleStop();
                break;
            default:
                break;
        }
    }
}
