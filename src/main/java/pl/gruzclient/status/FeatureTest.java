//#only MC >= 26.1 && MC < 26.2
package pl.gruzclient.status;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.level.levelgen.Heightmap;
import pl.gruzclient.GruzClient;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;

/**
 * Test funkcji w grze (tylko lokalnie: -Dgruz.featuretest=true, GRUZ_FT=1 ./gradlew :<mc>:runClient).
 * Wczytuje świat "GruzTest" z run/saves, stawia gracza na ziemi, włącza moduły po kolei, robi screeny ft-*.png
 * i zapisuje w logu ruch kamery we freecamie (bez wejścia, z klawiszem W i z ruchem myszy). Potem zamyka grę.
 */
public final class FeatureTest {
    private static final boolean ON = Boolean.getBoolean("gruz.featuretest");
    private static int t = -1;
    private static boolean opened;

    private FeatureTest() {}

    private static void log(String s) { GruzClient.LOG.info("GRUZ_FT " + s); }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "ft-" + name + ".png", mc.getMainRenderTarget(), 1, c -> log("shot " + name));
    }

    public static void tick() {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (!opened) {
            if (mc.getOverlay() != null || mc.screen == null) return;
            opened = true;
            log("open world");
            mc.createWorldOpenFlows().openWorld("GruzTest", () -> log("open cancelled"));
            return;
        }
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null && t > 0 && t < 530) mc.setScreen(null); // okno bez fokusu otwiera pauzę – zamykamy
        t++;
        var p = mc.player;
        switch (t) {
            case 2 -> {
                // tryb kreatywny (bez obrażeń od upadku) jeszcze przed teleportem
                var srv = mc.getSingleplayerServer();
                if (srv != null) srv.execute(() -> {
                    var sp = srv.getPlayerList().getPlayer(p.getUUID());
                    if (sp != null) sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                });
            }
            case 60 -> {
                // gracz na ziemi (teleport po stronie serwera świata jednoosobowego)
                var srv = mc.getSingleplayerServer();
                if (srv != null) srv.execute(() -> {
                    var sp = srv.getPlayerList().getPlayer(p.getUUID());
                    if (sp == null) return;
                    var lvl = sp.level();
                    int x = sp.getBlockX(), z = sp.getBlockZ();
                    int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    sp.teleportTo(x + 0.5, y, z + 0.5);
                });
            }
            case 120 -> {
                for (Module m : Modules.ALL) if (m.isEnabled()) log("on " + m.id);
                p.setXRot(70f); p.setYRot(0f);
                shot("00-start");
            }
            case 140 -> {
                Modules.MINIMAP.setEnabled(true);
                Modules.BLOCK_OVERLAY.setEnabled(true);
                Modules.BLOCK_OVERLAY.fill.value = true;
                Modules.BLOCK_OVERLAY.thickness.value = 3f;
            }
            case 150 -> {
                // przedmioty na ziemi przed graczem (item physics)
                Modules.ITEM_PHYSICS.setEnabled(true);
                var srv = mc.getSingleplayerServer();
                if (srv != null) srv.execute(() -> {
                    var sp = srv.getPlayerList().getPlayer(p.getUUID());
                    if (sp == null) return;
                    var lvl = sp.level();
                    net.minecraft.world.item.Item[] items = {net.minecraft.world.item.Items.DIAMOND_SWORD, net.minecraft.world.item.Items.STONE,
                            net.minecraft.world.item.Items.APPLE, net.minecraft.world.item.Items.OAK_LOG};
                    for (int i = 0; i < items.length; i++) {
                        var e = new net.minecraft.world.entity.item.ItemEntity(lvl, sp.getX() - 0.9 + i * 0.6, sp.getY() + 0.3, sp.getZ() + 1.6,
                                new net.minecraft.world.item.ItemStack(items[i]), 0, 0, 0);
                        e.setPickUpDelay(32767);
                        lvl.addFreshEntity(e);
                    }
                });
                // czat: kopiowanie wiadomości pod kursorem (prawy przycisk)
                for (int i = 1; i <= 3; i++) mc.gui.getChat().addClientSystemMessage(net.minecraft.network.chat.Component.literal("TEST LINE " + i));
            }
            case 200 -> {
                log("hit " + mc.hitResult.getType() + " " + mc.hitResult.getLocation());
                shot("01-minimap-overlay");
                Object chat = mc.gui.getChat();
                int h = mc.getWindow().getGuiScaledHeight();
                if (chat instanceof pl.gruzclient.chat.GruzChat g) {
                    log("chat bottom='" + g.gruz$messageAt(10, h - 40 - 4) + "' second='" + g.gruz$messageAt(10, h - 40 - 13) + "' far='" + g.gruz$messageAt(10, 5) + "'");
                } else log("chat: brak GruzChat");
            }
            case 220 -> {
                try {
                    var start = Modules.FREECAM.getClass().getDeclaredMethod("start");
                    start.setAccessible(true);
                    start.invoke(Modules.FREECAM);
                } catch (Exception e) { log("freecam start failed " + e); }
                log("freecam active=" + Modules.FREECAM.isActive());
            }
            case 235 -> { log("press jump"); mc.options.keyJump.setDown(true); }
            case 250 -> { log("release jump"); mc.options.keyJump.setDown(false); }
            case 260 -> { log("press W"); mc.options.keyUp.setDown(true); }
            case 280 -> { log("release W"); mc.options.keyUp.setDown(false); }
            case 300 -> log("mouse turn start");
            case 320 -> { log("mouse turn end"); shot("02-freecam"); }
            case 360 -> { log("press D"); mc.options.keyRight.setDown(true); }
            case 375 -> { log("release D"); mc.options.keyRight.setDown(false); }
            case 400 -> {
                Modules.FREECAM.stop();
                Modules.WORLD.time.index = 5; // północ – do testu fullbright
                Modules.WORLD.setEnabled(true);
                Modules.WORLD.cosmicSky.value = true;
                p.setXRot(-30f);
            }
            case 405 -> {
                // freelook: kamera obrócona o 90° względem postaci (tryb przełącz – bez trzymania Alt)
                Modules.FREELOOK.mode.index = 1;
                try {
                    var start = Modules.FREELOOK.getClass().getDeclaredMethod("start");
                    start.setAccessible(true);
                    start.invoke(Modules.FREELOOK);
                } catch (Exception e) { log("freelook start failed " + e); }
                Modules.FREELOOK.yaw = p.getYRot() + 90f;
            }
            case 412 -> {
                log(String.format(java.util.Locale.ROOT, "freelook active=%s view=%.1f player=%.1f",
                        Modules.FREELOOK.isActive(), mc.gameRenderer.getMainCamera().yRot(), p.getYRot()));
                try {
                    var stop = Modules.FREELOOK.getClass().getDeclaredMethod("stop");
                    stop.setAccessible(true);
                    stop.invoke(Modules.FREELOOK);
                } catch (Exception e) { log("freelook stop failed " + e); }
            }
            case 440 -> shot("03-cosmic");
            case 445 -> {
                // ciemny pokój z kamienia 40 bloków wyżej (bez światła) – do porównania fullbright wył./wł.
                Modules.WORLD.setEnabled(false);
                var srv = mc.getSingleplayerServer();
                if (srv != null) srv.execute(() -> {
                    var sp = srv.getPlayerList().getPlayer(p.getUUID());
                    if (sp == null) return;
                    var lvl = sp.level();
                    var c = sp.blockPosition().above(40);
                    for (int dx = -3; dx <= 3; dx++) for (int dy = -1; dy <= 4; dy++) for (int dz = -3; dz <= 3; dz++) {
                        boolean shell = Math.abs(dx) == 3 || Math.abs(dz) == 3 || dy == -1 || dy == 4;
                        lvl.setBlockAndUpdate(c.offset(dx, dy, dz), shell ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                                : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                    sp.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
                });
                p.setXRot(30f);
            }
            case 456 -> shot("03a-dark");
            case 460 -> { Modules.FULLBRIGHT.setEnabled(true); }
            case 470 -> shot("03b-fullbright");
            case 475 -> {
                Modules.WORLD.setEnabled(false);
                p.setXRot(20f);
                for (Module m : Modules.ALL) {
                    if (m == Modules.FREECAM || m == Modules.FREELOOK || m == Modules.ZOOM || m == Modules.MOTION_BLUR || m == Modules.RECORDING) continue;
                    m.setEnabled(true);
                }
                log("all modules on");
            }
            case 510 -> { shot("04-all"); log("fullbright=" + Modules.FULLBRIGHT.isEnabled() + ""); }
            case 530 -> mc.setScreen(new pl.gruzclient.screen.GruzModsScreen(null));
            case 570 -> shot("05-mods");
            // podglądy w ustawieniach modułów (celownik w nowym stylu „okrąg”, block overlay, hit color)
            case 580 -> { Modules.CROSSHAIR.style.index = 6; Modules.CROSSHAIR.dotSize.set(2); mc.setScreen(new pl.gruzclient.screen.GruzModsScreen(null, Modules.CROSSHAIR)); }
            case 600 -> shot("06-preview-crosshair");
            case 605 -> mc.setScreen(new pl.gruzclient.screen.GruzModsScreen(null, Modules.BLOCK_OVERLAY));
            case 625 -> shot("07-preview-overlay");
            case 630 -> mc.setScreen(new pl.gruzclient.screen.GruzModsScreen(null, Modules.HIT_COLOR));
            case 648 -> shot("08-preview-hitcolor");
            // kolorowy nick na liście graczy (Tab) i kosmetyki z bliska (GRUZ_FT_COS = zestaw jak w -Dgruz.cosmetics)
            case 645 -> {
                // w świecie jednoosobowym Tab pokazuje się tylko z celem na liście (jak „życie” na serwerach)
                var srv = mc.getSingleplayerServer();
                if (srv != null) srv.execute(() -> {
                    for (String c : new String[]{"scoreboard objectives add gtest dummy", "scoreboard objectives setdisplay list gtest", "scoreboard players set " + p.getScoreboardName() + " gtest 20"})
                        srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack(), c);
                });
            }
            case 650 -> { mc.setScreen(null); mc.options.keyPlayerList.setDown(true); }
            case 652 -> shot("09a-tab-plain");
            case 655 -> {
                // nazwa ze starymi kodami § – tak wysyła ją wiele serwerów (rangi w Tab)
                Modules.COSMETICS.preview("name", "rainbow");
                var info = mc.getConnection().getPlayerInfo(p.getUUID());
                if (info != null) info.setTabListDisplayName(net.minecraft.network.chat.Component.literal("§7[§eVIP§7] §a" + p.getScoreboardName()));
            }
            case 675 -> shot("09-tab-name");
            case 680 -> {
                mc.options.keyPlayerList.setDown(false);
                Modules.COSMETICS.preview(null, null);
                String cos = System.getenv("GRUZ_FT_COS");
                if (cos != null) { System.setProperty("gruz.cosmetics", cos); System.setProperty("gruz.unlocked", "all"); Modules.COSMETICS.applyLauncherChoice(); }
                // czysta scena: bez modułów (hitboxy, HUD), bez zbroi, w dzień
                for (Module m : Modules.ALL) if (m != Modules.COSMETICS && m.isEnabled()) m.setEnabled(false);
                var srv = mc.getSingleplayerServer();
                if (srv != null) srv.execute(() -> {
                    for (String c : new String[]{"time set noon", "item replace entity " + p.getScoreboardName() + " armor.head with air", "item replace entity " + p.getScoreboardName() + " armor.chest with air",
                            "item replace entity " + p.getScoreboardName() + " armor.legs with air", "item replace entity " + p.getScoreboardName() + " armor.feet with air"})
                        srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack(), c);
                });
                mc.options.hideGui = true;
                p.setXRot(10f); p.setYRot(180f);
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            }
            case 710 -> shot("10-cos-back");
            case 715 -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            case 745 -> shot("11-cos-front");
            case 750 -> { p.setYRot(270f); }
            case 775 -> shot("12-cos-side");
            case 785 -> { log("DONE"); mc.stop(); }
            default -> { }
        }
        if (t > 300 && t < 320) p.turn(8.0, 0.0); // „ruch myszy” w prawo
        if (Modules.FREECAM.isActive() && t >= 220 && t <= 400) {
            var cam = mc.getCameraEntity();
            var view = mc.gameRenderer.getMainCamera();
            log(String.format(java.util.Locale.ROOT, "cam t=%d pos=%.3f %.3f %.3f yaw=%.2f pitch=%.2f view=%.2f/%.2f player=%.3f %.3f %.3f pyaw=%.2f",
                    t, cam.getX(), cam.getY(), cam.getZ(), cam.getYRot(), cam.getXRot(), view.yRot(), view.xRot(), p.getX(), p.getY(), p.getZ(), p.getYRot()));
        }
    }
}
