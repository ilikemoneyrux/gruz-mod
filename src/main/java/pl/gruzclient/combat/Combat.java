package pl.gruzclient.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import pl.gruzclient.module.Modules;

/**
 * Wspólny stan walki: ostatni cel, zasięg ostatniego ciosu, combo i kille w sesji.
 * Zasilany przez mixin ClientPlayerInteractionManager.attackEntity i tick klienta.
 */
public final class Combat {
    private Combat() {}

    public static LivingEntity target;
    public static long lastHitAt = 0;
    public static double lastReach = -1;
    public static long lastReachAt = 0;
    public static int combo = 0;
    public static int bestCombo = 0;
    public static int kills = 0;
    public static int deaths = 0;
    // bieżąca walka (zaczyna się od pierwszego ciosu, kończy po 5 s spokoju)
    public static long fightStart = 0, fightLast = 0;
    public static int fightDealt = 0, fightTaken = 0, fightBestCombo = 0;
    public static float fightDamageTaken = 0;
    public static String fightOpponent = "";
    private static float lastHealth = -1;
    private static boolean wasDead = false;
    private static int lastHurtTime = 0;
    private static LivingEntity pendingKill;
    private static long pendingKillAt = 0;

    /** Gracz uderza encję (wywoływane przed wysłaniem pakietu ataku) */
    public static void onAttack(Player player, Entity entity) {
        long now = System.currentTimeMillis();
        Vec3 eye = player.getEyePosition();
        lastReach = Math.sqrt(entity.getBoundingBox().distanceToSqr(eye));
        lastReachAt = now;

        if (entity instanceof LivingEntity living && living.isAlive()) {
            if (target != living || now - lastHitAt > 3000) combo = 0;
            target = living;
            combo++;
            bestCombo = Math.max(bestCombo, combo);
            fightHit(now, living);
            fightDealt++;
            fightBestCombo = Math.max(fightBestCombo, combo);
            lastHitAt = now;
            pendingKill = living;
            pendingKillAt = now;
            Modules.HIT_SOUND.play();
        }
    }

    /** Co tick klienta: reset combo po otrzymaniu obrażeń, wykrywanie killi */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { target = null; return; }
        long now = System.currentTimeMillis();

        int hurt = mc.player.hurtTime;
        if (hurt > lastHurtTime && hurt > 0) {
            combo = 0;
            if (fightStart > 0) { fightTaken++; fightLast = now; }
        }
        float hp = mc.player.getHealth();
        if (lastHealth >= 0 && hp < lastHealth && fightStart > 0) fightDamageTaken += lastHealth - hp;
        lastHealth = hp;
        boolean dead = mc.player.isDeadOrDying();
        if (dead && !wasDead) { deaths++; endFight(true); }
        wasDead = dead;
        if (fightStart > 0 && now - fightLast > 5000) endFight(false);
        lastHurtTime = hurt;
        if (now - lastHitAt > 3000) combo = 0;

        if (pendingKill != null) {
            if (pendingKill.isDeadOrDying() || (pendingKill.isRemoved() && pendingKill.getHealth() <= 0)) {
                kills++;
                pendingKill = null;
                Modules.CLIP.onKill();
                endFight(false);
                Modules.AUTO_GG.onKill();
            } else if (now - pendingKillAt > 2500) {
                pendingKill = null;
            }
        }
        if (target != null && (target.isRemoved() || now - lastHitAt > 6000)) target = null;
    }

    private static void fightHit(long now, LivingEntity living) {
        if (fightStart == 0) {
            fightStart = now;
            fightDealt = fightTaken = fightBestCombo = 0;
            fightDamageTaken = 0;
        }
        fightLast = now;
        fightOpponent = living.getName().getString();
    }

    /** Czy trwa walka (do HUD walki) */
    public static boolean inFight() { return fightStart > 0; }

    public static long fightMillis() { return fightStart == 0 ? 0 : Math.max(fightLast, System.currentTimeMillis()) - fightStart; }

    private static void endFight(boolean died) {
        if (fightStart == 0) return;
        Modules.FIGHT.onFightEnd(died, fightLast - fightStart);
        fightStart = 0;
    }

    /** Nowy świat / serwer – zerujemy stan sesji */
    public static void reset() {
        target = null;
        combo = 0;
        pendingKill = null;
        fightStart = 0;
        lastHealth = -1;
        wasDead = false;
    }
}
