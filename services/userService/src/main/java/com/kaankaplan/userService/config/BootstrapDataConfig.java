package com.kaankaplan.userService.config;

import com.kaankaplan.userService.dao.ClaimDao;
import com.kaankaplan.userService.dao.UserDao;
import com.kaankaplan.userService.entity.Claim;
import com.kaankaplan.userService.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class BootstrapDataConfig {

    private final ClaimDao claimDao;
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner bootstrapUserData() {
        return args -> {
            Claim customerClaim = claimDao.getClaimByClaimName("CUSTOMER");
            if (customerClaim == null) {
                customerClaim = claimDao.save(Claim.builder().claimName("CUSTOMER").build());
            }

            Claim adminClaim = claimDao.getClaimByClaimName("ADMIN");
            if (adminClaim == null) {
                adminClaim = claimDao.save(Claim.builder().claimName("ADMIN").build());
            }

            if (userDao.findUserByEmail("admin@cinevision.local") == null) {
                userDao.save(User.builder()
                        .email("admin@cinevision.local")
                        .fullName("CineVision Admin")
                        .password(passwordEncoder.encode("Admin123!"))
                        .claim(adminClaim)
                        .build());
            }

            if (userDao.findUserByEmail("demo@cinevision.local") == null) {
                userDao.save(User.builder()
                        .email("demo@cinevision.local")
                        .fullName("Demo Customer")
                        .password(passwordEncoder.encode("Demo123!"))
                        .claim(customerClaim)
                        .build());
            }
        };
    }
}
