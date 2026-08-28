package dev.voidpulsar.lc_claim_economy.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.voidpulsar.lc_claim_economy.LcClaimEconomy;
import dev.voidpulsar.lc_claim_economy.config.LcClaimEconomyConfig;
import net.minecraft.server.MinecraftServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Small built-in HTTP server (JDK's own {@link HttpServer}, no new mod
 * dependency) serving two independent things, both gated by their own config
 * flags:
 * <ul>
 *   <li>{@code /} and {@code /api/data} - the read-only, unauthenticated
 *   leaderboard/info page (see {@code webEnabled}). Every request re-reads
 *   live data straight from {@link WebDataService} - no caching needed.</li>
 *   <li>{@code /dashboard} and {@code /api/*} dashboard routes - the
 *   login-gated player dashboard (see {@code webDashboardEnabled}), backed
 *   by {@link DashboardApi}, {@link DashboardSessions}, and short-lived
 *   session cookies. FTB Chunks/Teams only.</li>
 * </ul>
 */
public final class EmbeddedWebServer {
    private static final String INDEX_RESOURCE = "/web/index.html";
    private static final String DASHBOARD_RESOURCE = "/web/dashboard.html";
    private static final String SESSION_COOKIE = "lce_session";
    private static final int MAX_BODY_BYTES = 8192;

    private HttpServer httpServer;
    private byte[] indexHtml;
    private byte[] dashboardHtml;

    public void start(MinecraftServer server) {
        if (!LcClaimEconomyConfig.SERVER.webEnabled.get()) {
            return;
        }

        indexHtml = loadResource(INDEX_RESOURCE);
        if (indexHtml == null) {
            LcClaimEconomy.LOGGER.error("Claim Economy web server: could not load {} from mod resources, not starting.", INDEX_RESOURCE);
            return;
        }

        boolean dashboardEnabled = LcClaimEconomyConfig.SERVER.webDashboardEnabled.get();
        if (dashboardEnabled) {
            dashboardHtml = loadResource(DASHBOARD_RESOURCE);
            if (dashboardHtml == null) {
                LcClaimEconomy.LOGGER.error("Claim Economy web server: could not load {}, dashboard disabled for this session.", DASHBOARD_RESOURCE);
                dashboardEnabled = false;
            }
        }

        String bindAddress = LcClaimEconomyConfig.SERVER.webBindAddress.get();
        int port = LcClaimEconomyConfig.SERVER.webPort.get();

        try {
            httpServer = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            httpServer.createContext("/", this::handleIndex);
            httpServer.createContext("/api/data", exchange -> handleData(exchange, server));
            httpServer.createContext("/api/theme", this::handleTheme);

            if (dashboardEnabled) {
                httpServer.createContext("/dashboard", this::handleDashboardPage);
                httpServer.createContext("/api/login", exchange -> handleLogin(exchange, server));
                httpServer.createContext("/api/logout", this::handleLogout);
                httpServer.createContext("/api/me", exchange -> handleMe(exchange, server));
                // Registered alongside the more specific /api/dashboard/* action contexts below -
                // HttpServer resolves each request to the longest matching prefix, so this exact
                // path only ever serves the dashboard data GET, never the action POSTs.
                httpServer.createContext("/api/dashboard", exchange -> handleDashboardData(exchange, server));
                httpServer.createContext("/api/dashboard/protection", exchange -> handleProtection(exchange, server));
                httpServer.createContext("/api/dashboard/peaceful", exchange -> handlePeaceful(exchange, server));
                httpServer.createContext("/api/dashboard/forceload", exchange -> handleForceLoad(exchange, server));
                httpServer.createContext("/api/dashboard/unclaim", exchange -> handleUnclaim(exchange, server));
                httpServer.createContext("/api/dashboard/war", exchange -> handleWar(exchange, server));
            }

            httpServer.setExecutor(Executors.newFixedThreadPool(4, daemonThreadFactory()));
            httpServer.start();
            LcClaimEconomy.LOGGER.info("Claim Economy web server started on {}:{} (dashboard: {})", bindAddress, port, dashboardEnabled ? "enabled" : "disabled");
        } catch (IOException e) {
            LcClaimEconomy.LOGGER.error("Claim Economy web server failed to start on {}:{} - is the port already in use?", bindAddress, port, e);
            httpServer = null;
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
            LcClaimEconomy.LOGGER.info("Claim Economy web server stopped.");
        }
    }

    // ---------------- Static pages ----------------

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }
        sendHtml(exchange, 200, indexHtml);
    }

    private void handleDashboardPage(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }
        sendHtml(exchange, 200, dashboardHtml);
    }

    private void handleData(HttpExchange exchange, MinecraftServer server) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            sendJson(exchange, 200, WebApiJson.buildDataPayload(server));
        } catch (Exception e) {
            LcClaimEconomy.LOGGER.error("Claim Economy web server: failed to build /api/data response", e);
            sendPlain(exchange, 500, "Internal error building leaderboard data");
        }
    }

    private void handleTheme(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }
        var config = LcClaimEconomyConfig.SERVER;
        String json = JsonWriter.object()
                .field("siteName", config.webSiteName.get())
                .field("accentColor", config.webAccentColor.get())
                .field("logoUrl", config.webLogoUrl.get())
                .field("customCss", config.webCustomCss.get())
                .field("dashboardEnabled", config.webDashboardEnabled.get())
                .build();
        sendJson(exchange, 200, json);
    }

    // ---------------- Auth ----------------

    private void handleLogin(HttpExchange exchange, MinecraftServer server) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }
        JsonReader body = JsonReader.parse(readBody(exchange));
        Optional<UUID> playerId = DashboardSessions.LOGIN_CODES.redeem(body.getString("code"));
        if (playerId.isEmpty()) {
            sendJson(exchange, 401, resultJson(false, "Invalid or expired code."));
            return;
        }

        int ttlMinutes = LcClaimEconomyConfig.SERVER.webSessionMinutes.get();
        String token = DashboardSessions.SESSIONS.create(playerId.get(), ttlMinutes);
        exchange.getResponseHeaders().add("Set-Cookie",
                SESSION_COOKIE + "=" + token + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + (ttlMinutes * 60));
        sendJson(exchange, 200, resultJson(true, "Logged in."));
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }
        String token = sessionToken(exchange);
        DashboardSessions.SESSIONS.invalidate(token);
        exchange.getResponseHeaders().add("Set-Cookie", SESSION_COOKIE + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
        sendJson(exchange, 200, resultJson(true, "Logged out."));
    }

    private void handleMe(HttpExchange exchange, MinecraftServer server) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }
        Optional<UUID> playerId = resolveSession(exchange);
        if (playerId.isEmpty()) {
            sendJson(exchange, 401, resultJson(false, "Not logged in."));
            return;
        }
        String json = JsonWriter.object()
                .field("name", DashboardApi.playerName(server, playerId.get()))
                .field("uuid", playerId.get().toString())
                .build();
        sendJson(exchange, 200, json);
    }

    // ---------------- Dashboard data + actions ----------------

    private void handleDashboardData(HttpExchange exchange, MinecraftServer server) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }
        Optional<UUID> playerId = resolveSession(exchange);
        if (playerId.isEmpty()) {
            sendJson(exchange, 401, resultJson(false, "Not logged in."));
            return;
        }
        String json;
        try {
            json = DashboardApi.buildDashboardJson(server, playerId.get());
        } catch (Exception e) {
            LcClaimEconomy.LOGGER.error("Claim Economy web server: failed to build dashboard data", e);
            sendJson(exchange, 500, resultJson(false, "Internal error building dashboard data."));
            return;
        }
        if (json == null) {
            sendJson(exchange, 404, resultJson(false, "No team found for this player."));
            return;
        }
        sendJson(exchange, 200, json);
    }

    private void handleProtection(HttpExchange exchange, MinecraftServer server) throws IOException {
        Optional<UUID> playerId = requirePostSession(exchange);
        if (playerId.isEmpty()) {
            return;
        }
        JsonReader body = JsonReader.parse(readBody(exchange));
        ActionResult result = DashboardApi.applyProtection(server, playerId.get(), body.getString("key"), body.getBoolean("active", false));
        sendJson(exchange, result.success() ? 200 : 400, resultJson(result.success(), result.message()));
    }

    private void handlePeaceful(HttpExchange exchange, MinecraftServer server) throws IOException {
        Optional<UUID> playerId = requirePostSession(exchange);
        if (playerId.isEmpty()) {
            return;
        }
        JsonReader body = JsonReader.parse(readBody(exchange));
        ActionResult result = DashboardApi.setPeaceful(server, playerId.get(), body.getBoolean("active", false));
        sendJson(exchange, result.success() ? 200 : 400, resultJson(result.success(), result.message()));
    }

    private void handleForceLoad(HttpExchange exchange, MinecraftServer server) throws IOException {
        Optional<UUID> playerId = requirePostSession(exchange);
        if (playerId.isEmpty()) {
            return;
        }
        JsonReader body = JsonReader.parse(readBody(exchange));
        ActionResult result = DashboardApi.toggleForceLoad(server, playerId.get(), body.getString("key"), body.getBoolean("load", false));
        sendJson(exchange, result.success() ? 200 : 400, resultJson(result.success(), result.message()));
    }

    private void handleUnclaim(HttpExchange exchange, MinecraftServer server) throws IOException {
        Optional<UUID> playerId = requirePostSession(exchange);
        if (playerId.isEmpty()) {
            return;
        }
        JsonReader body = JsonReader.parse(readBody(exchange));
        ActionResult result = DashboardApi.unclaimChunk(server, playerId.get(), body.getString("key"));
        sendJson(exchange, result.success() ? 200 : 400, resultJson(result.success(), result.message()));
    }

    private void handleWar(HttpExchange exchange, MinecraftServer server) throws IOException {
        Optional<UUID> playerId = requirePostSession(exchange);
        if (playerId.isEmpty()) {
            return;
        }
        JsonReader body = JsonReader.parse(readBody(exchange));
        UUID targetTeamId;
        try {
            targetTeamId = UUID.fromString(body.getString("teamId"));
        } catch (Exception e) {
            sendJson(exchange, 400, resultJson(false, "Invalid team id."));
            return;
        }
        ActionResult result = DashboardApi.toggleWar(server, playerId.get(), targetTeamId);
        sendJson(exchange, result.success() ? 200 : 400, resultJson(result.success(), result.message()));
    }

    /** Checks method + session for a POST action endpoint; sends the error response itself if either fails. */
    private Optional<UUID> requirePostSession(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return Optional.empty();
        }
        Optional<UUID> playerId = resolveSession(exchange);
        if (playerId.isEmpty()) {
            sendJson(exchange, 401, resultJson(false, "Not logged in."));
        }
        return playerId;
    }

    // ---------------- Helpers ----------------

    private Optional<UUID> resolveSession(HttpExchange exchange) {
        return DashboardSessions.SESSIONS.resolve(sessionToken(exchange));
    }

    private String sessionToken(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            return null;
        }
        for (String part : cookieHeader.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(SESSION_COOKIE + "=")) {
                return trimmed.substring(SESSION_COOKIE.length() + 1);
            }
        }
        return null;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[512];
            int read;
            int total = 0;
            while ((read = in.read(chunk)) != -1) {
                total += read;
                if (total > MAX_BODY_BYTES) {
                    break;
                }
                buffer.write(chunk, 0, read);
            }
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    private String resultJson(boolean ok, String message) {
        return JsonWriter.object().field("ok", ok).field("message", message == null ? "" : message).build();
    }

    private void sendPlain(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private void sendHtml(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static byte[] loadResource(String path) {
        try (InputStream in = EmbeddedWebServer.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            in.transferTo(buffer);
            return buffer.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private static ThreadFactory daemonThreadFactory() {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, "lc-claim-economy-web-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
