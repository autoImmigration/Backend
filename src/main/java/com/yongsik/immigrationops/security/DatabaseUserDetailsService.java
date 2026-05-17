package com.yongsik.immigrationops.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserJpaRepository appUserJpaRepository;

    public DatabaseUserDetailsService(AppUserJpaRepository appUserJpaRepository) {
        this.appUserJpaRepository = appUserJpaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUserEntity user = appUserJpaRepository.findByUsername(username)
                .filter(appUser -> "ACTIVE".equalsIgnoreCase(appUser.getStatus()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles("OPS")
                .build();
    }
}
