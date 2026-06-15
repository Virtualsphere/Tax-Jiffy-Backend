package com.gst_reconsilation.user.service;

import com.gst_reconsilation.user.dto.*;
import com.gst_reconsilation.config.security.JwtUtil;
import com.gst_reconsilation.user.entity.UserDetails;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserDetailsRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest req) {
        UserDetails user = userRepository
                .findByUserEmailAndIsActiveTrue(req.getEmail())
                .orElseThrow(() -> new RuntimeException("USER NOT FOUND IN DB"));

        if (!passwordEncoder.matches(req.getPassword(), user.getUserPassword())) {
            throw new RuntimeException("PASSWORD DOES NOT MATCH");
        }

        String token = jwtUtil.generate(user.getId(), user.getUserEmail());
        return new LoginResponse(token, user.getId(),
                user.getUserName(), user.getUserEmail());
    }
}
