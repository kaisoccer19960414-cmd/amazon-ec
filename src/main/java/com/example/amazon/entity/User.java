package com.example.amazon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    /**
     * カード登録時にSMBCから発行されたトークン。
     * 生のカード番号はAmazon側では一切保持しない。
     * 未登録の場合はnull。
     */
    @Setter
    @Column(name = "smbc_token")
    private String smbcToken;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}