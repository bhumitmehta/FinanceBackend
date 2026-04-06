package org.example.financebackend.security;

import org.example.financebackend.model.User;
import org.example.financebackend.model.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security UserDetails backed by the {@link User} entity.
 * <p>
 * Authorities are derived from every permission attached to every role the
 * user holds. Each permission is prefixed with {@code PERM_} so that
 * {@code @PreAuthorize("hasAuthority('PERM_records:write')")} works out of
 * the box without any custom voter configuration.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;
        this.authorities = user.getUserRoles().stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(p -> (GrantedAuthority) new SimpleGrantedAuthority("PERM_" + p.getName()))
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Exposes the underlying entity for services that need the full User object. */
    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }
}
