package com.example.amazon.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("そのユーザー名は既に使われています: " + username);
    }
}