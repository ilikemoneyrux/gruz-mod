package pl.gruzclient.legacy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Połączenie z serwerem Gruz na 1.8.9 (od 0.2.0): ten sam protokół co w modzie na nowe wersje.
 * Logowanie bez hasła – gra potwierdza konto w Mojangu (joinServer), serwer sprawdza hasJoined.
 * Java 8 nie ma klienta WebSocket, więc jest tu własny, mały: nagłówek HTTP, ramki tekstowe z maską, ping/pong.
 * Daje: peleryny Gruz (swoje i innych graczy), znajomych na czacie (.friends, .msg, .r) i zaproszenia do party.
 */
public final class GruzNet {
    private GruzNet() {}

    private static final String URL = System.getProperty("gruz.server", "wss://gruz-server.onrender.com");
    /** Kosmetyki wybrane w launcherze (cape=nebula;head=…;color=b45cff) */
    public static volatile String myCosmetics = System.getProperty("gruz.cosmetics", "cape=gruz");
    /** uuid gracza -> jego kosmetyki (tylko gracze Gruz) */
    public static final Map<UUID, String> COSMETICS = new ConcurrentHashMap<UUID, String>();
    public static final List<Friend> FRIENDS = new CopyOnWriteArrayList<Friend>();

    public static final class Friend {
        public String uuid, name, state, server;
        public boolean online;
    }

    private static volatile Socket sock;
    private static volatile OutputStream out;
    private static volatile boolean authed, gaveUp;
    private static volatile Thread thread;
    private static long nextTry;
    private static int fails, ticks;
    private static String lastPresence = "";
    private static final Set<UUID> asked = new HashSet<UUID>();
    private static Object askedWorld;
    private static volatile String lastDm, lastDmName, partyInvite;
    private static final SecureRandom RND = new SecureRandom();

    public static boolean connected() { return authed; }

    // ---------------- czat ----------------
    private static String tr(String en, String pl) { return Mods.pl() ? pl : en; }

    /** Linia na czacie gry (z dowolnego wątku) */
    static void chat(final String text) {
        final MinecraftClient mc = MinecraftClient.getInstance();
        mc.submit(new Runnable() {
            @Override public void run() {
                if (mc.inGameHud != null && mc.world != null) mc.inGameHud.getChatHud().addMessage(new LiteralText("§d[Gruz] §7" + text));
            }
        });
    }

    // ---------------- co tick (wątek gry) ----------------
    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Mods.cfg.network) { if (sock != null) close(); return; }
        if (Boolean.getBoolean("gruz.smoketest")) return; // test startu w CI nie łączy się z prawdziwym serwerem
        if (thread == null && !gaveUp && System.currentTimeMillis() >= nextTry) {
            Session s = mc.getSession();
            // konto offline (brak tokenu) nie przejdzie logowania w Mojangu – nie próbujemy
            if (s == null || s.getAccessToken() == null || (s.getAccessToken().length() < 8 && !URL.startsWith("ws://"))) { gaveUp = true; return; }
            thread = new Thread(new Runnable() { @Override public void run() { run0(); } }, "Gruz network");
            thread.setDaemon(true);
            thread.start();
        }
        if (!authed || ++ticks % 20 != 0) return;
        // status: menu / świat / serwer (znajomi widzą, gdzie grasz; serwer łączy graczy Gruz z tego samego serwera)
        String state = mc.world == null ? "menu" : mc.isInSingleplayer() ? "singleplayer" : "multiplayer";
        String server = "multiplayer".equals(state) && mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : null;
        String p = state + "|" + server;
        if (!p.equals(lastPresence)) {
            lastPresence = p;
            JsonObject o = msg("presence");
            o.addProperty("state", state);
            if (server != null) o.addProperty("server", server);
            send(o);
        }
        // co 5 s pytamy o kosmetyki graczy, których widzimy (każdego raz na świat)
        if (mc.world != null && ticks % 100 == 0) {
            if (askedWorld != mc.world) { askedWorld = mc.world; asked.clear(); }
            JsonArray ids = new JsonArray();
            for (PlayerEntity pl : new ArrayList<PlayerEntity>(mc.world.playerEntities)) {
                if (pl == mc.player || pl.getUuid() == null || !asked.add(pl.getUuid())) continue;
                ids.add(new com.google.gson.JsonPrimitive(pl.getUuid().toString()));
                if (ids.size() >= 90) break;
            }
            if (ids.size() > 0) { JsonObject o = msg("lookup"); o.add("uuids", ids); send(o); }
        }
    }

    // ---------------- połączenie (własny wątek) ----------------
    private static void run0() {
        try {
            URI u = URI.create(URL);
            boolean tls = "wss".equalsIgnoreCase(u.getScheme());
            int port = u.getPort() > 0 ? u.getPort() : tls ? 443 : 80;
            Socket plain = new Socket();
            plain.connect(new InetSocketAddress(u.getHost(), port), 20000);
            Socket s = plain;
            if (tls) {
                s = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(plain, u.getHost(), port, true);
                ((SSLSocket) s).startHandshake();
            }
            s.setSoTimeout(90000); // serwer pinguje co 30 s – cisza przez 90 s to zerwane połączenie
            byte[] key = new byte[16];
            RND.nextBytes(key);
            OutputStream o = s.getOutputStream();
            o.write(("GET / HTTP/1.1\r\nHost: " + u.getHost() + "\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: "
                    + Base64.getEncoder().encodeToString(key) + "\r\nSec-WebSocket-Version: 13\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            o.flush();
            DataInputStream in = new DataInputStream(s.getInputStream());
            StringBuilder head = new StringBuilder();
            while (head.length() < 8192 && head.indexOf("\r\n\r\n") < 0) { int c = in.read(); if (c < 0) break; head.append((char) c); }
            if (head.indexOf(" 101") < 0) throw new java.io.IOException("brak 101: " + head.substring(0, Math.min(40, head.length())));
            sock = s;
            out = o;
            JsonObject hello = msg("hello");
            hello.addProperty("client", "game");
            hello.addProperty("version", "1.8.9");
            send(hello);
            ByteArrayOutputStream part = new ByteArrayOutputStream();
            while (true) {
                int b0 = in.readUnsignedByte(), b1 = in.readUnsignedByte();
                long len = b1 & 127;
                if (len == 126) len = in.readUnsignedShort(); else if (len == 127) len = in.readLong();
                if (len < 0 || len > 4_000_000) throw new java.io.IOException("ramka " + len);
                byte[] data = new byte[(int) len];
                in.readFully(data);
                int op = b0 & 15;
                if (op == 8) break;
                if (op == 9) { frame(10, data); continue; }
                if (op == 10) continue;
                part.write(data);
                if ((b0 & 0x80) == 0) continue; // dalszy ciąg w następnej ramce
                String text = new String(part.toByteArray(), StandardCharsets.UTF_8);
                part.reset();
                try { handle(new JsonParser().parse(text).getAsJsonObject()); }
                catch (Exception e) { GruzLegacy.LOG.warn("Gruz: zła wiadomość z serwera: {}", e.toString()); }
            }
        } catch (Exception e) {
            GruzLegacy.LOG.info("Gruz: brak połączenia z serwerem ({})", e.toString());
        } finally {
            close();
            fails = Math.min(fails + 1, 5);
            nextTry = System.currentTimeMillis() + 10000L * (1 << (fails - 1)); // 10 s, 20 s, … 160 s
            thread = null;
        }
    }

    private static void close() {
        authed = false;
        lastPresence = "";
        Socket s = sock;
        sock = null;
        out = null;
        if (s != null) try { s.close(); } catch (Exception ignored) { }
    }

    private static JsonObject msg(String type) { JsonObject o = new JsonObject(); o.addProperty("t", type); return o; }

    public static void send(JsonObject o) {
        try { frame(1, o.toString().getBytes(StandardCharsets.UTF_8)); }
        catch (Exception e) { close(); }
    }

    /** Ramka klienta: zawsze z maską (wymóg protokołu) */
    private static synchronized void frame(int op, byte[] data) throws java.io.IOException {
        OutputStream o = out;
        if (o == null) return;
        ByteArrayOutputStream b = new ByteArrayOutputStream(data.length + 14);
        b.write(0x80 | op);
        if (data.length < 126) b.write(0x80 | data.length);
        else if (data.length < 65536) { b.write(0x80 | 126); b.write(data.length >> 8); b.write(data.length); }
        else { b.write(0x80 | 127); for (int i = 7; i >= 0; i--) b.write((int) (((long) data.length) >> (8 * i))); }
        byte[] mask = new byte[4];
        RND.nextBytes(mask);
        b.write(mask, 0, 4);
        for (int i = 0; i < data.length; i++) b.write(data[i] ^ mask[i & 3]);
        o.write(b.toByteArray());
        o.flush();
    }

    private static String str(JsonObject o, String k) { JsonElement e = o.get(k); return e == null || e.isJsonNull() ? null : e.getAsString(); }

    private static Friend friend(JsonObject f) {
        Friend x = new Friend();
        x.uuid = str(f, "uuid"); x.name = str(f, "name"); x.state = str(f, "state"); x.server = str(f, "server");
        x.online = f.has("online") && f.get("online").getAsBoolean();
        return x;
    }

    private static String myServer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.world != null && !mc.isInSingleplayer() && mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address.toLowerCase(java.util.Locale.ROOT) : null;
    }

    // ---------------- wiadomości z serwera ----------------
    private static void handle(JsonObject m) {
        String t = str(m, "t");
        if (t == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (t.equals("challenge")) {
            Session s = mc.getSession();
            try { mc.getSessionService().joinServer(s.getProfile(), s.getAccessToken(), str(m, "serverId")); }
            catch (Exception e) { GruzLegacy.LOG.info("Gruz: Mojang nie potwierdził konta ({})", e.toString()); }
            JsonObject a = msg("auth");
            a.addProperty("name", s.getUsername());
            send(a);
        } else if (t.equals("welcome")) {
            authed = true;
            fails = 0;
            lastPresence = "";
            JsonObject c = msg("cosmetics");
            c.addProperty("set", myCosmetics);
            send(c);
            GruzLegacy.LOG.info("Gruz: połączono z serwerem jako {}", str(m, "name"));
        } else if (t.equals("cosmetics")) {
            for (Map.Entry<String, JsonElement> e : m.getAsJsonObject("map").entrySet()) {
                try { COSMETICS.put(UUID.fromString(e.getKey()), e.getValue().getAsString()); } catch (Exception ignored) { }
            }
        } else if (t.equals("friends")) {
            List<Friend> list = new ArrayList<Friend>();
            for (JsonElement e : m.getAsJsonArray("friends")) list.add(friend(e.getAsJsonObject()));
            FRIENDS.clear();
            FRIENDS.addAll(list);
        } else if (t.equals("friend.status")) {
            Friend now = friend(m.getAsJsonObject("friend"));
            Friend old = null;
            for (Friend f : FRIENDS) if (f.uuid != null && f.uuid.equals(now.uuid)) old = f;
            if (old != null) FRIENDS.remove(old);
            FRIENDS.add(now);
            if (!Mods.cfg.friendAlerts) return;
            String here = myServer();
            boolean sameNow = here != null && now.server != null && "multiplayer".equals(now.state) && here.equals(now.server.toLowerCase(java.util.Locale.ROOT));
            boolean sameOld = old != null && here != null && old.server != null && "multiplayer".equals(old.state) && here.equals(old.server.toLowerCase(java.util.Locale.ROOT));
            if (sameNow && !sameOld) chat("§f" + now.name + " §7" + tr("joined this server", "wszedł na ten serwer"));
            else if (now.online && (old == null || !old.online)) chat("§f" + now.name + " §7" + tr("is online", "jest online"));
        } else if (t.equals("dm")) {
            String text = str(m, "text");
            if (text == null) text = m.has("sticker") ? "[" + str(m, "sticker") + "]" : m.has("voice") ? tr("[voice message – open the launcher]", "[wiadomość głosowa – otwórz launcher]") : "";
            String mine = mc.getSession().getProfile().getId() == null ? "" : mc.getSession().getProfile().getId().toString();
            if (mine.equals(str(m, "from"))) chat("§8→ " + str(m, "toName") + ": §f" + text);
            else { lastDm = str(m, "from"); lastDmName = str(m, "fromName"); chat("§f" + lastDmName + " §8» §f" + text + " §8(.r)"); }
        } else if (t.equals("party.invite")) {
            partyInvite = str(m, "id");
            chat("§f" + str(m, "from") + " §7" + tr("invites you to a party – type §f.party accept", "zaprasza Cię do party – wpisz §f.party accept"));
        } else if (t.equals("party.warp")) {
            chat("§f" + str(m, "from") + " §7" + tr("moves the party to", "przenosi party na") + " §f" + str(m, "address"));
        } else if (t.equals("notify")) {
            if ("friendRequest".equals(str(m, "kind"))) chat("§f" + str(m, "name") + " §7" + tr("sent you a friend request (accept it in the launcher)", "zaprasza Cię do znajomych (przyjmij w launcherze)"));
        } else if (t.equals("error")) {
            String code = str(m, "code");
            if ("AUTH_FAILED".equals(code) || "BANNED".equals(code)) gaveUp = true; // ta sesja już się nie zaloguje
            else if ("NOT_FRIENDS".equals(code)) chat(tr("You can only message friends.", "Pisać można tylko do znajomych."));
            else if ("INVITE_EXPIRED".equals(code)) chat(tr("That party invite has expired.", "To zaproszenie do party wygasło."));
        }
    }

    // ---------------- komendy na czacie ----------------
    private static Friend byName(String name) {
        for (Friend f : FRIENDS) if (f.name != null && f.name.equalsIgnoreCase(name)) return f;
        return null;
    }

    /** Zwraca true, gdy wiadomość była komendą Gruz (nie idzie na serwer Minecrafta) */
    public static boolean command(String raw) {
        String[] a = raw.trim().split("\\s+", 3);
        String c = a[0].toLowerCase(java.util.Locale.ROOT);
        if (!c.equals(".gruz") && !c.equals(".friends") && !c.equals(".msg") && !c.equals(".r") && !c.equals(".party")) return false;
        if (!Mods.cfg.network) { chat(tr("Gruz network is off (Right Shift menu).", "Sieć Gruz jest wyłączona (menu pod prawym Shiftem).")); return true; }
        if (c.equals(".gruz")) {
            chat((authed ? "§a" + tr("connected", "połączono") : "§c" + tr("not connected", "brak połączenia"))
                    + " §7· .friends · .msg <nick> <" + tr("text", "tekst") + "> · .r <" + tr("text", "tekst") + "> · .party accept|leave|invite <nick>");
            return true;
        }
        if (!authed) { chat("§c" + tr("No connection to the Gruz server.", "Brak połączenia z serwerem Gruz.")); return true; }
        if (c.equals(".friends")) {
            int n = 0;
            for (Friend f : FRIENDS) {
                if (!f.online) continue;
                n++;
                chat("§a● §f" + f.name + " §7" + ("multiplayer".equals(f.state) && f.server != null ? f.server : "singleplayer".equals(f.state) ? tr("singleplayer", "świat") : tr("in the launcher", "w launcherze")));
            }
            chat(n == 0 ? tr("No friends online (" + FRIENDS.size() + " total).", "Nikt ze znajomych nie jest online (razem " + FRIENDS.size() + ").")
                    : tr("Online: ", "Online: ") + n + "/" + FRIENDS.size());
        } else if (c.equals(".msg") || c.equals(".r")) {
            String to = null, text;
            if (c.equals(".r")) { to = lastDm; text = raw.trim().length() > 3 ? raw.trim().substring(3).trim() : ""; }
            else { Friend f = a.length > 1 ? byName(a[1]) : null; to = f == null ? null : f.uuid; text = a.length > 2 ? a[2] : ""; }
            if (to == null) { chat(c.equals(".r") ? tr("Nobody to reply to.", "Nie masz komu odpisać.") : tr("No such friend. See .friends", "Nie masz takiego znajomego. Zobacz .friends")); return true; }
            if (text.isEmpty()) { chat(tr("Empty message.", "Pusta wiadomość.")); return true; }
            JsonObject o = msg("dm");
            o.addProperty("to", to);
            o.addProperty("text", text.length() > 500 ? text.substring(0, 500) : text);
            send(o);
        } else {
            String sub = a.length > 1 ? a[1].toLowerCase(java.util.Locale.ROOT) : "";
            if (sub.equals("accept")) {
                if (partyInvite == null) { chat(tr("No party invite.", "Nie masz zaproszenia do party.")); return true; }
                JsonObject o = msg("party.accept"); o.addProperty("id", partyInvite); send(o);
                partyInvite = null;
                chat(tr("Joined the party.", "Dołączono do party."));
            } else if (sub.equals("leave")) { send(msg("party.leave")); chat(tr("Left the party.", "Opuszczono party.")); }
            else if (sub.equals("invite") && a.length > 2) {
                Friend f = byName(a[2].trim());
                if (f == null) { chat(tr("No such friend. See .friends", "Nie masz takiego znajomego. Zobacz .friends")); return true; }
                JsonObject o = msg("party.invite"); o.addProperty("uuid", f.uuid); send(o);
                chat(tr("Invited ", "Zaproszono ") + "§f" + f.name);
            } else chat(".party accept · .party leave · .party invite <nick>");
        }
        return true;
    }
}
