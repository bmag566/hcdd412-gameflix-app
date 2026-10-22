package com.gameflix.gameflix.user;

import java.time.Instant;
import java.util.List;

public record UserResponse(Long userId, String email, String firstName, String lastName,
                           String displayName, AccountStatus status, List<String> roles,
                           Instant createdAt, Instant lastLoginAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
                u.getDisplayName(), u.getStatus(), u.getRoleNames(),
                u.getCreatedAt(), u.getLastLoginAt());
    }
}
