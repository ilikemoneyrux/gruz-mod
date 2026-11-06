package pl.gruzclient.cosmetics;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Emotki: animacje całej postaci (ręce, głowa, tułów, nogi) nakładane po vanillowym setupAnim.
 *
 * Od 0.5.3 animacje są płynne: wejście/wyjście z pozy liczone w sekundach (z łagodnym hamowaniem),
 * ruchy mają kilka warstw (główny ruch + opóźnione „dociągnięcie” innych części), tułów może się
 * skręcać i pochylać – wtedy ręce i głowa idą za nim (jak w vanillowym ataku i kucaniu), więc nic
 * nie odrywa się od barków. Pozycje części gra resetuje co klatkę, dlatego przesunięcia są bezpieczne.
 */
public enum Emote {
    WAVE("wave", 3000),
    CHEER("cheer", 2600),
    CLAP("clap", 2800),
    DANCE("dance", 4800),
    SALUTE("salute", 2600),
    FACEPALM("facepalm", 2800),
    BOW("bow", 2400),
    POINT("point", 2400),
    // płatne emotki we dwoje (od 0.5.2): kod emote:<id>, zaproszenie dla gracza obok, start u obu naraz (Duo.java)
    HIGHFIVE("highfive", 2600, true),
    HUG("hug", 3400, true),
    DUODANCE("duodance", 5000, true);

    public final String id;
    public final long durationMs;
    /** emotka we dwoje (płatna, wymaga drugiego gracza) */
    public final boolean duo;

    Emote(String id, long durationMs) { this(id, durationMs, false); }
    Emote(String id, long durationMs, boolean duo) { this.id = id; this.durationMs = durationMs; this.duo = duo; }

    public static Emote byId(String id) {
        for (Emote e : values()) if (e.id.equals(id)) return e;
        return null;
    }

    public record Playing(Emote emote, long startedAt) {
        public float progress() { return (System.currentTimeMillis() - startedAt) / (float) emote.durationMs; }
        public boolean finished() { return progress() >= 1f; }
        public float seconds() { return (System.currentTimeMillis() - startedAt) / 1000f; }
    }

    // ---------------- krzywe ----------------
    private static final float PI = (float) Math.PI;
    private static final float IN = 0.32f;   // sekundy wejścia w pozę
    private static final float OUT = 0.42f;  // sekundy powrotu do zwykłej pozy

    private static float clamp01(float x) { return Mth.clamp(x, 0f, 1f); }
    /** Gładki start i koniec (smootherstep) */
    private static float smooth(float x) { x = clamp01(x); return x * x * x * (x * (x * 6f - 15f) + 10f); }
    /** Szybki ruch z lekkim przestrzeleniem i powrotem – „sprężyste” dojście do pozy */
    private static float back(float x) {
        x = clamp01(x);
        float c1 = 1.4f, c3 = c1 + 1f, y = x - 1f;
        return 1f + c3 * y * y * y + c1 * y * y;
    }
    private static float sin(float a) { return Mth.sin(a); }
    private static float cos(float a) { return Mth.cos(a); }
    /** 0..1 w rytmie (fala bez ostrych końców) */
    private static float pulse(float a) { return 0.5f - 0.5f * cos(a); }

    /** Waga emotki w danej chwili: łagodne wejście i wyjście (czasy w sekundach, niezależne od długości) */
    private static float envelope(float t, float d) { return Math.min(smooth(t / IN), smooth((d - t) / OUT)); }

    // ---------------- ustawianie części ----------------
    /** Obrót części w stronę pozy (k = 0 – vanilla, 1 – poza emotki) */
    private static void rot(ModelPart part, float pitch, float yaw, float roll, float k) {
        part.xRot = Mth.lerp(k, part.xRot, pitch);
        part.yRot = Mth.lerp(k, part.yRot, yaw);
        part.zRot = Mth.lerp(k, part.zRot, roll);
    }
    /** Przesunięcie części (w pikselach modelu, y w dół) */
    private static void move(ModelPart part, float dx, float dy, float dz, float k) {
        part.x += dx * k;
        part.y += dy * k;
        part.z += dz * k;
    }
    /** Przesunięcie całej górnej części ciała (tułów, głowa, ręce) – np. podskok, ugięcie */
    private static void lift(HumanoidModel<?> m, float dy, boolean legsToo) {
        m.body.y += dy; m.head.y += dy; m.rightArm.y += dy; m.leftArm.y += dy;
        if (legsToo) { m.rightLeg.y += dy; m.leftLeg.y += dy; }
    }
    /**
     * Skręt tułowia (yaw, radiany). Ręce przesuwają się razem z barkami i obracają z tułowiem,
     * dokładnie tak jak w vanillowym machnięciu ręką.
     */
    private static void twist(HumanoidModel<?> m, float yaw) {
        if (Math.abs(yaw) < 1e-4f) return;
        m.body.yRot += yaw;
        float by = m.body.yRot;
        m.rightArm.z = sin(by) * 5f;
        m.rightArm.x = -cos(by) * 5f;
        m.leftArm.z = -sin(by) * 5f;
        m.leftArm.x = cos(by) * 5f;
        m.rightArm.yRot += yaw;
        m.leftArm.yRot += yaw;
        m.head.yRot += yaw * 0.6f;
    }
    /**
     * Pochylenie do przodu (a w radianach, 0.5 = jak przy kucaniu). Głowa, ręce i nogi przesuwają się
     * tak jak w vanillowym kucaniu, więc postać się nie „łamie”.
     */
    private static void bend(HumanoidModel<?> m, float a) {
        if (a <= 1e-4f) return;
        float s = a / 0.5f;
        m.body.xRot += a;
        m.body.y += 3.2f * s;
        m.head.y += 4.2f * s;
        m.rightArm.y += 3.2f * s;
        m.leftArm.y += 3.2f * s;
        m.rightArm.xRot += 0.4f * s;
        m.leftArm.xRot += 0.4f * s;
        m.rightLeg.z += 3.9f * s;
        m.leftLeg.z += 3.9f * s;
        m.rightLeg.y += 0.2f * s;
        m.leftLeg.y += 0.2f * s;
    }

    public void apply(HumanoidModel<?> m, Playing play) {
        float d = durationMs / 1000f;
        float t = Mth.clamp(play.seconds(), 0f, d);
        float k = envelope(t, d);
        if (k <= 0f) return;
        switch (this) {
            case WAVE -> {
                // ręka unosi się bokiem, macha z nadgarstka (dwie fale z przesunięciem), głowa patrzy na dłoń
                float amp = smooth((t - 0.22f) / 0.3f) * smooth((d - t - 0.15f) / 0.4f);
                float w = sin(t * 10.5f), w2 = sin(t * 10.5f - 1.1f);
                rot(m.rightArm, -0.28f + w2 * 0.07f * amp, -0.12f + w2 * 0.14f * amp, 2.5f + w * 0.42f * amp, k);
                rot(m.leftArm, 0.06f + sin(t * 2.2f) * 0.04f, 0f, -0.1f, k);
                rot(m.head, -0.08f, -0.22f, 0.1f + w * 0.035f * amp, k);
                twist(m, (-0.08f + w2 * 0.035f * amp) * k);
            }
            case CHEER -> {
                // ręce w górę w kształt V, podskoki w rytmie (cała postać odrywa się od ziemi)
                float b = t * 2f * PI * 1.55f;
                float hop = pulse(b);
                float sq = hop * hop;
                float lean = -0.12f - 0.1f * hop;
                rot(m.rightArm, lean, -0.15f, 2.45f + 0.4f * sq, k);
                rot(m.leftArm, lean, 0.15f, -2.45f - 0.4f * sq, k);
                rot(m.head, -0.32f - 0.12f * hop, 0f, sin(b * 0.5f) * 0.08f, k);
                rot(m.rightLeg, -0.1f * sq, 0f, 0.05f * sq, k);
                rot(m.leftLeg, -0.1f * sq, 0f, -0.05f * sq, k);
                lift(m, -1.6f * sq * k, true);
            }
            case CLAP -> {
                // dłonie schodzą się szybko i rozchodzą wolniej; głowa i tułów lekko kiwają w rytmie
                float ph = t * 2f * PI * 2.3f;
                float open = (float) Math.pow(pulse(ph), 0.65);
                float closed = 1f - open;
                float yaw = 0.14f + 0.42f * open;
                rot(m.rightArm, -1.12f + 0.06f * closed, -yaw, -0.06f, k);
                rot(m.leftArm, -1.12f + 0.06f * closed, yaw, 0.06f, k);
                rot(m.head, 0.04f + 0.07f * closed, m.head.yRot, 0f, k);
                lift(m, 0.35f * closed * k, false);
            }
            case DANCE -> {
                // krok w bok co takt, skręt bioder, ręce na zmianę w górę i w dół, kiwanie głową i ugięcia
                float beat = t * 2f * PI * 1.9f;
                float step = sin(beat * 0.5f);            // lewo / prawo co dwa takty
                float bounce = pulse(beat);               // ugięcie na każdy takt
                float late = sin(beat * 0.5f - 0.6f);     // ręce lekko spóźnione za biodrami
                rot(m.rightArm, -0.95f - 0.95f * late, -0.1f, 0.35f + 0.3f * bounce, k);
                rot(m.leftArm, -0.95f + 0.95f * late, 0.1f, -0.35f - 0.3f * bounce, k);
                rot(m.rightLeg, 0.32f * step, 0f, 0.06f + 0.05f * bounce, k);
                rot(m.leftLeg, -0.32f * step, 0f, -0.06f - 0.05f * bounce, k);
                rot(m.head, -0.04f + 0.14f * bounce, m.head.yRot, step * 0.14f, k);
                twist(m, step * 0.28f * k);
                lift(m, 0.7f * bounce * k, false);
            }
            case SALUTE -> {
                // energiczne podniesienie ręki do skroni z lekkim „sprężynowaniem”, broda do góry
                float reach = back(t / 0.42f) * smooth((d - t) / OUT);
                rot(m.rightArm, -2.4f, -0.4f, 0.12f, reach);
                rot(m.leftArm, 0.02f, 0f, -0.04f, k);
                rot(m.head, -0.14f, m.head.yRot * (1f - k), 0f, k);
                move(m.rightArm, 0f, -0.2f, 0f, reach);
            }
            case FACEPALM -> {
                // ręka do twarzy, chwilę później głowa opada i kręci się z rezygnacją
                float reach = back(t / 0.4f) * smooth((d - t) / OUT);
                float drop = smooth((t - 0.28f) / 0.42f) * smooth((d - t) / OUT);
                rot(m.rightArm, -2.05f, -0.52f, 0.1f, reach);
                rot(m.leftArm, -0.18f, 0.05f, -0.05f, k);
                rot(m.head, 0.42f * drop, sin(t * 5.2f) * 0.13f * drop, 0f, k);
                bend(m, 0.14f * drop);
            }
            case BOW -> {
                // ukłon: ręka na piersi, druga za plecami, tułów pochyla się płynnie i wraca
                float down = smooth((t - 0.15f) / 0.55f) * smooth((d - t - 0.1f) / 0.6f);
                rot(m.rightArm, -1.05f, -0.62f, 0.1f, k);
                rot(m.leftArm, 0.5f, 0.25f, -0.1f, k);
                bend(m, 0.62f * down);
                m.head.xRot += 0.3f * down;
            }
            case POINT -> {
                // szybkie wyciągnięcie ręki z lekkim odbiciem, tułów i głowa za ręką
                float reach = back(t / 0.34f) * smooth((d - t) / OUT);
                float idle = sin(t * 3.1f) * 0.035f;
                rot(m.rightArm, -1.58f + idle, -0.06f, 0f, reach);
                rot(m.leftArm, 0.14f, 0f, -0.1f, k);
                rot(m.head, -0.04f + idle * 0.5f, -0.12f, 0f, k);
                twist(m, -0.14f * reach);
            }
            // zapas bez biblioteki animacji (prawdziwe animacje we dwoje są w player_animations/*.json)
            case HIGHFIVE -> {
                float up = back(t / 0.45f) * smooth((d - t) / OUT);
                float slap = smooth((t - 0.75f) / 0.12f) * smooth((1.15f - t) / 0.25f);
                rot(m.rightArm, -2.5f + 0.25f * slap, -0.25f, 0.25f, up);
                rot(m.head, -0.12f, m.head.yRot, 0f, k);
                bend(m, 0.08f * slap);
            }
            case HUG -> {
                float wrap = smooth((t - 0.1f) / 0.5f) * smooth((d - t) / OUT);
                rot(m.rightArm, -1.45f, -0.55f, 0.1f, wrap);
                rot(m.leftArm, -1.45f, 0.55f, -0.1f, wrap);
                rot(m.head, 0.15f, -0.25f, 0.12f * sin(t * 2.4f), k);
                bend(m, 0.18f * wrap);
            }
            case DUODANCE -> DANCE.apply(m, play);
        }
        //#if MC < 1.21.2
        m.hat.copyFrom(m.head); // od 1.21.2 czapka jest dzieckiem głowy i idzie za nią sama
        //#endif
    }
}
