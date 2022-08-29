package spring.application.tree.data.users.attributes;

import lombok.Getter;

@Getter
public enum Permission {
    CREATE_USER          ("permission:user:create"),
    READ_USER            ("permission:user:read"),
    UPDATE_USER          ("permission:user:update"),
    DELETE_USER          ("permission:user:delete");

    private final String permission;

    Permission(String permission)
    {
        this.permission = permission;
    }
}