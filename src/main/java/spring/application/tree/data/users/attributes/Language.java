package spring.application.tree.data.users.attributes;

import lombok.Getter;

@Getter
public enum Language {
    ENGLISH("EN"), UKRAINIAN("UA");

    private final String language;

    Language(String language) {
        this.language = language;
    }
}
