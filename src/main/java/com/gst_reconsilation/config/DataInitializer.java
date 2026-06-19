package com.gst_reconsilation.config;

import com.gst_reconsilation.roles.entity.Roles;
import com.gst_reconsilation.roles.repository.RolesRepository;
import com.gst_reconsilation.user.entity.UserDetails;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolesRepository rolesRepo;
    private final UserDetailsRepository userRepo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        // Seed roles
        if (rolesRepo.findByRoleNameAndIsActiveTrue("SUPER_ADMIN").isEmpty()) {
            rolesRepo.save(Roles.builder().roleName("SUPER_ADMIN").description("Platform owner").build());
        }
        if (rolesRepo.findByRoleNameAndIsActiveTrue("ADMIN").isEmpty()) {
            rolesRepo.save(Roles.builder().roleName("ADMIN").description("Subscription buyer").build());
        }
        if (rolesRepo.findByRoleNameAndIsActiveTrue("USER").isEmpty()) {
            rolesRepo.save(Roles.builder().roleName("USER").description("Regular user").build());
        }

        // Seed super admin user
        if (userRepo.findByUserEmailAndIsActiveTrue("superadmin@gst.com").isEmpty()) {
            userRepo.save(UserDetails.builder()
                    .userName("Super Admin")
                    .userEmail("superadmin@gst.com")
                    .userPassword(encoder.encode("ChangeMe@123"))
                    .isSuperAdmin(true)
                    .build());
        }
    }
}
