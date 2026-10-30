package pl.gruzclient.cosmetics;

import pl.gruzclient.module.Modules;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Kto ma jakie kosmetyki i emotki. Twoje biorą się z modułu Kosmetyki,
 * innych graczy Gruz Client – z serwera Gruz (GruzNetwork wypełnia mapę REMOTE).
 */
public final class Cosmetics {
    private Cosmetics() {}

    /** Kosmetyki innych graczy Gruz Client (uuid -> zestaw) */
    public static final Map<UUID, CosmeticSet> REMOTE = new ConcurrentHashMap<>();
    /** Aktywne emotki: uuid -> [id emotki, czas startu] */
    public static final Map<UUID, Emote.Playing> EMOTES = new ConcurrentHashMap<>();

    public static CosmeticSet of(Player player) {
        if (player == null) return CosmeticSet.NONE;
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player || player.getUUID().equals(mc.getUser().getProfileId())) {
            return Modules.COSMETICS.isEnabled() ? Modules.COSMETICS.current() : CosmeticSet.NONE;
        }
        if (!Modules.COSMETICS.showOthers.value) return CosmeticSet.NONE;
        return REMOTE.getOrDefault(player.getUUID(), CosmeticSet.NONE);
    }

    /** Kosmetyki gracza po UUID (np. dla listy graczy, gdzie nie ma encji) */
    public static CosmeticSet ofUuid(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (uuid.equals(mc.getUser().getProfileId())) return Modules.COSMETICS.isEnabled() ? Modules.COSMETICS.current() : CosmeticSet.NONE;
        if (!Modules.COSMETICS.showOthers.value) return CosmeticSet.NONE;
        return REMOTE.getOrDefault(uuid, CosmeticSet.NONE);
    }

    /** Czy gracz używa Gruz Client (ikonka przy nicku) */
    public static boolean isGruzUser(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        return REMOTE.containsKey(uuid) || uuid.equals(mc.getUser().getProfileId());
    }
}
