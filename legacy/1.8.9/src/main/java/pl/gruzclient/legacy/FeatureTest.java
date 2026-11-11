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

    /** Zdjęcie na kartę wersji w launcherze (-Dgruz.cardshot=true): świat z nasionem gruz.cardseed, 8 ujęć bez HUD */
    private static final boolean CARD = Boolean.getBoolean("gruz.cardshot");
    private static final boolean ON = Boolean.getBoolean("gruz.featuretest") || CARD;
    private static int t = -1;
    private static boolean started;

    private static void log(String s) { GruzLegacy.LOG.info("GRUZ_FT " + s); }

    private static void shot(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ScreenshotUtils.saveScreenshot(mc.runDirectory, "ft-" + name + ".png", mc.width, mc.height, mc.getFramebuffer());
        log("shot " + name);
    }

    private static double cardX, cardY, cardZ;

    private static void card(MinecraftClient mc) {
        if (t == 40) {
            mc.options.hudHidden = true;
            mc.options.viewDistance = 16;
            mc.player.sendChatMessage("/time set 6000");
            mc.player.sendChatMessage("/weather clear");
            mc.player.abilities.flying = true;
            mc.player.sendAbilitiesUpdate();
            cardX = mc.player.x; cardY = mc.player.y + 22; cardZ = mc.player.z;
        }
        if (t > 40) { // wisimy nad miejscem startu (serwer nie ściąga gracza w trybie kreatywnym)
            mc.player.updatePosition(cardX, cardY, cardZ);
            mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
        }
        int k = t - 520; // czas na wczytanie terenu i zniknięcie powiadomienia o osiągnięciu
        if (k >= 0 && k < 8 * 30) {
            if (k % 30 == 0) { mc.player.yaw = (k / 30) * 45f; mc.player.pitch = 14f; }
            if (k % 30 == 20) shot("card-" + Long.getLong("gruz.cardseed", 1L) + "-" + (k / 30));
        }
        if (k == 8 * 30) { log("DONE"); mc.scheduleStop(); }
    }

    public static void tick() {
        if (!ON) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!started) {
            if (mc.currentScreen == null || mc.world != null) return;
            started = true;
            log("start world");
            if (CARD) {
                String w = "GruzCard" + Long.getLong("gruz.cardseed", 1L);
                mc.startIntegratedServer(w, w, new LevelInfo(Long.getLong("gruz.cardseed", 1L), LevelInfo.GameMode.CREATIVE, true, false, LevelGeneratorType.DEFAULT).enableCommands());
            } else
            mc.startIntegratedServer("GruzTest", "GruzTest", new LevelInfo(12345L, LevelInfo.GameMode.CREATIVE, true, false, LevelGeneratorType.DEFAULT));
            return;
        }
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null && t < 190) mc.setScreen(null);
        t++;
        if (CARD) { card(mc); return; }
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
            case 240: // sieć Gruz i peleryny (0.2.0): widok z tyłu, komendy na czacie
                mc.setScreen(null);
                mc.options.perspective = 1;
                mc.player.sendChatMessage(".gruz");
                break;
            case 700:
                mc.player.sendChatMessage(".friends");
                break;
            case 720:
                mc.player.sendChatMessage(".r no hej");
                break;
            case 770:
                mc.player.sendChatMessage(".party accept");
                break;
            case 800:
                shot("04-cape-chat");
                log("net connected=" + GruzNet.connected() + " friends=" + GruzNet.FRIENDS.size());
                GruzNet.myCosmetics = "cape=aurora;color=b45cff";
                break;
            case 850:
                shot("05-cape-anim");
                break;
            case 870:
                log("DONE");
                mc.scheduleStop();
                break;
            default:
                break;
        }
    }
}
