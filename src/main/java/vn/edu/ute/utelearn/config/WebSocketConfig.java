package vn.edu.ute.utelearn.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final UtelearnProperties properties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        UtelearnProperties.Websocket ws = properties.getWebsocket();
        registry.enableSimpleBroker(ws.getTopicDestinationPrefix(), ws.getQueueDestinationPrefix());
        registry.setApplicationDestinationPrefixes(ws.getAppDestinationPrefix());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String endpoint = properties.getWebsocket().getEndpoint();
        registry.addEndpoint(endpoint)
                .setAllowedOriginPatterns("*")
                .withSockJS();
        registry.addEndpoint(endpoint)
                .setAllowedOriginPatterns("*");
    }
}
