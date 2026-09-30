package ru.itmo.securityapi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.itmo.securityapi.data.PostRepository;
import ru.itmo.securityapi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void cleanDatabase() {
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void protectedEndpointRejectsRequestWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/data"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationLoginAndProtectedDataFlowWorks() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student","password":"StrongPassword1!",\
                                "displayName":"<script>alert(1)</script>"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName", containsString("&lt;script&gt;")))
                .andExpect(jsonPath("$.displayName", not(containsString("<script>"))));

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student","password":"StrongPassword1!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andReturn().getResponse().getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String token = loginJson.get("token").asText();

        mockMvc.perform(post("/api/data")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Security <b>note</b>",\
                                "content":"<img src=x onerror=alert(1)>"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", containsString("&lt;b&gt;")))
                .andExpect(jsonPath("$.content", containsString("&lt;img")));

        mockMvc.perform(get("/api/data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].author").value("student"));
    }

    @Test
    void sqlInjectionStyleLoginDoesNotBypassAuthentication() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"' OR '1'='1","password":"anything123!"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
