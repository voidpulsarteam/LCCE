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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A small, read-only, unauthenticated HTTP server for the live leaderboard
 * page - see {@code LcClaimEconomyConfig.SERVER.webEnabled}. Built on the
 * JDK's own {@link HttpServer} (part of the {@code jdk.httpserver} module,
 * bundled with every JDK) rather than pulling in a web framework as a new
 * mod dependency.
 * <p>
 * Every request re-reads live data straight from {@link WebDataService} -
 * there's no caching or push updates, since leaderboard/info requests are
 * infrequent (one browser tab polling every ~10s) and cheap enough
 * (iterating however many teams/parties exist) not to need it.
 */
public final class EmbeddedWebServer {
    private static final String INDEX_RESOURCE = "/web/index.html";

    private HttpServer httpServer;
    private byte[] indexHtml;

    public void start(MinecraftServer server) {
        if (!LcClaimEconomyConfig.SERVER.webEnabled.get()) {
            return;
        }

        indexHtml = loadIndexHtml();
        if (indexHtml == null) {
            LcClaimEconomy.LOGGER.error("Claim Economy web server: could not load {} from mod resources, not starting.", INDEX_RESOURCE);
            return;
        }

        String bindAddress = LcClaimEconomyConfig.SERVER.webBindAddress.get();
        int port = LcClaimEconomyConfig.SERVER.webPort.get();

        try {
            httpServer = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            httpServer.createContext("/", this::handleIndex);
            httpServer.createContext("/api/data", exchange -> handleData(exchange, server));
            httpServer.setExecutor(Executors.newFixedThreadPool(2, daemonThreadFactory()));
            httpServer.start();
            LcClaimEconomy.LOGGER.info("Claim Economy web server started on {}:{}", bindAddress, port);
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

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, indexHtml.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(indexHtml);
        }
    }

    private void handleData(HttpExchange exchange, MinecraftServer server) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }

        String json;
        try {
            json = WebApiJson.buildDataPayload(server);
        } catch (Exception e) {
            LcClaimEconomy.LOGGER.error("Claim Economy web server: failed to build /api/data response", e);
            sendPlain(exchange, 500, "Internal error building leaderboard data");
            return;
        }

        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private void sendPlain(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static byte[] loadIndexHtml() {
        try (InputStream in = EmbeddedWebServer.class.getResourceAsStream(INDEX_RESOURCE)) {
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
