package com.gameflix.gameflix.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Makes sure the fixed role rows exist. ddl-auto creates tables but never inserts data. */
@Component
public class RoleSeeder implements ApplicationRunner {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    private final RoleRepository roles;

    public RoleSeeder(RoleRepository roles) {
        this.roles = roles;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String name : List.of(USER, ADMIN)) {
            if (roles.findByName(name).isEmpty()) {
                roles.save(new Role(name));
            }
        }
    }
}
