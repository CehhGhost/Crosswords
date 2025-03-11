package com.backend.crosswords.admin.enums;

public enum AuthorityEnum {
    EDIT_DELETE_DOCS("This authority lets you edit/delete documents");
    private final String description;

    AuthorityEnum(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
