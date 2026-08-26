package dev.voidpulsar.lc_claim_economy.web;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public final class WebServerLifecycle {
    private final EmbeddedWebServer webServer = new EmbeddedWebServer();

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        webServer.start(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        webServer.stop();
    }
}
