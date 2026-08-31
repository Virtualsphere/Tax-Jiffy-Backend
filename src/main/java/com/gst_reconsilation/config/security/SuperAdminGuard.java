package com.gst_reconsilation.config.security;

import com.gst_reconsilation.user.repository.UserDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SuperAdminGuard {

    private final UserDetailsRepository userDetailsRepository;

    public boolean isSuperAdmin(Integer userId) {
        if (userId == null) return false;
        return userDetailsRepository.findById(userId)
                .map(u -> Boolean.TRUE.equals(u.getIsSuperAdmin()))
                .orElse(false);
    }
}
