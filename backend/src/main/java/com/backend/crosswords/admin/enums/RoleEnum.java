package com.backend.crosswords.admin.enums;

public enum RoleEnum {
    ADMIN(AuthorityEnum.values(), new String[]{"admin"}),
    MODERATOR(new AuthorityEnum[]{AuthorityEnum.EDIT_DELETE_DOCS}, new String[]{"Gek"});

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
