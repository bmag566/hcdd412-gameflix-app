package com.gameflix.gameflix.user;

import jakarta.persistence.*;

@Entity
@Table(name = "roles",
       uniqueConstraints = @UniqueConstraint(name = "uk_roles_role_name", columnNames = "role_name"))
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer id;

    @Column(name = "role_name", nullable = false, length = 30)
    private String name;

    protected Role() {}

    public Role(String name) {
        this.name = name;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
}
