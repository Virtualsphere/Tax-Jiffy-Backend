package com.gst_reconsilation.config;

import com.gst_reconsilation.roles.entity.Roles;
import com.gst_reconsilation.roles.repository.RolesRepository;
import com.gst_reconsilation.subscription.entity.SubscriptionPlan;
import com.gst_reconsilation.subscription.repository.SubscriptionPlanRepository;
import com.gst_reconsilation.user.entity.UserDetails;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;


@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolesRepository rolesRepo;
    private final UserDetailsRepository userRepo;
    private final SubscriptionPlanRepository subscriptionPlanRepo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        Roles superAdminRole = rolesRepo.findByRoleNameAndCompanyGSTIsNullAndIsActiveTrue("SUPER_ADMIN")
                .orElseGet(() -> rolesRepo.save(Roles.builder()
                        .roleName("SUPER_ADMIN").description("Platform owner").build()));

        if (userRepo.findByUserEmailAndIsActiveTrue("superadmin@gst.com").isEmpty()) {
            userRepo.save(UserDetails.builder()
                    .userName("Super Admin")
                    .userEmail("superadmin@gst.com")
                    .userPassword(encoder.encode("ChangeMe@123"))
                    .isSuperAdmin(true)
                    .role(superAdminRole)
                    .build());
        }

        seedSubscriptionPlans();
    }

    // Plans are otherwise only ever created by hand through the Super Admin
    // screen, which leaves a fresh database with nothing to sell: the Buy
    // Subscription dropdown comes up empty. Seed the starter tiers so every
    // environment has something selectable.
    private void seedSubscriptionPlans() {
        if (!subscriptionPlanRepo.findByIsActiveTrue().isEmpty()) {
            return;
        }

        subscriptionPlanRepo.saveAll(List.of(
                SubscriptionPlan.builder()
                        .name("Basic")
                        .userCount(1)
                        .transactionCount(500)
                        .planAmount(new BigDecimal("999.00"))
                        .build(),
                SubscriptionPlan.builder()
                        .name("Professional")
                        .userCount(5)
                        .transactionCount(5000)
                        .planAmount(new BigDecimal("4999.00"))
                        .build(),
                SubscriptionPlan.builder()
                        .name("Enterprise")
                        .userCount(25)
                        .transactionCount(50000)
                        .planAmount(new BigDecimal("14999.00"))
                        .build()));
    }
}