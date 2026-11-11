package pl.gruzclient.legacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Gruz Client 1.8.9 (Legacy Fabric): stan modułów, ustawienia (config/gruzclient-1.8.9.json) i logika co tick.
 * Osobny, mały mod – kod 1.20+ (mojmap, render state) nie pasuje do 1.8.9 (yarn, LWJGL2, GL immediate mode).
 */
public final class Mods {
    private Mods() {}

    /** Zapisywane ustawienia (nazwy pól = klucze w JSON) */
    public static final class Config {
        public boolean fps = true, cps = true, coords = false, keystrokes = true, armor = true, potions = true;
        public boolean toggleSprint = false, fullbright = false, zoom = true, freelook = true;
        /** Sieć Gruz (od 0.2.0): połączenie z serwerem, peleryny, powiadomienia o znajomych */
        public boolean network = true, capes = true, friendAlerts = true;
        public int zoomKey = Keyboard.KEY_C, freelookKey = Keyboard.KEY_LMENU, menuKey = Keyboard.KEY_RSHIFT;
        public float oldGamma = 1f;
    }

    public static Config cfg = new Config();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File file() { return new File(MinecraftClient.getInstance().runDirectory, "config/gruzclient-1.8.9.json"); }

    public static void load() {
        try (FileReader r = new FileReader(file())) {
            Config c = GSON.fromJson(r, Config.class);
            if (c != null) cfg = c;
        } catch (Exception ignored) { /* pierwszy start – domyślne */ }
    }

    public static void save() {
        try {
            file().getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(file())) { GSON.toJson(cfg, w); }
        } catch (Exception e) { GruzLegacy.LOG.warn("Gruz: nie zapisano ustawień: {}", e.toString()); }
    }

    public static boolean pl() { return MinecraftClient.getInstance().options.language.startsWith("pl"); }

    // ---------------- CPS ----------------
    private static final Deque<Long> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();

    /** KeyBinding.onKeyPressed: kod klawisza jak w ustawieniach (mysz: -100 lewy, -99 prawy) */
    public static void keyPressed(int code) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;
        long now = System.currentTimeMillis();
        if (code == mc.options.attackKey.getCode()) LEFT.addLast(now);
        else if (code == mc.options.useKey.getCode()) RIGHT.addLast(now);
    }

    private static int count(Deque<Long> d) {
        long cut = System.currentTimeMillis() - 1000;
        while (!d.isEmpty() && d.peekFirst() < cut) d.pollFirst();
        return d.size();
    }
    public static int leftCps() { return count(LEFT); }
    public static int rightCps() { return count(RIGHT); }

    // ---------------- perspective (freelook) ----------------
    public static boolean freelookActive;
    /** Test w grze (FeatureTest): freelook jak przy trzymanym klawiszu */
    static boolean forceFreelook;
    public static float lookYaw, lookPitch;
    private static int savedPerspective;

    /** Ruch myszy w trakcie freelooka obraca tylko kamerę (te same przeliczniki co Entity.increaseTransforms) */
    public static void look(float dx, float dy) {
        lookYaw += dx * 0.15f;
        lookPitch = Math.max(-90f, Math.min(90f, lookPitch - dy * 0.15f));
    }

    private static boolean down(int key) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return key > 0 && mc.currentScreen == null && Keyboard.isKeyDown(key);
    }

    public static boolean zooming() { return cfg.zoom && down(cfg.zoomKey); }

    private static boolean wasMenu;
    private static int smokeTicks;

    /** Test w CI (-Dgruz.smoketest=true): menu główne -> menu Gruz -> GRUZ_SMOKE_OK i koniec */
    private static void smoke(MinecraftClient mc) {
        if (!Boolean.getBoolean("gruz.smoketest") || mc.currentScreen == null) return;
        smokeTicks++;
        if (smokeTicks == 60) mc.setScreen(new GruzMenuScreen());
        if (smokeTicks == 120) {
            System.out.println("GRUZ_SMOKE_OK"); // stdout – CI szuka tego w logu Gradle
            GruzLegacy.LOG.info("GRUZ_SMOKE_OK");
            mc.scheduleStop();
        }
    }

    /** MinecraftClient.tick (koniec) */
    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        FeatureTest.tick();
        smoke(mc);
        GruzNet.tick();
        Capes.tick();
        boolean menu = down(cfg.menuKey);
        if (menu && !wasMenu) mc.setScreen(new GruzMenuScreen());
        wasMenu = menu;
        if (mc.player == null) { freelookActive = false; return; }

        // freelook: przytrzymaj klawisz – kamera z tyłu krąży wokół postaci
        boolean fl = cfg.freelook && (forceFreelook || down(cfg.freelookKey));
        if (fl && !freelookActive) {
            freelookActive = true;
            lookYaw = mc.player.yaw;
            lookPitch = mc.player.pitch;
            savedPerspective = mc.options.perspective;
            mc.options.perspective = 1;
        } else if (!fl && freelookActive) {
            freelookActive = false;
            mc.options.perspective = savedPerspective;
        }

        // toggle sprint: postać biegnie bez trzymania Ctrl (tylko gdy idzie do przodu)
        if (cfg.toggleSprint && mc.currentScreen == null && mc.options.forwardKey.isPressed() && !mc.player.isSneaking()) {
            KeyBinding.setKeyPressed(mc.options.sprintKey.getCode(), true);
        }

        // fullbright: jasność gry na maksimum, po wyłączeniu wraca poprzednia
        if (cfg.fullbright) {
            if (mc.options.gamma < 100f) { cfg.oldGamma = mc.options.gamma; mc.options.gamma = 100f; }
        } else if (mc.options.gamma >= 100f) {
            mc.options.gamma = Math.min(1f, cfg.oldGamma);
        }
    }
}
