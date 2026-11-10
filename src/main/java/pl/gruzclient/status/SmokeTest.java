package pl.gruzclient.status;

import net.minecraft.client.Minecraft;
import pl.gruzclient.GruzClient;

/**
 * Test uruchomienia w CI (-Dgruz.smoketest=true): gra startuje, pokazuje menu Gruz,
 * otwiera kilka ekranów moda i sama się zamyka. Błędy miksów wywalają grę po drodze,
 * więc "GRUZ_SMOKE_OK" w logu znaczy, że mod naprawdę działa na tej wersji.
 */
public final class SmokeTest {
    private static final boolean ON = Boolean.getBoolean("gruz.smoketest");
    private static int ticks, step;

    private SmokeTest() {}

    public static void tick() {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null || mc.getOverlay() != null) return;
        if (++ticks < 60) return;
        ticks = 0;
        GruzClient.LOG.info("GRUZ_SMOKE step {} screen {}", step, mc.screen.getClass().getName());
        switch (step++) {
            case 0 -> mc.setScreen(new pl.gruzclient.screen.GruzModsScreen(mc.screen));
            case 1 -> {
                //#if MC >= 26.3
                // -Dgruz.smokeshot=true: zrzut menu modów (smoke-mods.png) – do sprawdzenia czcionki w grze uruchomionej z launchera
                if (Boolean.getBoolean("gruz.smokeshot")) net.minecraft.client.Screenshot.grab(mc.gameDirectory, "smoke-mods.png", mc.gameRenderer.mainRenderTarget(), 1, c -> GruzClient.LOG.info("GRUZ_SMOKE shot"));
                //#endif
                mc.setScreen(new pl.gruzclient.screen.HudEditorScreen(null));
            }
            case 2 -> mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
            default -> {
                // wczytuje wszystkie klasy, w które wpinają się miksy – także te ze świata gry, do których ten test nie dochodzi
                // (gracz, pakiety, kamera…). Zepsuty miks wywala grę tutaj, zamiast u gracza po wejściu do świata.
                org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
                GruzClient.LOG.info("GRUZ_SMOKE_AUDIT done");
                GruzClient.LOG.info("GRUZ_SMOKE_OK");
                mc.stop();
            }
        }
    }
}
