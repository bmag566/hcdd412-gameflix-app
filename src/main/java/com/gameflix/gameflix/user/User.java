package com.gameflix.gameflix.user;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
           @UniqueConstraint(name = "uk_users_display_name", columnNames = "display_name")
       })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected User() {}

    public User(String email, String passwordHash, String firstName, String lastName,
                String displayName, Role initialRole) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.roles.add(initialRole);
    }

    public void recordLogin(Instant when) {
        this.lastLoginAt = when;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public List<String> getRoleNames() {
        return roles.stream().map(Role::getName).sorted().toList();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDisplayName() { return displayName; }
    public AccountStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Set<Role> getRoles() { return roles; }
}
