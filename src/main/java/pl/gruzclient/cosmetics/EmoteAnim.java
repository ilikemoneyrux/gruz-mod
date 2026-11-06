package pl.gruzclient.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import pl.gruzclient.GruzClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emotki z prawdziwym zginaniem (łokcie, kolana, tułów) przez Player Animation Library.
 *
 * Biblioteka jest OPCJONALNA: launcher Gruz doinstalowuje ją razem z Fabric API (gdy jest dla danej wersji MC).
 * Gdy jej nie ma albo coś pójdzie nie tak, emotki grają jak dawniej (Emote.apply w BipedEntityModelMixin).
 * Animacje: assets/gruzclient/player_animations/*.json (generuje je tools/emotes/build.js).
 */
public final class EmoteAnim {
    private EmoteAnim() {}

    private static boolean ready;
    /** gracz -> startedAt emotki, którą gra biblioteka (wtedy nasza stara animacja się nie nakłada) */
    private static final Map<UUID, Long> VIA_LIB = new ConcurrentHashMap<>();

    public static void init() {
        //#if MC == 1.21.1 || MC >= 1.21.11
        try {
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("player_animation_library")) {
                EmoteAnimPal.init();
                ready = true;
                GruzClient.LOG.info("Emotki: Player Animation Library – zginanie włączone");
            }
        } catch (Throwable t) {
            ready = false;
            GruzClient.LOG.warn("Emotki: nie udało się podpiąć Player Animation Library – zostają zwykłe animacje", t);
        }
        //#endif
    }

    public static boolean ready() { return ready; }

    /** Start emotki: próbujemy przez bibliotekę; jak się nie da, zostaje zwykła animacja z Emote.apply */
    public static void start(UUID uuid, Emote.Playing play) { start(uuid, play, play.emote().id); }

    /** anim = nazwa pliku w player_animations (np. duodance_b dla drugiego gracza w parze) */
    public static void start(UUID uuid, Emote.Playing play, String anim) {
        VIA_LIB.remove(uuid);
        if (!ready) return;
        //#if MC == 1.21.1 || MC >= 1.21.11
        try {
            AbstractClientPlayer p = player(uuid);
            if (p != null && EmoteAnimPal.play(p, anim)) VIA_LIB.put(uuid, play.startedAt());
        } catch (Throwable t) {
            GruzClient.LOG.warn("Emotki: błąd biblioteki animacji – wyłączam ją do restartu gry", t);
            ready = false;
        }
        //#endif
    }

    /** Przerwanie emotki (np. gracz ruszył) */
    public static void stop(UUID uuid) {
        Long was = VIA_LIB.remove(uuid);
        if (was == null || !ready) return;
        //#if MC == 1.21.1 || MC >= 1.21.11
        try {
            AbstractClientPlayer p = player(uuid);
            if (p != null) EmoteAnimPal.stop(p);
        } catch (Throwable t) {
            GruzClient.LOG.warn("Emotki: błąd przy zatrzymaniu animacji", t);
        }
        //#endif
    }

    /** Czy tę emotkę gra biblioteka (wtedy mixin nie nakłada starej animacji) */
    public static boolean handled(UUID uuid, Emote.Playing play) {
        Long s = VIA_LIB.get(uuid);
        return s != null && s == play.startedAt();
    }

    /** Sprzątanie po skończonych emotkach */
    public static void forget(UUID uuid) { VIA_LIB.remove(uuid); }

    private static AbstractClientPlayer player(UUID uuid) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        Player p = level.getPlayerByUUID(uuid);
        return p instanceof AbstractClientPlayer a ? a : null;
    }
}
