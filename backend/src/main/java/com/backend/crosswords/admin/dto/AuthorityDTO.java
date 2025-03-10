package com.backend.crosswords.admin.dto;

import java.util.ArrayList;
import java.util.List;

public class AuthorityDTO {
    private List<String> authorities;

    public AuthorityDTO() {
        authorities = new ArrayList<>();
    }

    public AuthorityDTO(List<String> authorities) {
        this.authorities = authorities;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }
}
