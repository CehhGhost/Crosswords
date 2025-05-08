package com.backend.crosswords.admin.models;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "_verifycode")
public class VerifyCode {

    @Column(name = "code")
    private Integer code;

    @Column(name = "expiration_date")
    private Timestamp expirationDate;

    @Id
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Timestamp getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Timestamp expirationDate) {
        this.expirationDate = expirationDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
