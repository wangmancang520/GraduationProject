package com.example.springboot;

import com.example.springboot.entity.User;
import com.example.springboot.service.UserService;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class SpringbootApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserService users;

    private String token(String login) throws Exception {
        String body = mvc.perform(post("/login").contentType("application/json")
                .content("{\"uloginName\":\"" + login + "@example.com\",\"upassword\":\"Demo12345\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return json.readTree(body).path("data").path("token").asText();
    }
    @Test void requiresAuthenticationAndHandlesMalformedTokens() throws Exception {
        mvc.perform(get("/classroom")).andExpect(status().isUnauthorized());
        mvc.perform(get("/classroom").header("token", "e30.e30.invalid"))
                .andExpect(status().isUnauthorized());
    }
    @Test void registrationIsPublicAndCannotEscalateRoles() throws Exception {
        mvc.perform(post("/user/register").contentType("application/json")
                .content("{\"uloginName\":\"new@example.com\",\"upassword\":\"NewPass123\",\"utype\":4}"))
                .andExpect(jsonPath("$.code").value("200"));
        User user = users.list().stream().filter(u -> "new@example.com".equals(u.getULoginName())).findFirst().orElseThrow();
        assertEquals(1, user.getUType());
        assertTrue(user.getUPassword().startsWith("$2"));
        assertNotEquals("NewPass123", user.getUPassword());
    }
    @Test void passwordHashesNeverAppearInUserResponses() throws Exception {
        mvc.perform(get("/user/3").header("token", token("student")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.upassword").doesNotExist());
        mvc.perform(get("/user/page").param("pageNum", "1").param("pageSize", "10").header("token", token("manager")))
                .andExpect(jsonPath("$.data.records[0].upassword").doesNotExist());
    }
    @Test void studentCannotModifyAnotherUserOrReadTheirMessages() throws Exception {
        String student = token("student");
        mvc.perform(post("/user").header("token", student).contentType("application/json").content("{\"uid\":1,\"utype\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/message/get-by-receiver/1").header("token", student)).andExpect(status().isForbidden());
        mvc.perform(post("/user").header("token", student).contentType("application/json").content("{\"uid\":3,\"utype\":4}"))
                .andExpect(status().isOk());
        assertEquals(1, users.getById(3).getUType());
    }
    @Test void bookingApprovalAndNotificationsWorkTogether() throws Exception {
        String student = token("student");
        mvc.perform(post("/appointment/new").header("token", student).param("uid", "3").param("cid", "1")
                .param("date", "2035-01-02").param("startTime", "2035-01-02T01:00:00.000Z").param("endTime", "2035-01-02T02:00:00.000Z"))
                .andExpect(jsonPath("$.code").value("200"));
        String response = mvc.perform(get("/appointment/page").header("token", student).param("pageNum", "1").param("pageSize", "10"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode page = json.readTree(response).path("data");
        assertEquals(1, page.path("total").asInt());
        assertEquals("明理楼101", page.path("records").get(0).path("cname").asText());
        int aid = page.path("records").get(0).path("aid").asInt();
        mvc.perform(post("/appointment/approve/" + aid).header("token", student)).andExpect(status().isForbidden());
        mvc.perform(post("/appointment/approve/" + aid).header("token", token("teacher"))).andExpect(jsonPath("$.code").value("200"));
        mvc.perform(get("/message/get-unread-num/3").header("token", student)).andExpect(jsonPath("$.data").value(2));
    }
    @Test void passwordChangeRequiresOldPasswordAndRevokesOldToken() throws Exception {
        String student = token("student");
        mvc.perform(post("/user/change-password").header("token", student).contentType("application/json")
                .content("{\"uid\":3,\"oldPassword\":\"wrong\",\"newPassword\":\"NewPass123\"}"))
                .andExpect(jsonPath("$.code").value("600"));
        mvc.perform(post("/user/change-password").header("token", student).contentType("application/json")
                .content("{\"uid\":3,\"oldPassword\":\"Demo12345\",\"newPassword\":\"NewPass123\"}"))
                .andExpect(jsonPath("$.code").value("200"));
        mvc.perform(get("/classroom").header("token", student)).andExpect(status().isUnauthorized());
    }
    @Test void emptyDatabaseStatisticsAreFinite() throws Exception {
        mvc.perform(get("/classroom/active-ratio").header("token", token("manager"))).andExpect(jsonPath("$.data").value(0.0));
        mvc.perform(get("/appointment/ocpt-ratio-1").header("token", token("manager"))).andExpect(jsonPath("$.data").value(0.0));
    }
}
