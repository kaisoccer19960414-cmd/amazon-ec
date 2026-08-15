package com.example.amazon.service;

import com.example.amazon.dto.request.SignupRequest;
import com.example.amazon.entity.User;
import com.example.amazon.exception.UsernameAlreadyExistsException;
import com.example.amazon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }

        String encoded = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getUsername(), encoded);
        return userRepository.save(user);
    }

    /**
     * SMBCで発行されたトークンを、ログイン中ユーザーのカード情報として保存する。
     * 生のカード番号はここでも一切扱わない。
     */
    @Transactional
    public void registerCardToken(Long userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));
        user.setSmbcToken(token);
        userRepository.save(user);
    }
}