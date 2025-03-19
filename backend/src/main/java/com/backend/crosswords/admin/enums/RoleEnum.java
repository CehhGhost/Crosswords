package com.backend.crosswords.admin.enums;

public enum RoleEnum {
    ROLE_ADMIN(AuthorityEnum.values(), new String[]{"admin"}),
    ROLE_MODERATOR(new AuthorityEnum[]{AuthorityEnum.EDIT_DELETE_DOCS}, new String[]{"Gek", "Chook"});

    private final AuthorityEnum[] authorities;
    private final String[] usersWhiteList;

    RoleEnum(AuthorityEnum[] authorities, String[] usersWhiteList) {
        this.authorities = authorities;
        this.usersWhiteList = usersWhiteList;
    }

    public AuthorityEnum[] getAuthorities() {
        return authorities;
    }

    public String[] getUsersWhiteList() {
        return usersWhiteList;
    }
}
