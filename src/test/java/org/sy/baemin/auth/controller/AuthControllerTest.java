package org.sy.baemin.auth.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.sy.baemin.auth.dto.SignupResponse;
import org.sy.baemin.common.BusinessException;
import org.sy.baemin.common.ErrorCode;
import org.sy.baemin.user.domain.UserRole;
import org.sy.baemin.user.service.UserService;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러만 띄워서 요청·응답 형식과 입력값 검증을 확인한다.
 * UserService는 가짜(Mock)로 바꿔서, DB 없이 "서비스가 이렇게 답하면 컨트롤러는 이렇게 응답한다"만 본다.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    private static final String VALID_BODY = """
            {"email":"kim@baemin.com","password":"pass1234!","name":"김배민","role":"CUSTOMER"}
            """;

    @Test
    @DisplayName("회원가입에 성공하면 201과 회원 정보를 응답한다 (ID는 문자열)")
    void signupSuccess() throws Exception {
        given(userService.signup(any()))
                .willReturn(new SignupResponse("1234567890123456789", "kim@baemin.com", "김배민", UserRole.CUSTOMER));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1234567890123456789"))
                .andExpect(jsonPath("$.email").value("kim@baemin.com"))
                .andExpect(jsonPath("$.password").doesNotExist());   // 비밀번호는 응답에 없어야 함
    }

    @Test
    @DisplayName("이메일 형식이 틀리면 400이고, 서비스는 호출되지 않는다")
    void invalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"pass1234!","name":"김배민","role":"CUSTOMER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("email: ")));

        verify(userService, never()).signup(any());
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 400이다")
    void shortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"kim@baemin.com","password":"short","name":"김배민","role":"CUSTOMER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("password: ")));
    }

    @Test
    @DisplayName("비밀번호는 이메일 형식이 아니어도 가입할 수 있다")
    void passwordDoesNotNeedEmailFormat() throws Exception {
        given(userService.signup(any()))
                .willReturn(new SignupResponse("1", "kim@baemin.com", "김배민", UserRole.CUSTOMER));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 409와 DUPLICATE_EMAIL로 응답한다")
    void duplicateEmail() throws Exception {
        given(userService.signup(any())).willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }
}
