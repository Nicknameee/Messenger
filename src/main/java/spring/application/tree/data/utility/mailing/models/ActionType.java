package spring.application.tree.data.utility.mailing.models;

import lombok.Getter;

import java.util.Arrays;

public enum ActionType {
    SIGN_UP("sign_up");

    @Getter
    private final String description;

    ActionType(String description) {
        this.description = description;
    }

    public static ActionType fromKey(String actionDescription) {
        return Arrays.stream(ActionType.values()).filter(action -> action.getDescription().equals(actionDescription)).findAny().orElse(null);
    }
}
