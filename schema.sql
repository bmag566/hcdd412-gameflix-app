-- GameFlix auth schema (MySQL 8). Created by Hibernate (ddl-auto=update) from the
-- User and Role entities; follows the Module 3 ERD's auth.users / auth.roles / auth.user_roles.
-- Replace with the output of SHOW CREATE TABLE for each table after the first run.

CREATE TABLE roles (
  role_id    INT          NOT NULL AUTO_INCREMENT,
  role_name  VARCHAR(30)  NOT NULL,
  PRIMARY KEY (role_id),
  CONSTRAINT uk_roles_role_name UNIQUE (role_name)
);

CREATE TABLE users (
  user_id        BIGINT       NOT NULL AUTO_INCREMENT,
  email          VARCHAR(255) NOT NULL,
  password_hash  VARCHAR(255) NOT NULL,
  first_name     VARCHAR(100) NOT NULL,
  last_name      VARCHAR(100) NOT NULL,
  display_name   VARCHAR(50)  NOT NULL,
  status         ENUM('ACTIVE','SUSPENDED') NOT NULL,
  created_at     DATETIME(6)  NOT NULL,
  last_login_at  DATETIME(6)  NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT uk_users_email UNIQUE (email),
  CONSTRAINT uk_users_display_name UNIQUE (display_name)
);

CREATE TABLE user_roles (
  user_id  BIGINT NOT NULL,
  role_id  INT    NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (user_id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (role_id)
);

INSERT INTO roles (role_name) VALUES ('USER'), ('ADMIN');  -- done at startup by RoleSeeder
