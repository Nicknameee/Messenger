package spring.application.tree.data.utility.mailing.models;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

public enum ActionType {
    SIGN_UP("sign_up", "to sign up"),
    RESTORE_PASSWORD("restore_password", "to restore password"),
    CHANGE_EMAIL("change_email", "to change email"),
    CHANGE_PASSWORD("change_password", "to change password"),
    SPAM("spam", ""),
    NOTIFICATION("notification", "");

    @Getter
    private final String description;
    @Getter
    private final String processDescription;

    public static final List<ActionType> confirmationActions = Arrays.asList(SIGN_UP, RESTORE_PASSWORD, CHANGE_EMAIL, CHANGE_PASSWORD);
    public static final List<ActionType> simpleActions = Arrays.asList(SPAM, NOTIFICATION);

    ActionType(String description, String processDescription) {
        this.description = description;
        this.processDescription = processDescription;
    }

    public static ActionType fromKey(String actionDescription) {
        return Arrays.stream(ActionType.values()).filter(action -> action.getDescription().equals(actionDescription)).findAny().orElse(null);
    }
}
