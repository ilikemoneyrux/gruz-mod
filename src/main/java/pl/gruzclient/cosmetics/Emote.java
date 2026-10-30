package pl.gruzclient.cosmetics;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.math.MathHelper;

/** Emotki: każda to animacja rąk/głowy przez określony czas. Ustawiane po vanillowym setAngles. */
public enum Emote {
    WAVE("wave", 3000),
    CHEER("cheer", 2600),
    CLAP("clap", 2800),
    DANCE("dance", 4800),
    SALUTE("salute", 2600),
    FACEPALM("facepalm", 2800),
    BOW("bow", 2200),
    POINT("point", 2400);

    public final String id;
    public final long durationMs;

    Emote(String id, long durationMs) { this.id = id; this.durationMs = durationMs; }

    public static Emote byId(String id) {
        for (Emote e : values()) if (e.id.equals(id)) return e;
        return null;
    }

    public record Playing(Emote emote, long startedAt) {
        public float progress() { return (System.currentTimeMillis() - startedAt) / (float) emote.durationMs; }
        public boolean finished() { return progress() >= 1f; }
        public float seconds() { return (System.currentTimeMillis() - startedAt) / 1000f; }
    }

    /** Płynne wejście i wyjście z pozy (żeby ręce nie "teleportowały się") */
    private static float blend(float p) {
        float in = MathHelper.clamp(p * 6f, 0f, 1f);
        float out = MathHelper.clamp((1f - p) * 6f, 0f, 1f);
        float k = Math.min(in, out);
        return k * k * (3f - 2f * k);
    }

    private static void set(ModelPart part, float pitch, float yaw, float roll, float k) {
        part.pitch = MathHelper.lerp(k, part.pitch, pitch);
        part.yaw = MathHelper.lerp(k, part.yaw, yaw);
        part.roll = MathHelper.lerp(k, part.roll, roll);
    }

    public void apply(BipedEntityModel<?> m, Playing play) {
        float p = MathHelper.clamp(play.progress(), 0f, 1f);
        float t = play.seconds();
        float k = blend(p);
        switch (this) {
            case WAVE -> set(m.rightArm, 0f, 0f, 2.55f + MathHelper.sin(t * 9f) * 0.38f, k);
            case CHEER -> {
                float b = MathHelper.sin(t * 10f) * 0.12f;
                set(m.rightArm, 0f, 0f, 2.85f + b, k);
                set(m.leftArm, 0f, 0f, -2.85f - b, k);
                set(m.head, -0.25f, m.head.yaw, 0f, k);
            }
            case CLAP -> {
                float c = 0.15f + 0.4f * Math.abs(MathHelper.sin(t * 9f));
                set(m.rightArm, -1.3f, -c, 0f, k);
                set(m.leftArm, -1.3f, c, 0f, k);
            }
            case DANCE -> {
                float s = MathHelper.sin(t * 7f);
                set(m.rightArm, 0f, 0f, 2.1f + s * 0.7f, k);
                set(m.leftArm, 0f, 0f, -2.1f + s * 0.7f, k);
                set(m.head, m.head.pitch, m.head.yaw, s * 0.22f, k);
                set(m.body, 0f, s * 0.18f, 0f, k);
            }
            case SALUTE -> set(m.rightArm, -2.25f, -0.62f, 0f, k);
            case FACEPALM -> {
                set(m.rightArm, -2.15f, -0.45f, 0f, k);
                set(m.head, 0.45f, m.head.yaw, 0f, k);
            }
            case BOW -> {
                set(m.head, 0.75f, m.head.yaw, 0f, k);
                set(m.rightArm, -0.35f, 0f, 0f, k);
                set(m.leftArm, -0.35f, 0f, 0f, k);
            }
            case POINT -> set(m.rightArm, -1.6f + MathHelper.sin(t * 5f) * 0.04f, 0f, 0f, k);
        }
        m.hat.copyTransform(m.head);
    }
}
