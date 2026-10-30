package pl.gruzclient.obs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pl.gruzclient.GruzClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Minimalny klient OBS WebSocket 5 (wbudowany w OBS 28+). Łączy się, loguje i wysyła
 * SaveReplayBuffer (albo StartReplayBuffer, jeśli bufor jest wyłączony). Wynik:
 * "saved", "started", "ok", "noObs", "auth", "error".
 */
public final class ObsClient {
    private ObsClient() {}

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public static void saveReplay(int port, String password, boolean testOnly, Consumer<String> done) {
        AtomicBoolean finished = new AtomicBoolean(false);
        Consumer<String> once = r -> { if (finished.compareAndSet(false, true)) done.accept(r); };
        StringBuilder buf = new StringBuilder();
        WebSocket.Listener listener = new WebSocket.Listener() {
            boolean triedStart = false;

            @Override
            public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                buf.append(data);
                if (last) {
                    String msg = buf.toString();
                    buf.setLength(0);
                    try { handle(ws, JsonParser.parseString(msg).getAsJsonObject()); }
                    catch (Exception e) { once.accept("error"); ws.abort(); }
                }
                ws.request(1);
                return null;
            }

            private void handle(WebSocket ws, JsonObject m) throws Exception {
                int op = m.get("op").getAsInt();
                JsonObject d = m.getAsJsonObject("d");
                if (op == 0) { // Hello
                    JsonObject id = new JsonObject();
                    id.addProperty("rpcVersion", 1);
                    if (d.has("authentication")) {
                        if (password == null || password.isEmpty()) { once.accept("auth"); ws.abort(); return; }
                        JsonObject a = d.getAsJsonObject("authentication");
                        String secret = b64(sha256(password + a.get("salt").getAsString()));
                        id.addProperty("authentication", b64(sha256(secret + a.get("challenge").getAsString())));
                    }
                    send(ws, 1, id);
                } else if (op == 2) { // Identified
                    if (testOnly) { request(ws, "GetReplayBufferStatus"); return; }
                    request(ws, "SaveReplayBuffer");
                } else if (op == 7) { // RequestResponse
                    String type = d.get("requestType").getAsString();
                    JsonObject st = d.getAsJsonObject("requestStatus");
                    boolean ok = st.get("result").getAsBoolean();
                    if (type.equals("GetReplayBufferStatus")) {
                        boolean active = ok && d.has("responseData") && d.getAsJsonObject("responseData").get("outputActive").getAsBoolean();
                        if (active) { once.accept("ok"); ws.sendClose(WebSocket.NORMAL_CLOSURE, ""); }
                        else { triedStart = true; request(ws, "StartReplayBuffer"); }
                    } else if (type.equals("SaveReplayBuffer")) {
                        if (ok) { once.accept("saved"); ws.sendClose(WebSocket.NORMAL_CLOSURE, ""); }
                        else if (!triedStart) { triedStart = true; request(ws, "StartReplayBuffer"); }
                        else { once.accept("error"); ws.sendClose(WebSocket.NORMAL_CLOSURE, ""); }
                    } else if (type.equals("StartReplayBuffer")) {
                        once.accept(ok ? "started" : "error");
                        ws.sendClose(WebSocket.NORMAL_CLOSURE, "");
                    }
                }
            }

            @Override
            public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                once.accept(code == 4009 ? "auth" : "error");
                return null;
            }

            @Override
            public void onError(WebSocket ws, Throwable error) { once.accept("noObs"); }
        };
        HTTP.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .buildAsync(URI.create("ws://127.0.0.1:" + port), listener)
                .whenComplete((ws, err) -> {
                    if (err != null) {
                        GruzClient.LOG.info("OBS niedostępny: {}", err.getMessage());
                        once.accept("noObs");
                    }
                });
    }

    private static int counter = 0;

    private static void request(WebSocket ws, String type) {
        JsonObject d = new JsonObject();
        d.addProperty("requestType", type);
        d.addProperty("requestId", "gruz" + (++counter));
        send(ws, 6, d);
    }

    private static void send(WebSocket ws, int op, JsonObject d) {
        JsonObject m = new JsonObject();
        m.addProperty("op", op);
        m.add("d", d);
        ws.sendText(m.toString(), true);
    }

    private static byte[] sha256(String s) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String b64(byte[] b) { return Base64.getEncoder().encodeToString(b); }
}
