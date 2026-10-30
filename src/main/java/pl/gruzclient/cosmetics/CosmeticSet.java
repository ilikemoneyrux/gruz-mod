package pl.gruzclient.cosmetics;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

/**
 * Zestaw kosmetyków jednego gracza. Kody są takie same w modzie, launcherze i na serwerze Gruz
 * ("cape=gruz;head=halo;wings=angel;back=none;trail=none;pet=none;name=rainbow;color=b45cff").
 *
 * Za darmo jest tylko peleryna "gruz". Resztę odblokowuje się kodem (panel admina serwera)
 * albo – kosmetyki sezonowe – odbiera za darmo w czasie wydarzenia (Halloween, Święta).
 */
public record CosmeticSet(String cape, String head, String wings, String back, String trail, String pet, String name, int color) {
    public static final CosmeticSet NONE = new CosmeticSet("none", "none", "none", "none", "none", "none", "none", 0xB45CFF);

    public static final String[] CAPES = {"none", "gruz", "nebula", "sakura", "inferno", "ocean", "flow", "crystals", "galaxy", "aurora", "ember", "pumpkin", "winter"};
    /** Peleryny HD z obrazka (assets/gruzclient/textures/cape/*.png); reszta jest animowana */
    public static final Set<String> HD_CAPES = Set.of("gruz", "nebula", "sakura", "inferno", "ocean", "pumpkin", "winter");
    public static final String[] HEADS = {"none", "halo", "crown", "tophat", "witch", "santa"};
    public static final String[] WINGS = {"none", "angel", "demon", "crystal", "bat"};
    public static final String[] BACKS = {"none", "backpack", "jetpack"};
    public static final String[] TRAILS = {"none", "shards", "sparks", "stars", "witch", "souls", "snow"};
    public static final String[] PETS = {"none", "crystal", "pumpkin"};
    public static final String[] NAMES = {"none", "rainbow", "neon", "gold", "fire", "frost"};

    /** Jedyna darmowa kosmetyka */
    public static final String FREE = "cape:gruz";

    public boolean hasCape() { return !"none".equals(cape); }
    public boolean hasHead() { return !"none".equals(head); }
    public boolean hasWings() { return !"none".equals(wings); }
    public boolean hasBack() { return !"none".equals(back); }
    public boolean hasTrail() { return !"none".equals(trail); }
    public boolean hasPet() { return !"none".equals(pet); }
    public boolean hasName() { return !"none".equals(name); }
    public boolean has3d() { return hasHead() || hasWings() || hasBack() || hasPet(); }

    public String encode() {
        return "cape=" + cape + ";head=" + head + ";wings=" + wings + ";back=" + back + ";trail=" + trail + ";pet=" + pet
                + ";name=" + name + ";color=" + String.format("%06x", color & 0xFFFFFF);
    }

    public static CosmeticSet decode(String s) {
        if (s == null || s.isBlank()) return NONE;
        String cape = "none", head = "none", wings = "none", back = "none", trail = "none", pet = "none", name = "none";
        int color = 0xB45CFF;
        for (String part : s.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            String v = kv[1].trim();
            switch (kv[0].trim()) {
                case "cape" -> cape = pick(v, CAPES);
                case "head" -> head = pick(v, HEADS);
                case "wings" -> wings = pick(v, WINGS);
                case "back" -> back = pick(v, BACKS);
                case "trail" -> trail = pick(v, TRAILS);
                case "pet" -> pet = pick(v, PETS);
                case "name" -> name = pick(v, NAMES);
                case "color" -> { try { color = Integer.parseInt(v.replace("#", ""), 16) & 0xFFFFFF; } catch (NumberFormatException ignored) { } }
                default -> { }
            }
        }
        return new CosmeticSet(cape, head, wings, back, trail, pet, name, color);
    }

    private static String pick(String v, String[] allowed) {
        for (String a : allowed) if (a.equals(v)) return a;
        return "none";
    }

    /**
     * Czy wolno założyć daną kosmetykę. Launcher przekazuje odblokowane jako
     * -Dgruz.unlocked=cape:nebula,wings:angel (albo "all").
     */
    public static boolean unlocked(String slot, String value) {
        if (value == null || "none".equals(value)) return true;
        String key = slot + ":" + value;
        if (FREE.equals(key)) return true;
        for (String u : System.getProperty("gruz.unlocked", "").split(",")) {
            String t = u.trim();
            if (t.equals(key) || t.equals("all")) return true;
        }
        return false;
    }

    /** Ten sam zestaw z wyczyszczonymi zablokowanymi elementami */
    public CosmeticSet onlyUnlocked() {
        return new CosmeticSet(
                unlocked("cape", cape) ? cape : "none", unlocked("head", head) ? head : "none",
                unlocked("wings", wings) ? wings : "none", unlocked("back", back) ? back : "none",
                unlocked("trail", trail) ? trail : "none", unlocked("pet", pet) ? pet : "none",
                unlocked("name", name) ? name : "none", color);
    }

    // ---------------- sezony ----------------
    /** Aktywny sezon ("halloween" / "christmas") albo null */
    public static String season() {
        MonthDay d = MonthDay.from(LocalDate.now());
        if (!d.isBefore(MonthDay.of(10, 15)) && !d.isAfter(MonthDay.of(11, 10))) return "halloween";
        if (!d.isBefore(MonthDay.of(12, 1)) || !d.isAfter(MonthDay.of(1, 6))) return "christmas";
        return null;
    }
}
