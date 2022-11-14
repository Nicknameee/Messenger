package spring.application.tree.web.webscoket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import spring.application.tree.web.webscoket.models.WebSocketEvent;
import spring.application.tree.web.webscoket.models.WebSocketMessage;

import javax.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@PropertySource("classpath:websocket.properties")
@Slf4j
public class WebSocketService {
    @Value("${websocket.timeout}")
    private String timeout;
    private final SimpMessagingTemplate messagingTemplate;
    @PostConstruct
    private void setup() {
        log.debug("Timeout for websocket message sending has been set to '{}'", timeout);
        messagingTemplate.setSendTimeout(Long.parseLong(timeout));
    }

    private void sendMessage(String message, String destination) {
        log.debug("Sending message '{}' to '{}'", message, destination);
        messagingTemplate.convertAndSend(destination, message);
    }

    public void sendMessage(Object payload, String destination, WebSocketEvent event) throws JsonProcessingException {
        if (!(payload instanceof WebSocketMessage)) {
            payload = new WebSocketMessage(payload, event);
        }
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        sendMessage(mapper.writeValueAsString(payload), destination);
    }
}
