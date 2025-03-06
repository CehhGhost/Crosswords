package com.backend.crosswords.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO for registering a user")
public class RegisterUserDTO {
    @Schema(
            example = "Гека"
    )
    private String name;

    @Schema(
            example = "Гека"
    )
    private String surname;

    @Schema(
            description = "User's login, can be used to login",
            example = "Geka"
    )
    private String username;

    @Schema(
            example = "12345"
    )
    private String password;

    @Schema(
            description = "User's email, can be used to login",
            example = "geka2003@mail.ru"
    )
    private String email;

    public RegisterUserDTO(String name, String surname, String username, String password, String email) {
        this.name = name;
        this.surname = surname;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public RegisterUserDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "RegisterUserDTO{" +
                "name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
