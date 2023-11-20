package com.upemail.registration.security;

import com.upemail.registration.entity.User;
import com.upemail.registration.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class EmailPasswordFilter implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String pwd = authentication.getCredentials().toString();
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isEmpty()) {
            throw new BadCredentialsException("No user registered with this details!");
        }
        if (passwordEncoder.matches(pwd, user.get().getPassword())) {
            return new UsernamePasswordAuthenticationToken(email, pwd, List.of(new SimpleGrantedAuthority(user.get().getRole().name())));
        } else {
            throw new BadCredentialsException("Invalid password!");
        }
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
