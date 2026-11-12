package pl.gruzclient.cosmetics;

/**
 * Kształt nakryć głowy (od 0.5.12): okrągłe bryły zamiast klocków. Same trójkąty z kolorem w każdym wierzchołku.
 * TEN SAM algorytm jest w launcherze i na stronie (gruzhats.js) – zmieniając coś tutaj, zmień też tam.
 * Jednostka = piksel modelu (1/16 bloku), środek = oś głowy, y w dół (czubek głowy to y = -8), z do tyłu.
 * Także plecy i pety (backpack, jetpack, flame, petcrystal, petpumpkin): plecy w układzie tułowia
 * (y = 0 szyja … 12 pas, plecy to z = 2), pety wokół własnego środka.
 * Jedno wspólne pole robocze: rysowanie idzie w jednym wątku.
 */
public final class HatMesh {
    private HatMesh() {}

    /** Wynik ostatniego build(): N wierzchołków (po 3 na trójkąt), POS = x,y,z, COL = rgb */
    public static final float[] POS = new float[3 * 3 * 700];
    public static final int[] COL = new int[3 * 700];
    public static int N;
    private static final int SEG = 16;
    /** Kształt przekroju brył obrotowych: PW < 1 = zaokrąglony kwadrat, SZ = spłaszczenie w osi z, RIB = żebra (dynia) */
    private static float PW = 1f, SZ = 1f, RIB = 0f;

    private static int mix(int a, int b, float k) {
        k = Math.max(0f, Math.min(1f, k));
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * k);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * k);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * k);
        return (r << 16) | (g << 8) | bl;
    }

    private static void put(float x, float y, float z, int c) {
        if (N >= COL.length) return;
        POS[N * 3] = x; POS[N * 3 + 1] = y; POS[N * 3 + 2] = z;
        COL[N++] = c;
    }

    private static void tri(float[] a, float[] b, float[] c, int ca, int cb, int cc) {
        put(a[0], a[1], a[2], ca); put(b[0], b[1], b[2], cb); put(c[0], c[1], c[2], cc);
    }

    /** Kolor ściany zależny od kąta: jaśniej z przodu-lewej, ciemniej z tyłu (udaje światło, gra nie cieniuje sama) */
    private static int lit(int c, float ang) {
        return mix(mix(c, 0x000000, 0.3f), mix(c, 0xFFFFFF, 0.12f), 0.5f + 0.5f * (float) Math.cos(ang - 2.2f));
    }

    private static float[] ringPoint(float[] r, float t) {
        float k = r[0] * (1f - RIB * (float) Math.abs(Math.cos(t * 4)));
        return new float[]{(r.length > 3 ? r[3] : 0f) + pwr(Math.cos(t)) * k, r[1], (r.length > 4 ? r[4] : 0f) + pwr(Math.sin(t)) * k * SZ};
    }

    private static float pwr(double v) { return (float) (Math.signum(v) * Math.pow(Math.abs(v), PW)); }

    /** Te same pierścienie ze środkiem przesuniętym na (x, z) */
    private static float[][] at(float[][] rings, float x, float z) {
        float[][] out = new float[rings.length][];
        for (int i = 0; i < rings.length; i++) out[i] = new float[]{rings[i][0], rings[i][1], rings[i][2], x, z};
        return out;
    }

    private static void quad(float[] a, float[] b, float[] c, float[] d, int cl) { tri(a, b, c, cl, cl, cl); tri(a, c, d, cl, cl, cl); }

    /**
     * Bryła obrotowa: pierścienie {promień, y, kolor, przesunięcie x, przesunięcie z} łączone ścianami.
     * Przesunięcia środka pozwalają wygiąć bryłę (czubek kapelusza wiedźmy, czapka Mikołaja).
     */
    private static void lathe(float[][] rings, int seg) {
        for (int i = 0; i < rings.length - 1; i++) {
            float[] a = rings[i], b = rings[i + 1];
            for (int s = 0; s < seg; s++) {
                float t0 = (float) (s / (double) seg * Math.PI * 2), t1 = (float) ((s + 1) / (double) seg * Math.PI * 2), tm = (t0 + t1) / 2f;
                int ca = lit((int) a[2], tm), cb = lit((int) b[2], tm);
                if (a[0] > 0.001f) tri(ringPoint(a, t0), ringPoint(a, t1), ringPoint(b, t1), ca, ca, cb);
                if (b[0] > 0.001f) tri(ringPoint(a, t0), ringPoint(b, t1), ringPoint(b, t0), ca, cb, cb);
            }
        }
    }

    /** Kula (pompon, kulka na koronie) */
    private static void ball(float cx, float cy, float cz, float r, int c) {
        float[][] rings = new float[5][];
        for (int i = 0; i <= 4; i++) {
            double a = i / 4.0 * Math.PI;
            rings[i] = new float[]{(float) Math.sin(a) * r, cy - (float) Math.cos(a) * r, mix(c, 0x000000, 0.25f * (i / 4f)), cx, cz};
        }
        lathe(rings, 8);
    }

    private static float[] polar(float a, float y, float rr) { return new float[]{(float) Math.cos(a) * rr, y, (float) Math.sin(a) * rr}; }

    /** Punkt na przodzie dyni (kula R = 3, H = 2.5), tuż nad powierzchnią */
    private static float[] onPumpkin(float x, float y) {
        return new float[]{x, y, -((float) Math.sqrt(Math.max(0, 9 * (1 - (y / 2.5) * (y / 2.5)) - x * x)) + 0.15f)};
    }

    private static void face(float x1, float y1, float x2, float y2, float x3, float y3) {
        tri(onPumpkin(x1, y1), onPumpkin(x2, y2), onPumpkin(x3, y3), 0xFFD35C, 0xFFD35C, 0xFFD35C);
    }

    /** Liczy siatkę do POS/COL/N. Zwraca true, gdy bryła świeci (aureola, płomień, pety). */
    public static boolean build(String style, int color) {
        N = 0;
        PW = 1f; SZ = 1f; RIB = 0f;
        int hot = mix(color, 0xFFFFFF, 0.55f);
        switch (style) {
            case "halo" -> {
                // torus nad głową: jasny od góry, w kolorze motywu od spodu
                float R = 4.6f, r = 0.62f, y0 = -11.2f;
                float[][] rings = new float[9][];
                for (int i = 0; i <= 8; i++) {
                    double a = i / 8.0 * Math.PI * 2;
                    rings[i] = new float[]{R + (float) Math.cos(a) * r, y0 + (float) Math.sin(a) * r, mix(hot, color, 0.5f + 0.5f * (float) Math.sin(a))};
                }
                lathe(rings, SEG);
                return true;
            }
            case "crown" -> {
                int gold = 0xE2AD2B, light = 0xFFE58A, dark = 0xA8741A;
                float y0 = -7.6f, y1 = -9.6f, R = 4.55f;
                lathe(new float[][]{{R, y0, dark}, {R + 0.25f, y0 - 0.35f, gold}, {R, y0 - 0.7f, gold}, {R, y1, light}, {R + 0.2f, y1 - 0.3f, light},
                        {R - 0.15f, y1 - 0.3f, gold}, {R - 0.15f, y0, dark}}, SEG);
                // zęby korony: 8 trójkątów na obręczy, na przemian wysokie i niskie, z kulką na czubku
                for (int i = 0; i < 8; i++) {
                    float a0 = (float) ((i - 0.42) / 8 * Math.PI * 2), a1 = (float) ((i + 0.42) / 8 * Math.PI * 2), am = (float) (i / 8.0 * Math.PI * 2);
                    float h = i % 2 == 1 ? 1.9f : 3.1f;
                    int c = lit(gold, am), ct = lit(light, am);
                    tri(polar(a0, y1 - 0.3f, R), polar(a1, y1 - 0.3f, R), polar(am, y1 - 0.3f - h, R + 0.25f), c, c, ct);
                    tri(polar(a1, y1 - 0.3f, R - 0.15f), polar(a0, y1 - 0.3f, R - 0.15f), polar(am, y1 - 0.3f - h, R + 0.25f), mix(c, 0x000000, 0.2f), mix(c, 0x000000, 0.2f), ct);
                    ball((float) Math.cos(am) * (R + 0.25f), y1 - 0.3f - h - 0.3f, (float) Math.sin(am) * (R + 0.25f), 0.42f, i % 2 == 1 ? light : hot);
                    // klejnot w kolorze motywu na obręczy pod każdym wysokim zębem
                    if (i % 2 == 0) {
                        float yc = (y0 + y1) / 2f - 0.35f, w = 0.13f;
                        tri(polar(am - w, yc, R + 0.12f), polar(am, yc - 0.75f, R + 0.12f), polar(am + w, yc, R + 0.12f), color, hot, color);
                        tri(polar(am - w, yc, R + 0.12f), polar(am + w, yc, R + 0.12f), polar(am, yc + 0.75f, R + 0.12f), color, color, mix(color, 0x000000, 0.35f));
                    }
                }
                return false;
            }
            case "tophat" -> {
                int black = 0x1B1B22, sheen = 0x34343F;
                lathe(new float[][]{{0, -8.15f, black}, {5.7f, -8.15f, black}, {5.9f, -8.45f, sheen}, {5.7f, -8.75f, black}, {3.75f, -8.75f, black},
                        {3.75f, -10.0f, color}, {3.8f, -10.05f, hot}, {3.8f, -10.1f, black}, {3.95f, -15.6f, sheen}, {3.8f, -15.9f, black}, {0, -15.9f, sheen}}, SEG);
                return false;
            }
            case "witch" -> {
                int hat = 0x2C1646, hi = 0x4A2A70, band = 0x16081F;
                lathe(new float[][]{{0, -8.2f, hat}, {6.9f, -7.75f, hat}, {7.1f, -8.05f, hi}, {6.8f, -8.4f, hat}, {4.3f, -8.7f, hat}, {4.2f, -9.5f, band}, {4.1f, -10.4f, band},
                        {3.5f, -11.6f, hi, 0, 0.2f}, {2.7f, -13.4f, hat, 0.1f, 0.7f}, {1.9f, -15.0f, hat, 0.3f, 1.5f}, {1.1f, -16.2f, hi, 0.7f, 2.6f},
                        {0.45f, -16.9f, hat, 1.3f, 3.8f}, {0, -17.1f, hat, 1.9f, 4.7f}}, SEG);
                // złota klamra z przodu na pasku
                int gold = 0xE2AD2B, goldHi = 0xFFE58A;
                float z = -4.32f, z2 = -4.36f;
                tri(new float[]{-0.95f, -9.3f, z}, new float[]{0.95f, -9.3f, z}, new float[]{0.95f, -10.6f, z}, gold, gold, goldHi);
                tri(new float[]{-0.95f, -9.3f, z}, new float[]{0.95f, -10.6f, z}, new float[]{-0.95f, -10.6f, z}, gold, goldHi, goldHi);
                tri(new float[]{-0.5f, -9.6f, z2}, new float[]{0.5f, -9.6f, z2}, new float[]{0.5f, -10.3f, z2}, band, band, band);
                tri(new float[]{-0.5f, -9.6f, z2}, new float[]{0.5f, -10.3f, z2}, new float[]{-0.5f, -10.3f, z2}, band, band, band);
                return false;
            }
            case "santa" -> {
                int red = 0xC8202A, redHi = 0xE8424A, white = 0xF6F6FA, shade = 0xD6D6E2;
                // futrzane obszycie (gruby wałek) i czerwony stożek opadający na bok
                lathe(new float[][]{{4.2f, -7.5f, shade}, {5.15f, -7.7f, white}, {5.35f, -8.5f, white}, {5.15f, -9.3f, white}, {4.3f, -9.5f, shade},
                        {4.25f, -9.6f, red}, {3.7f, -11.2f, redHi, 0.2f, 0.2f}, {3.0f, -12.8f, red, 0.7f, 0.7f}, {2.2f, -14.0f, redHi, 1.6f, 1.4f},
                        {1.4f, -14.6f, red, 2.8f, 2.1f}, {0.8f, -14.5f, red, 4.0f, 2.7f}, {0.3f, -13.9f, red, 4.9f, 3.1f}}, SEG);
                ball(5.3f, -13.2f, 3.3f, 1.25f, white);
                return false;
            }
            case "backpack" -> {
                int leather = 0x7A5230, hi = 0x94673C, dark = 0x4F3520;
                // worek o przekroju zaokrąglonego prostokąta, z ciemną klapą na górze
                PW = 0.6f; SZ = 0.5f;
                lathe(at(new float[][]{{0, 0.9f, dark}, {2.7f, 1.0f, dark}, {3.55f, 1.7f, dark}, {3.8f, 3.0f, dark}, {3.8f, 3.7f, dark},
                        {3.6f, 3.75f, leather}, {3.7f, 6.0f, hi}, {3.7f, 8.6f, leather}, {3.3f, 9.6f, dark}, {0, 9.8f, dark}}, 0f, 3.9f), SEG);
                // kieszeń z tyłu
                SZ = 0.4f;
                lathe(at(new float[][]{{0, 5.5f, dark}, {2.0f, 5.6f, dark}, {2.3f, 6.2f, leather}, {2.3f, 8.4f, leather}, {2.0f, 8.9f, dark}, {0, 8.9f, dark}}, 0f, 5.6f), SEG);
                // klamra w kolorze motywu na klapie
                float z = 5.88f;
                tri(new float[]{-0.7f, 2.6f, z}, new float[]{0.7f, 2.6f, z}, new float[]{0.7f, 3.9f, z}, color, hot, color);
                tri(new float[]{-0.7f, 2.6f, z}, new float[]{0.7f, 3.9f, z}, new float[]{-0.7f, 3.9f, z}, color, color, mix(color, 0x000000, 0.3f));
                // szelki: przez ramię i z przodu tułowia
                for (int s = -1; s <= 1; s += 2) {
                    float x0 = s * 2.6f - 0.6f, x1 = s * 2.6f + 0.6f;
                    quad(new float[]{x0, -0.12f, -2.12f}, new float[]{x1, -0.12f, -2.12f}, new float[]{x1, -0.12f, 2.1f}, new float[]{x0, -0.12f, 2.1f}, dark);
                    quad(new float[]{x0, -0.12f, -2.12f}, new float[]{x1, -0.12f, -2.12f}, new float[]{x1, 7f, -2.12f}, new float[]{x0, 7f, -2.12f}, dark);
                }
                return false;
            }
            case "jetpack" -> {
                int metal = 0x8C8F99, hi = 0xB9BCC6, dark = 0x4A4D57;
                // dwa zbiorniki z kopułą, pasek w kolorze motywu, dysza z rozgrzanym wnętrzem
                for (int s = -1; s <= 1; s += 2)
                    lathe(at(new float[][]{{0, 0.6f, hi}, {0.9f, 0.8f, hi}, {1.45f, 1.4f, metal}, {1.6f, 2.2f, metal}, {1.6f, 3.4f, metal}, {1.68f, 3.45f, hot}, {1.68f, 4.2f, color}, {1.6f, 4.25f, metal},
                            {1.6f, 8.2f, metal}, {1.3f, 9.0f, dark}, {0.9f, 9.3f, dark}, {1.25f, 10.4f, dark}, {1.0f, 10.4f, 0x22242A}, {0.6f, 9.5f, 0xFF8A1A}, {0, 9.5f, 0xFFD35C}}, s * 1.95f, 3.7f), 8);
                // łącznik między zbiornikami
                PW = 0.5f; SZ = 0.7f;
                lathe(at(new float[][]{{0, 2.4f, dark}, {1.3f, 2.5f, dark}, {1.3f, 7.4f, metal}, {0, 7.5f, dark}}, 0f, 3.2f), 8);
                return false;
            }
            case "flame" -> {
                // płomień z dyszy: długość 1, rysujący skaluje go w osi y
                lathe(new float[][]{{0.95f, 0, 0xFF8A1A}, {0.7f, 0.4f, 0xFF8A1A}, {0, 1, 0xFF5A10}}, 8);
                lathe(new float[][]{{0.5f, 0, 0xFFF1A0}, {0, 0.6f, 0xFFF1A0}}, 8);
                return true;
            }
            case "petcrystal" -> {
                // sześciokątny kryształ i dwa odłamki obok
                int deep = mix(color, 0x000000, 0.4f);
                float[][] gems = {{0, 0, 0, 1}, {2.5f, -1.2f, 0.8f, 0.45f}, {-2.1f, 1.0f, -1.5f, 0.35f}};
                for (float[] g : gems) {
                    float y = g[1], k = g[3];
                    lathe(at(new float[][]{{0, y - 3.4f * k, hot}, {1.5f * k, y - 1.3f * k, color}, {1.5f * k, y + 0.7f * k, color}, {0, y + 2.6f * k, deep}}, g[0], g[2]), 6);
                }
                return true;
            }
            case "petpumpkin" -> {
                float R = 3f, H = 2.5f;
                // spłaszczona kula z żebrami, ogonek, wycięta buzia z przodu
                RIB = 0.1f;
                float[][] rings = new float[7][];
                for (int i = 0; i <= 6; i++) {
                    double a = i / 6.0 * Math.PI;
                    rings[i] = new float[]{(float) Math.sin(a) * R, -(float) Math.cos(a) * H, mix(0xF58A2A, 0xB64F10, i / 6f)};
                }
                lathe(rings, SEG);
                RIB = 0f;
                lathe(new float[][]{{0.55f, -2.3f, 0x4E7A22}, {0.45f, -3.3f, 0x4E7A22, 0.2f, 0}, {0, -3.6f, 0x35561A, 0.6f, 0}}, 6);
                for (int s = -1; s <= 1; s += 2) face(s * 0.6f, -0.3f, s * 1.9f, -0.5f, s * 1.3f, -1.4f);
                face(-0.3f, 0.3f, 0.3f, 0.3f, 0, -0.3f);
                for (int i = -2; i < 2; i++) face(i * 0.9f, 0.9f, (i + 1) * 0.9f, 0.9f, (i + 0.5f) * 0.9f, 1.7f);
                return true;
            }
            default -> { return false; }
        }
    }
}
