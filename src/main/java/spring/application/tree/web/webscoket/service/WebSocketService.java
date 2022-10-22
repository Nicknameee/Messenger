package spring.application.tree.web.webscoket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final long timeout = TimeUnit.SECONDS.toMillis(10);
    private final SimpMessagingTemplate messagingTemplate;

    public void sendMessage(String message, String destination) {
        messagingTemplate.setSendTimeout(timeout);
        messagingTemplate.convertAndSend(destination, message);
    }

    public void sendMessage(Object payload, String destination) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        sendMessage(mapper.writeValueAsString(payload), destination);
    }
}
