package pl.gruzclient.module.impl;

import pl.gruzclient.compat.Compat;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import pl.gruzclient.GruzClient;
import pl.gruzclient.cosmetics.CapeTextures;
import pl.gruzclient.cosmetics.CosmeticSet;
import pl.gruzclient.cosmetics.Cosmetics;
import pl.gruzclient.cosmetics.Emote;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.Setting;
import pl.gruzclient.network.GruzNetwork;
import pl.gruzclient.render.GruzDraw;
import pl.gruzclient.screen.EmoteWheelScreen;

import java.util.Random;
import net.minecraft.client.CameraType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Kosmetyki Gruz Client: peleryna (HD albo animowana), nakrycie głowy, skrzydła, plecak, ślad cząsteczek,
 * pet, efekt nicku i emotki.
 * Wybór robisz w Szafie w launcherze (albo tutaj). Inni gracze Gruz widzą je przez serwer Gruz.
 */
public class CosmeticsModule extends Module {
    public final Setting.Choice cape = add(new Setting.Choice("capeId", "setting.cape", 1, keys("cape", CosmeticSet.CAPES)));
    public final Setting.Choice head = add(new Setting.Choice("headId", "setting.headwear", 0, keys("head", CosmeticSet.HEADS)));
    public final Setting.Choice wings = add(new Setting.Choice("wingsId", "setting.wings", 0, keys("wings", CosmeticSet.WINGS)));
    public final Setting.Choice back = add(new Setting.Choice("backId", "setting.back", 0, keys("back", CosmeticSet.BACKS)));
    public final Setting.Choice trail = add(new Setting.Choice("trailId", "setting.trail", 0, keys("trail", CosmeticSet.TRAILS)));
    public final Setting.Choice pet = add(new Setting.Choice("petId", "setting.pet", 0, keys("pet", CosmeticSet.PETS)));
    public final Setting.Choice name = add(new Setting.Choice("nameId", "setting.nameEffect", 0, keys("name", CosmeticSet.NAMES)));

    /** "none" -> cos.none, reszta -> slot.wartość (klucze tłumaczeń) */
    private static String[] keys(String slot, String[] values) {
        String[] out = new String[values.length];
        for (int i = 0; i < values.length; i++) out[i] = "none".equals(values[i]) ? "cos.none" : slot + "." + values[i];
        return out;
    }
    public final Setting.Bool showOthers = add(new Setting.Bool("others", "setting.showOthers", true));
    public final Setting.Key emoteKey = add(new Setting.Key("emoteKey", "setting.emoteKey", GLFW.GLFW_KEY_B));
    public final Setting.Bool emoteCamera = add(new Setting.Bool("emoteCam", "setting.emoteCamera", true));

    private final Random random = new Random();
    private boolean wasDown;
    private CameraType beforeEmote;
    private CosmeticSet cached;
    private String cachedKey = "";

    public CosmeticsModule() { super("cosmetics", Category.COSMETICS, true); }

    /** Wybór z launchera (-Dgruz.cosmetics=cape=flow;head=halo;…) nadpisuje ustawienia przy starcie gry */
    public void applyLauncherChoice() {
        String prop = System.getProperty("gruz.cosmetics");
        if (prop == null || prop.isBlank()) return;
        CosmeticSet s = CosmeticSet.decode(prop);
        cape.index = indexOf(CosmeticSet.CAPES, s.cape());
        head.index = indexOf(CosmeticSet.HEADS, s.head());
        wings.index = indexOf(CosmeticSet.WINGS, s.wings());
        back.index = indexOf(CosmeticSet.BACKS, s.back());
        trail.index = indexOf(CosmeticSet.TRAILS, s.trail());
        pet.index = indexOf(CosmeticSet.PETS, s.pet());
        name.index = indexOf(CosmeticSet.NAMES, s.name());
        GruzClient.LOG.info("Kosmetyki z launchera: {}", prop);
    }

    private static int indexOf(String[] arr, String v) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
        return 0;
    }

    public CosmeticSet current() {
        String key = cape.index + "," + head.index + "," + wings.index + "," + back.index + "," + trail.index + "," + pet.index + "," + name.index;
        if (cached == null || !key.equals(cachedKey)) {
            // zablokowane (bez kodu) po prostu się nie pokazują
            cached = new CosmeticSet(CosmeticSet.CAPES[cape.index], CosmeticSet.HEADS[head.index], CosmeticSet.WINGS[wings.index],
                    CosmeticSet.BACKS[back.index], CosmeticSet.TRAILS[trail.index], CosmeticSet.PETS[pet.index],
                    CosmeticSet.NAMES[name.index], GruzDraw.NEON).onlyUnlocked();
            cachedKey = key;
            GruzNetwork.publishCosmetics(cached);
        }
        return cached;
    }

    // ---------------- emotki ----------------

    public void playEmote(Emote e) {
        if (mc.player == null) return;
        Cosmetics.EMOTES.put(mc.player.getUUID(), new Emote.Playing(e, System.currentTimeMillis()));
        if (emoteCamera.value && beforeEmote == null) {
            beforeEmote = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
        GruzNetwork.publishEmote(e);
    }

    private void endEmote() {
        if (mc.player != null) Cosmetics.EMOTES.remove(mc.player.getUUID());
        if (beforeEmote != null) {
            mc.options.setCameraType(beforeEmote);
            beforeEmote = null;
        }
    }

    @Override
    public void tick() {
        CapeTextures.tick();
        if (mc.player == null) return;

        // klawisz emotek otwiera koło wyboru
        boolean down = Modules.keyDown(emoteKey.code);
        if (down && !wasDown) mc.setScreen(new EmoteWheelScreen(this));
        wasDown = down;

        // emotka kończy się po czasie albo gdy zaczniesz iść
        Emote.Playing mine = Cosmetics.EMOTES.get(mc.player.getUUID());
        if (mine != null) {
            boolean moving = mc.player.input != null && (mc.player.input.forwardImpulse != 0 || mc.player.input.leftImpulse != 0 || 
                    //#if MC >= 1.21.2
                    mc.player.input.keyPresses.jump()
                    //#else
                    mc.player.input.jumping
                    //#endif
                    );
            if (mine.finished() || (moving && mine.progress() > 0.1f)) endEmote();
        }
        Cosmetics.EMOTES.entrySet().removeIf(en -> en.getValue().finished() && !en.getKey().equals(mc.player.getUUID()));

        // ślady cząsteczek: Twoje i innych graczy Gruz w pobliżu
        if (mc.level != null) {
            for (Player p : mc.level.players()) {
                CosmeticSet set = Cosmetics.of(p);
                if (set.hasTrail()) spawnTrail(p, set);
            }
        }
    }

    private void spawnTrail(Player p, CosmeticSet set) {
        Vec3 vel = p.getDeltaMovement();
        double speed = vel.horizontalDistance();
        if (speed < 0.03 && random.nextFloat() > 0.15f) return; // stojąc: tylko czasem
        if (p == mc.player && mc.options.getCameraType().isFirstPerson() && random.nextFloat() > 0.5f) return;
        int n = speed > 0.15 ? 2 : 1;
        for (int i = 0; i < n; i++) {
            double x = p.getX() - vel.x * 2 + (random.nextDouble() - 0.5) * 0.4;
            double y = p.getY() + 0.1 + random.nextDouble() * 0.3;
            double z = p.getZ() - vel.z * 2 + (random.nextDouble() - 0.5) * 0.4;
            ParticleOptions effect = switch (set.trail()) {
                case "sparks" -> ParticleTypes.ELECTRIC_SPARK;
                case "stars" -> ParticleTypes.END_ROD;
                case "witch" -> ParticleTypes.WITCH;
                case "souls" -> ParticleTypes.SOUL;
                case "snow" -> ParticleTypes.SNOWFLAKE;
                default -> {
                    int c = random.nextFloat() < 0.3f ? GruzDraw.NEON_HOT : set.color();
                    yield Compat.dust(c, 0.9f);
                }
            };
            double vy = "stars".equals(set.trail()) ? 0.02 : 0.0;
            mc.level.addParticle(effect, x, y, z, 0, vy, 0);
        }
    }

    @Override
    protected void onDisable() { endEmote(); }
}
