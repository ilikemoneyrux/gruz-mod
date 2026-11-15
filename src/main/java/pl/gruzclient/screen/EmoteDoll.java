package pl.gruzclient.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphics;
import pl.gruzclient.GruzClient;
import pl.gruzclient.cosmetics.Emote;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pl.gruzclient.render.GruzDraw.*;

/**
 * Ludzik widziany z przodu w pozie emotki (koło emotek). Pozy biorą się z tych samych plików player_animations,
 * które gra Player Animation Library, więc ludzik robi dokładnie to, co postać w grze.
 * Model jak w Minecrafcie: jednostki modelu, y w dół, przód postaci = -z, obrót części Z·Y·X, kolano/łokieć = bend.
 */
final class EmoteDoll {
    private EmoteDoll() {}

    private static final String[] BONES = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
    private static final String[] AXES = {"pitch", "yaw", "roll", "bend"};
    /** ticks[i] -> v[i][kość][oś] w radianach; stop = koniec powrotu do zwykłej pozy */
    private record Anim(int[] ticks, float[][][] v, int begin, int end, int stop) {}
    private static final Map<String, Anim> CACHE = new HashMap<>();

    private static Anim anim(Emote e) {
        if (!CACHE.containsKey(e.id)) CACHE.put(e.id, load(e.id));
        return CACHE.get(e.id);
    }

    private static Anim load(String id) {
        for (String f : new String[]{id, id + "_a"}) { // emotki we dwoje mają plik na osobę (duodance_a/_b)
            try (InputStream in = EmoteDoll.class.getResourceAsStream("/assets/gruzclient/player_animations/" + f + ".json")) {
                if (in == null) continue;
                JsonObject em = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("emote");
                boolean deg = !em.has("degrees") || em.get("degrees").getAsBoolean();
                JsonArray moves = em.getAsJsonArray("moves");
                int[] ticks = new int[moves.size()];
                float[][][] v = new float[moves.size()][BONES.length][AXES.length];
                for (int i = 0; i < moves.size(); i++) {
                    JsonObject m = moves.get(i).getAsJsonObject();
                    ticks[i] = m.get("tick").getAsInt();
                    for (int b = 0; b < BONES.length; b++) {
                        if (!m.has(BONES[b])) continue;
                        JsonObject bone = m.getAsJsonObject(BONES[b]);
                        for (int a = 0; a < AXES.length; a++) {
                            JsonElement x = bone.get(AXES[a]);
                            if (x == null) continue;
                            // bend jest zawsze w radianach (tak czyta go biblioteka), reszta w stopniach
                            v[i][b][a] = deg && a < 3 ? (float) Math.toRadians(x.getAsFloat()) : x.getAsFloat();
                        }
                    }
                }
                if (ticks.length == 0) return null;
                return new Anim(ticks, v, em.get("beginTick").getAsInt(), em.get("endTick").getAsInt(), em.get("stopTick").getAsInt());
            } catch (Exception ex) {
                GruzClient.LOG.debug("Koło emotek: nie wczytano pozy {}", f, ex);
            }
        }
        return null;
    }

    /** Wartości w chwili tick: przed pierwszą klatką i po ostatniej łagodne przejście z/do zwykłej pozy */
    private static float[][] sample(Anim a, float tick) {
        int n = a.ticks.length;
        float[][] zero = new float[BONES.length][AXES.length];
        float[][] from, to;
        float t0, t1;
        if (tick <= a.ticks[0]) { from = zero; to = a.v[0]; t0 = 0; t1 = a.ticks[0]; }
        else if (tick >= a.ticks[n - 1]) { from = a.v[n - 1]; to = zero; t0 = a.ticks[n - 1]; t1 = Math.max(t0 + 1, a.stop); }
        else {
            int i = 0;
            while (a.ticks[i + 1] <= tick) i++;
            from = a.v[i]; to = a.v[i + 1]; t0 = a.ticks[i]; t1 = a.ticks[i + 1];
        }
        float k = clamp01((tick - t0) / Math.max(1e-3f, t1 - t0));
        float[][] out = new float[BONES.length][AXES.length];
        for (int b = 0; b < BONES.length; b++) for (int x = 0; x < AXES.length; x++) out[b][x] = from[b][x] + (to[b][x] - from[b][x]) * k;
        return out;
    }

    // ---------------- macierze 3x3 (wiersze) ----------------
    /** Rz·Ry·Rx – kolejność obrotów części modelu w Minecrafcie */
    private static float[] rot(float x, float y, float z) {
        float cx = (float) Math.cos(x), sx = (float) Math.sin(x), cy = (float) Math.cos(y), sy = (float) Math.sin(y),
              cz = (float) Math.cos(z), sz = (float) Math.sin(z);
        float[] rx = {1, 0, 0, 0, cx, -sx, 0, sx, cx};
        float[] ry = {cy, 0, sy, 0, 1, 0, -sy, 0, cy};
        float[] rz = {cz, -sz, 0, sz, cz, 0, 0, 0, 1};
        return mul(rz, mul(ry, rx));
    }

    private static float[] mul(float[] a, float[] b) {
        float[] o = new float[9];
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) o[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c];
        return o;
    }

    private static float[] ap(float[] m, float x, float y, float z) {
        return new float[]{m[0] * x + m[1] * y + m[2] * z, m[3] * x + m[4] * y + m[5] * z, m[6] * x + m[7] * y + m[8] * z};
    }

    private static float[] add(float[] p, float[] d, float k) { return new float[]{p[0] + d[0] * k, p[1] + d[1] * k, p[2] + d[2] * k}; }

    private record Seg(float[] a, float[] b, float width, int rgb, float bias) {
        float z() { return (a[2] + b[2]) / 2 + bias; }
    }

    /** Ludzik o środku (x, y), s px na jednostkę modelu (cała postać ma 32 jednostki wysokości); playing = animacja w pętli, inaczej poza ze środka */
    static void draw(GuiGraphics c, Emote e, float x, float y, float s, boolean playing, int tint, float alpha) {
        Anim a = anim(e);
        float tick;
        if (a == null) tick = 0;
        else if (playing) tick = (System.currentTimeMillis() / 50f) % (a.stop + 14); // po każdym razie chwila przerwy
        else tick = (a.begin + a.end) / 2f;
        float[][] p = a == null ? new float[BONES.length][AXES.length] : sample(a, tick);

        float[] hip = {0, 12, 0};
        float[] t = rot(p[1][0] + p[1][3], p[1][1], p[1][2]); // tułów (z ugięciem) obraca całą górę wokół bioder
        List<Seg> segs = new ArrayList<>();
        int body = mix(0x8C8499, tint, 0.35f), limb = mix(0xA8A1B6, tint, 0.35f), head = mix(0xD2CCDC, tint, 0.35f), leg = mix(0x787185, tint, 0.35f);

        float[] neck = add(hip, ap(t, 0, -12, 0), 1);
        segs.add(new Seg(hip, neck, 8 * xScale(t), body, 0.6f));
        float[] h = mul(t, rot(p[0][0], p[0][1], p[0][2]));
        segs.add(new Seg(neck, add(neck, ap(h, 0, -8, 0), 1), 8 * xScale(h), head, -0.6f));
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? -1 : 1; // prawa ręka/noga postaci po lewej stronie obrazka (patrzy na nas)
            float[] ar = p[2 + side], lg = p[4 + side];
            float[] m = mul(t, rot(ar[0], ar[1], ar[2]));
            float[] up = ap(m, 0, 1, 0), lo = ap(mul(m, rot(ar[3], 0, 0)), 0, 1, 0);
            float[] sh = add(hip, ap(t, 5 * sx, -10, 0), 1);
            float[] elbow = add(sh, up, 4);
            segs.add(new Seg(add(sh, up, -2), elbow, 4, limb, 0));
            segs.add(new Seg(elbow, add(elbow, lo, 6), 4, limb, 0));
            float[] l = rot(lg[0], lg[1], lg[2]);
            float[] lu = ap(l, 0, 1, 0), ll = ap(mul(l, rot(lg[3], 0, 0)), 0, 1, 0);
            float[] top = {1.9f * sx, 12, 0}, knee = add(top, lu, 6);
            segs.add(new Seg(top, knee, 4, leg, 0.3f));
            segs.add(new Seg(knee, add(knee, ll, 6), 4, leg, 0.3f));
        }
        // widok 3/4 lekko z góry: ręka wyciągnięta do przodu nie znika jak przy patrzeniu na wprost
        float[] view = rot(-0.18f, 0.5f, 0);
        segs.replaceAll(g -> new Seg(ap(view, g.a[0], g.a[1], g.a[2]), ap(view, g.b[0], g.b[1], g.b[2]), g.width, g.rgb, g.bias));
        segs.sort((u, v) -> Float.compare(v.z(), u.z())); // najdalsze pierwsze (przód postaci = -z)
        for (Seg g : segs) seg(c, x, y - 8 * s, s, g, alpha); // środek postaci 8 jednostek pod szyją
    }

    /** Szerokość części widzianej z przodu: rzut jej osi x */
    private static float xScale(float[] m) { return Math.max(0.45f, (float) Math.hypot(m[0], m[3])); }

    private static void seg(GuiGraphics c, float ox, float oy, float s, Seg g, float alpha) {
        float ax = ox + g.a[0] * s, ay = oy + g.a[1] * s, dx = (g.b[0] - g.a[0]) * s, dy = (g.b[1] - g.a[1]) * s;
        float len = Math.max((float) Math.hypot(dx, dy), 2 * s); // część skierowana na nas: widać choć jej koniec
        // bliżej = jaśniej
        int rgb = mix(g.rgb, 0x000000, clamp01(0.25f + g.z() * 0.05f) * 0.5f);
        var m = pl.gruzclient.compat.Pose2D.of(c);
        m.push();
        m.translate(ax, ay);
        m.rotate((float) Math.atan2(-dx, dy));
        m.scale(g.width * s / 2f, len);
        c.fill(-1, 0, 1, 1, argb(alpha, rgb));
        m.pop();
    }
}
