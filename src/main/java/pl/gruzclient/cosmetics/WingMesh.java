package pl.gruzclient.cosmetics;

/**
 * Kształt skrzydeł (od 0.5.12): pióra i błony zamiast klocków. Same trójkąty z kolorem w każdym wierzchołku.
 * TEN SAM algorytm jest w launcherze i na stronie (gruzwings.js) – zmieniając coś tutaj, zmień też tam.
 * Jednostka = piksel modelu (1/16 bloku), x na zewnątrz (prawe skrzydło), y w dół, z do tyłu.
 * Jedno wspólne pole robocze: rysowanie idzie w jednym wątku, więc nie tworzymy tablic co klatkę.
 */
public final class WingMesh {
    private WingMesh() {}

    /** Wynik ostatniego build(): N wierzchołków (po 3 na trójkąt), POS = x,y,z, COL = rgb */
    public static final float[] POS = new float[3 * 3 * 400];
    public static final int[] COL = new int[3 * 400];
    public static int N;

    private static float scale, wristX, cs, sn;
    private static final float DEG = (float) (Math.PI / 180.0);

    private static int mix(int a, int b, float k) {
        k = Math.max(0f, Math.min(1f, k));
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * k);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * k);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * k);
        return (r << 16) | (g << 8) | bl;
    }

    private static float[] lerp(float[] a, float[] b, float k) { return new float[]{a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k}; }

    /** Punkt na łamanej (0–1 wzdłuż całej długości) */
    private static float[] along(float[][] pts, float u) {
        float total = 0;
        float[] len = new float[pts.length - 1];
        for (int i = 0; i < len.length; i++) { len[i] = (float) Math.hypot(pts[i + 1][0] - pts[i][0], pts[i + 1][1] - pts[i][1]); total += len[i]; }
        float d = Math.max(0f, Math.min(1f, u)) * total;
        for (int i = 0; i < len.length; i++) {
            if (d <= len[i] || i == len.length - 1) return lerp(pts[i], pts[i + 1], len[i] > 0 ? Math.min(1f, d / len[i]) : 0f);
            d -= len[i];
        }
        return pts[pts.length - 1];
    }

    private static void put(float[] p, float z, int c) {
        if (N >= COL.length) return;
        float x = p[0] * scale, y = p[1] * scale, zz = z;
        if (x > wristX) { float dx = x - wristX; x = wristX + dx * cs; zz = z + dx * sn; } // część za nadgarstkiem zgina się do tyłu
        POS[N * 3] = x; POS[N * 3 + 1] = y; POS[N * 3 + 2] = zz;
        COL[N++] = c;
    }

    private static void tri(float[] a, float[] b, float[] c, float z, int ca, int cb, int cc) { put(a, z, ca); put(b, z, cb); put(c, z, cc); }

    private static void quad(float[] a, float[] b, float[] c, float[] d, float z, int ca, int cb, int cc, int cd) {
        tri(a, b, c, z, ca, cb, cc);
        tri(a, c, d, z, ca, cc, cd);
    }

    /** Pasek o zmiennej grubości wzdłuż łamanej (kość, przednia krawędź skrzydła) */
    private static void strip(float[][] pts, float t0, float t1, float z, int c0, int c1) {
        int n = pts.length - 1;
        for (int i = 0; i < n; i++) {
            float[] a = pts[i], b = pts[i + 1];
            float dx = b[0] - a[0], dy = b[1] - a[1], l = (float) Math.hypot(dx, dy);
            if (l == 0) l = 1;
            float nx = -dy / l, ny = dx / l;
            float ta = (t0 + (t1 - t0) * (i / (float) n)) / 2f, tb = (t0 + (t1 - t0) * ((i + 1) / (float) n)) / 2f;
            int ca = mix(c0, c1, i / (float) n), cb = mix(c0, c1, (i + 1) / (float) n);
            quad(new float[]{a[0] + nx * ta, a[1] + ny * ta}, new float[]{b[0] + nx * tb, b[1] + ny * tb},
                    new float[]{b[0] - nx * tb, b[1] - ny * tb}, new float[]{a[0] - nx * ta, a[1] - ny * ta},
                    z, ca, cb, mix(cb, 0x000000, 0.25f), mix(ca, 0x000000, 0.25f));
        }
    }

    private static int fc0, fc1, fc2;
    private static boolean crystal;

    private static float[] at(float[] p, float[] d, float[] n, float L, float w, float k, float s) {
        return new float[]{p[0] + d[0] * L * k + n[0] * w * s, p[1] + d[1] * L * k + n[1] * w * s};
    }

    /** Jedno pióro: nasada p, kąt od „w dół” w stronę „na zewnątrz”, długość, szerokość */
    private static void feather(float[] p, float ang, float L, float w, float z, float shade) {
        float[] d = {(float) Math.sin(ang), (float) Math.cos(ang)}, n = {(float) Math.cos(ang), (float) -Math.sin(ang)};
        int a = mix(fc0, 0x000000, shade), b = mix(fc1, 0x000000, shade), c = mix(fc2, 0x000000, shade * 0.6f);
        float sharp = crystal ? 0.12f : 0.3f; // kryształ ma ostre końce
        quad(at(p, d, n, L, w, 0f, -0.3f), at(p, d, n, L, w, 0f, 0.3f), at(p, d, n, L, w, 0.6f, 0.5f), at(p, d, n, L, w, 0.6f, -0.5f), z, a, a, b, b);
        quad(at(p, d, n, L, w, 0.6f, -0.5f), at(p, d, n, L, w, 0.6f, 0.5f), at(p, d, n, L, w, 0.9f, sharp), at(p, d, n, L, w, 0.9f, -sharp), z + L * 0.02f, b, b, c, c);
        tri(at(p, d, n, L, w, 0.9f, -sharp), at(p, d, n, L, w, 0.9f, sharp), at(p, d, n, L, w, 1f, 0f), z + L * 0.035f, c, c, c);
        // stosina (środkowa żyłka) – cienka, ciemniejsza kreska
        int a2 = mix(a, 0x000000, 0.2f), b2 = mix(b, 0x000000, 0.12f);
        quad(at(p, d, n, L, w, 0.05f, -0.05f), at(p, d, n, L, w, 0.05f, 0.05f), at(p, d, n, L, w, 0.88f, 0.03f), at(p, d, n, L, w, 0.88f, -0.03f), z + L * 0.02f + 0.03f, a2, a2, b2, b2);
    }

    /** Liczy siatkę jednego (prawego) skrzydła do POS/COL/N. Zwraca true, gdy skrzydło świeci (kryształ). */
    public static boolean build(String style, int color, float fold) {
        N = 0;
        boolean membrane = "demon".equals(style) || "bat".equals(style);
        scale = "bat".equals(style) ? 0.82f : 1f;
        wristX = (membrane ? 9.5f : 11f) * scale;
        cs = (float) Math.cos(fold);
        sn = (float) Math.sin(fold);

        if (!membrane) {
            // ---------------- skrzydło z piór (anioł, kryształ) ----------------
            crystal = "crystal".equals(style);
            fc0 = crystal ? mix(color, 0x000000, 0.5f) : 0x9D97BD;   // nasada pióra (cień)
            fc1 = crystal ? color : 0xE9E6F4;                         // środek
            fc2 = crystal ? mix(color, 0xFFFFFF, 0.7f) : 0xFFFFFF;    // koniec
            float[] S = {0, 0}, E = {5.5f, -3.6f}, W = {11, -4.2f}, T = {14.5f, -2.2f};
            float[][] arm = {S, E, W, T};
            // lotki pierwszego rzędu: wachlarz od nadgarstka do końca dłoni
            for (int i = 0; i < 8; i++) {
                float k = i / 7f;
                feather(lerp(W, T, k), (34 + k * 52) * DEG, 14 + 2.5f * (float) Math.sin(Math.PI * k) - i * 0.25f, 2.3f, 0.1f + i * 0.012f, (i % 2) * 0.05f);
            }
            // lotki drugiego rzędu: wzdłuż ramienia, w dół
            for (int i = 0; i < 9; i++) {
                float u = 0.08f + 0.92f * (i / 8f) * 0.76f;
                float[] p = along(arm, u);
                feather(new float[]{p[0], p[1] + 0.6f}, (4 + 30 * (i / 8f)) * DEG, 9.5f + 3.5f * (i / 8f), 2.2f, 0.26f + i * 0.012f, (i % 2) * 0.05f);
            }
            // pokrywy: krótsze pióra zachodzące na lotki
            for (int i = 0; i < 11; i++) {
                float k = i / 10f;
                float[] p = along(arm, 0.05f + 0.95f * k);
                feather(new float[]{p[0], p[1] + 0.3f}, (8 + 44 * k) * DEG, 5.4f + 1.4f * (float) Math.sin(Math.PI * k), 2f, 0.5f + i * 0.01f, 0.04f + (i % 2) * 0.04f);
            }
            for (int i = 0; i < 10; i++) {
                float k = i / 9f;
                float[] p = along(arm, 0.03f + 0.94f * k);
                feather(new float[]{p[0], p[1]}, (12 + 46 * k) * DEG, 2.9f + 0.6f * (float) Math.sin(Math.PI * k), 1.7f, 0.72f + i * 0.01f, (i % 2) * 0.03f);
            }
            // przednia krawędź (ramię skrzydła)
            strip(new float[][]{{-0.6f, 0.6f}, S, E, W, T, {15.4f, -1.2f}}, 2.2f, 0.7f, 0.9f,
                    crystal ? mix(color, 0xFFFFFF, 0.35f) : 0xF6F4FB, crystal ? color : 0xD9D5EA);
            return crystal;
        }

        // ---------------- skrzydło z błony (demon, nietoperz) ----------------
        boolean demon = "demon".equals(style);
        int m0 = demon ? 0x22040A : 0x0C0912;   // błona przy kości
        int m1 = demon ? 0x7A0F1C : 0x33185A;   // błona na brzegu
        int edge = demon ? 0xB21C2E : 0x5B2D9A; // sam brzeg
        int bone = demon ? 0x3D0A12 : 0x15101F, boneHi = demon ? 0x8A1826 : 0x3B2466;
        float[] S = {0, 0}, E = {4.5f, -4}, W = {9.5f, -6.5f}, B = {0.8f, 6.5f};
        float[][] F = {{17, -7.5f}, {16.5f, 0.5f}, {13, 6.5f}, {8.5f, 9.5f}};
        for (int i = 0; i < 3; i++) {
            float[] M = lerp(lerp(F[i], F[i + 1], 0.5f), W, 0.24f);
            tri(W, F[i], M, 0, m0, edge, m1);
            tri(W, M, F[i + 1], 0, m0, m1, edge);
        }
        float[] M3 = lerp(lerp(F[3], B, 0.5f), E, 0.26f);
        tri(W, F[3], M3, 0, m0, edge, m1);
        tri(W, M3, E, 0, m0, m1, m0);
        tri(E, M3, B, 0, m0, m1, edge);
        tri(E, B, S, 0, m0, edge, m0);
        // kości: ramię i palce
        strip(new float[][]{{-0.5f, 0.5f}, S, E, W}, 1.7f, 1.1f, 0.3f, boneHi, bone);
        for (int i = 0; i < F.length; i++) strip(new float[][]{W, lerp(W, F[i], 0.5f), F[i]}, 0.85f - i * 0.05f, 0.2f, 0.25f, boneHi, bone);
        // pazur na nadgarstku i szpony na końcach palców
        int claw = demon ? 0x0D0406 : 0x08060C;
        tri(new float[]{W[0] - 0.7f, W[1] - 0.2f}, new float[]{W[0] + 0.9f, W[1] - 0.3f}, new float[]{W[0] + 0.9f, W[1] - 3.4f}, 0.35f, claw, claw, demon ? 0x5A5A5A : 0x3A3050);
        if (demon) for (float[] f : F) {
            float[] d = lerp(W, f, 1.09f);
            tri(new float[]{f[0] - 0.25f, f[1] - 0.25f}, new float[]{f[0] + 0.25f, f[1] + 0.25f}, d, 0.3f, claw, claw, 0x6A6A6A);
        }
        return false;
    }
}
