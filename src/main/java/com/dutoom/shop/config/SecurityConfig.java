package com.dutoom.shop.config;

import com.dutoom.shop.domain.Member;
import com.dutoom.shop.domain.Repos.MemberRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/mypage/**", "/reviews/write/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(f -> f.loginPage("/login").usernameParameter("email").defaultSuccessUrl("/", false).permitAll())
                .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/"))
                .headers(h -> h.frameOptions(fo -> fo.sameOrigin()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(MemberRepo members) {
        return email -> members.findByEmail(email)
                .map(m -> User.withUsername(m.getEmail()).password(m.getPassword()).roles(m.getRole()).build())
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }

    /** ADMIN_EMAIL/ADMIN_PASSWORD 가 설정되어 있고 해당 계정이 없을 때만 관리자 생성 (기존 계정은 건드리지 않음) */
    @Bean
    ApplicationRunner adminBootstrap(ShopProperties props, MemberRepo members, PasswordEncoder encoder) {
        return args -> {
            var c = props.company();
            log.info("사업자 정보: 상호={}, 대표={}, 사업자번호={}, 연락처={}", c.name(), c.ceo(), c.businessNo(), c.phone());
            var a = props.admin();
            if (a == null || !StringUtils.hasText(a.email()) || !StringUtils.hasText(a.password())) {
                log.info("관리자 계정: ADMIN_EMAIL/ADMIN_PASSWORD 미설정 → 생성 안 함");
                return;
            }
            if (members.findByEmail(a.email()).isPresent()) {
                log.info("관리자 계정: {} 이미 있음 (비밀번호 변경 안 함)", a.email());
                return;
            }
            Member m = new Member();
            m.setEmail(a.email());
            m.setPassword(encoder.encode(a.password()));
            m.setName("관리자");
            m.setRole("ADMIN");
            members.save(m);
            log.info("관리자 계정 생성: {}", a.email());
        };
    }
}
