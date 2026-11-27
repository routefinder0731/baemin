package org.sy.baemin.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "가게를 찾을 수 없습니다."),

    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),
    MENU_SOLD_OUT(HttpStatus.CONFLICT, "메뉴가 소진되었습니다."),

    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니에 해당 메뉴가 없습니다."),
    CART_STORE_MISMATCH(HttpStatus.CONFLICT, "장바구니에는 한 가게의 메뉴만 담을 수 있습니다."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");


    private final HttpStatus status;
    private final String message;

}
