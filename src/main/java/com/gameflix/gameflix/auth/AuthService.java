package com.gameflix.gameflix.auth;

import com.gameflix.gameflix.user.Role;
import com.gameflix.gameflix.user.RoleRepository;
import com.gameflix.gameflix.user.RoleSeeder;
import com.gameflix.gameflix.user.User;
import com.gameflix.gameflix.user.UserRepository;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final String dummyHash;

    public AuthService(UserRepository users, RoleRepository roles,
                       PasswordEncoder encoder, TokenService tokens) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.tokens = tokens;
        this.dummyHash = encoder.encode("timing-equalizer-not-a-real-password");
    }

    // Deliberately NOT @Transactional: saveAndFlush must run the INSERT inside this try
    // so a unique-constraint violation is caught here and mapped to 409.
    public User register(RegisterRequest req) {
        Role userRole = roles.findByName(RoleSeeder.USER)
                .orElseThrow(() -> new IllegalStateException("USER role missing"));
        User user = new User(
                normalize(req.email()),
                encoder.encode(req.password()),
                req.firstName().trim(),
                req.lastName().trim(),
                req.displayName().trim(),
                userRole);
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            String msg = String.valueOf(NestedExceptionUtils.getMostSpecificCause(e).getMessage());
            if (msg.contains("uk_users_email")) {
                throw new EmailAlreadyRegisteredException();
            }
            if (msg.contains("uk_users_display_name")) {
                throw new DisplayNameTakenException();
            }
            throw e;
        }
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = users.findByEmail(normalize(req.email())).orElse(null);
        if (user == null) {
            encoder.matches(req.password(), dummyHash);
            throw new InvalidCredentialsException();
        }
        if (!encoder.matches(req.password(), user.getPasswordHash()) || !user.isActive()) {
            throw new InvalidCredentialsException();
        }
        user.recordLogin(Instant.now());
        return tokens.issue(user);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
