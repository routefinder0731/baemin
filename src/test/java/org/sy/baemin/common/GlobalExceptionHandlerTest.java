package org.sy.baemin.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler 가 예외 종류별로 올바른 상태 코드와 응답 형식을 돌려주는지 검증한다.
 * <p>
 * {@code @WebMvcTest}는 웹 계층(컨트롤러, ControllerAdvice)만 띄우는 테스트라 DB 없이 빠르게 실행된다.
 * 예외를 일부러 던지는 테스트 전용 컨트롤러를 만들어 핸들러를 호출한다.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("BusinessException은 ErrorCode에 정의된 상태 코드와 메시지로 응답한다")
    void businessException() throws Exception {
        mockMvc.perform(get("/test/sold-out"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MENU_SOLD_OUT"))
                .andExpect(jsonPath("$.message").value(ErrorCode.MENU_SOLD_OUT.getMessage()));
    }

    @Test
    @DisplayName("@Valid 검증에 실패하면 400과 '필드이름: 오류 메시지'로 응답한다")
    void validationFailed() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(startsWith("quantity: ")));
    }

    @Test
    @DisplayName("필수 헤더가 없으면 400과 빠진 헤더 이름으로 응답한다")
    void missingHeader() throws Exception {
        mockMvc.perform(get("/test/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("필수 헤더가 없습니다: Idempotency-Key"));
    }

    @Test
    @DisplayName("JSON 형식이 틀리면 400과 고정 메시지로 응답하고, 내부 정보를 노출하지 않는다")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": "))   // 닫히지 않은 JSON
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("요청된 본문이 없거나 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.message").value(not(containsString("Exception"))));
    }

    @Test
    @DisplayName("요청 본문이 아예 없어도 같은 400 응답을 한다")
    void missingBody() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 500과 일반 메시지로 응답하고, 예외 메시지를 노출하지 않는다")
    void unexpectedException() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()))
                .andExpect(jsonPath("$.message").value(not(containsString("secret"))));
    }

    // ---------- 테스트 전용 컨트롤러: 각 상황의 예외를 일부러 발생시킨다 ----------

    record QuantityRequest(@Min(1) int quantity) {
    }

    @RestController
    static class TestController {

        @GetMapping("/test/sold-out")
        void soldOut() {
            throw new BusinessException(ErrorCode.MENU_SOLD_OUT);
        }

        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody QuantityRequest request) {
        }

        @GetMapping("/test/header")
        void header(@RequestHeader("Idempotency-Key") String key) {
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            // 내부 정보가 담긴 예외 메시지가 응답으로 새어 나가지 않는지 확인하기 위한 값
            throw new IllegalStateException("db password=secret");
        }
    }
}
