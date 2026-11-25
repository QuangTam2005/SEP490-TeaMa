package com.asc.studycompanion.security;

import com.asc.studycompanion.module.auth.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Authenticated student principal stored in SecurityContextHolder.
 * getUsername() returns the numeric userId string so SecurityAuditorHelper
 * can extract created_by/updated_by without coupling common/ to module/auth/.
 */
@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private static final List<GrantedAuthority> DEFAULT_AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String fullName;

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return DEFAULT_AUTHORITIES;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return String.valueOf(id);
    }
}
