package com.team1.cityfarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.cityfarm.dto.SignupRequestDto;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F-20/21/22 팔로우, F-30 피드 목록 조회, F-50 좋아요한 게시글 목록 조회 기능에 대한
 * 실제 HTTP 요청/응답 흐름 검증용 통합 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FollowFeedLikeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String signupAndLogin(String email, String nickname) throws Exception {
        SignupRequestDto signup = new SignupRequestDto();
        signup.setEmail(email);
        signup.setPassword("Password123!");
        signup.setPasswordConfirm("Password123!");
        signup.setNickname(nickname);
        signup.setName(nickname);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private Long userId(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return user.getId();
    }

    private Long createBoard(String token, String title) throws Exception {
        MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                MediaType.APPLICATION_JSON_VALUE,
                ("{\"title\":\"" + title + "\",\"content\":\"content-" + title + "\",\"category\":\"FREE\"}")
                        .getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/api/board")
                        .file(request)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("id").asLong();
    }

    @Test
    void follow_feed_like_flow() throws Exception {
        String tokenA = signupAndLogin("usera@cityfarm.com", "userA");
        String tokenB = signupAndLogin("userb@cityfarm.com", "userB");
        Long userBId = userId("userb@cityfarm.com");

        // ---------- 인증 없이 접근 시 401 ----------
        mockMvc.perform(get("/api/feed"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/profile/likes"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followingId\":" + userBId + "}"))
                .andExpect(status().isUnauthorized());

        // ---------- F-20 팔로우 등록 (A -> B) ----------
        mockMvc.perform(post("/api/follows")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followingId\":" + userBId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // 자기 자신 팔로우 -> 400 계열 예외
        mockMvc.perform(post("/api/follows")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followingId\":" + userBId + "}"))
                .andExpect(status().is4xxClientError()); // 이미 팔로우한 사용자 (409)

        // ---------- F-21 팔로잉/팔로워 목록 조회 ----------
        MvcResult followingResult = mockMvc.perform(get("/api/follows/following")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nickname").value("userB"))
                .andReturn();

        MvcResult followerResult = mockMvc.perform(get("/api/follows/followers")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nickname").value("userA"))
                .andReturn();

        JsonNode followingJson = objectMapper.readTree(followingResult.getResponse().getContentAsString());
        Long followId = followingJson.get("data").get(0).get("followId").asLong();

        JsonNode followerJson = objectMapper.readTree(followerResult.getResponse().getContentAsString());
        assertThat(followerJson.get("data").size()).isEqualTo(1);

        // ---------- B가 게시글 2개 작성 ----------
        Long boardId1 = createBoard(tokenB, "글1");
        Long boardId2 = createBoard(tokenB, "글2");

        // ---------- F-30 피드 목록 조회 (A가 팔로우한 B의 글이 보여야 함) ----------
        mockMvc.perform(get("/api/feed")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].writer").value("userB"));

        // 팔로우하지 않은 B 입장에서 피드는 비어있어야 함
        mockMvc.perform(get("/api/feed")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // ---------- A가 boardId1에 좋아요 ----------
        mockMvc.perform(post("/api/board/" + boardId1 + "/likes")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        // ---------- F-50 좋아요한 게시글 목록 조회 ----------
        mockMvc.perform(post("/api/profile/likes")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(boardId1))
                .andExpect(jsonPath("$.data.content[0].title").value("글1"));

        // B는 좋아요한 글이 없어야 함
        mockMvc.perform(post("/api/profile/likes")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // ---------- 좋아요 취소 후 목록에서 사라지는지 확인 ----------
        mockMvc.perform(delete("/api/board/" + boardId1 + "/likes")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false));

        mockMvc.perform(post("/api/profile/likes")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // ---------- F-22 팔로우 취소 ----------
        mockMvc.perform(delete("/api/follows/" + followId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/feed")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // 남의 팔로우 취소 시도 -> 예외 (B가 A의 팔로우 id로 취소 시도 -> 이미 삭제되어 NOT_FOUND)
        mockMvc.perform(delete("/api/follows/" + followId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().is4xxClientError());
    }
}
