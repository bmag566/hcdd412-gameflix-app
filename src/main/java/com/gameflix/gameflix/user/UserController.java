package com.gameflix.gameflix.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        Long id = Long.valueOf(jwt.getSubject());
        return users.findById(id)
                .filter(User::isActive)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
