package org.example.financebackend.security;

import lombok.RequiredArgsConstructor;
import org.example.financebackend.repository.UserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Primary {@link UserDetailsService} implementation.
 * <p>
 * Uses the {@code @EntityGraph} query on {@link UserRepository} to fetch
 * the user together with all roles and their permissions in a single SQL
 * join — eliminating N+1 queries on every authenticated request.
 */
@Primary
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findWithRolesAndPermissionsByEmail(email)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
