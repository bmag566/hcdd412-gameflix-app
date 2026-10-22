package com.gameflix.gameflix.auth;

import com.gameflix.gameflix.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest req) {
        return UserResponse.from(auth.register(req));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }
}
