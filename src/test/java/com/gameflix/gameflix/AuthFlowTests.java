package com.gameflix.gameflix;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTests {

    private static final String PASSWORD = "Throwaway123!";

    @Autowired
    private MockMvc mvc;

    // 1. Successful registration
    @Test
    void registerReturns201AndNoHash() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, PASSWORD, uniqueDisplayName())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // 2. Duplicate email rejected
    @Test
    void duplicateEmailReturns409() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, PASSWORD, uniqueDisplayName())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }

    // 2b. Duplicate display name rejected (unique in the Module 3 ERD)
    @Test
    void duplicateDisplayNameReturns409() throws Exception {
        String displayName = uniqueDisplayName();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueEmail(), PASSWORD, displayName)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueEmail(), PASSWORD, displayName)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Display name already taken"));
    }

    // 3. Successful login returns a token
    @Test
    void loginReturns200WithToken() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    // 4. Wrong password
    @Test
    void wrongPasswordReturns401Generic() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "WrongPassword1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    // 4b. Unknown email gets the identical response
    @Test
    void unknownEmailReturnsSameGeneric401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(uniqueEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    // 5. Protected call with a valid token
    @Test
    void meWithValidTokenReturnsProfile() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        String token = loginAndGetToken(email, PASSWORD);
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.lastLoginAt").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // 6a. Protected call with no token
    @Test
    void meWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // 6b. Protected call with a tampered token (signature altered)
    @Test
    void meWithTamperedTokenReturns401() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        String token = loginAndGetToken(email, PASSWORD);
        String tampered = token.substring(0, token.length() - 5) + "AAAAA";
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer error=\"invalid_token\"")));
    }

    // Extra: invalid input
    @Test
    void invalidRegistrationReturns400() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short","firstName":"","lastName":"User","displayName":"x"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists())
                .andExpect(jsonPath("$.fields.firstName").exists());
    }

    // --- helpers ---

    private static String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    private static String uniqueDisplayName() {
        return "test-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private static String registerJson(String email, String password, String displayName) {
        return """
                {"email":"%s","password":"%s","firstName":"Test","lastName":"User","displayName":"%s"}
                """.formatted(email, password, displayName);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private void register(String email, String password) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, password, uniqueDisplayName())))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
