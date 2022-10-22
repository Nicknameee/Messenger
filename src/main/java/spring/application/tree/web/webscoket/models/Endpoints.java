package spring.application.tree.web.webscoket.models;

import lombok.Getter;

@Getter
public enum Endpoints {
    CHAT("/topic/chat");

    private final String endpointPrefix;

    Endpoints(String endpointPrefix) {
        this.endpointPrefix = endpointPrefix;
    }
}
