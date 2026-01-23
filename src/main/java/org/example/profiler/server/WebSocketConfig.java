package org.example.profiler.server;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ProfilerWebSocketHandler handler = new ProfilerWebSocketHandler();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/profiler")
                .setAllowedOrigins("*");
    }
}
