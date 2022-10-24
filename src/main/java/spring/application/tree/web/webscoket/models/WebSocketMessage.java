package spring.application.tree.web.webscoket.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WebSocketMessage {
    private Object payload;
    private WebSocketEvent event;
}
