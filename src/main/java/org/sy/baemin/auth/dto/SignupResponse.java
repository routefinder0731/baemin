package org.sy.baemin.auth.dto;

import org.sy.baemin.user.domain.User;
import org.sy.baemin.user.domain.UserRole;

public record SignupResponse(String id, String email, String name, UserRole role) {

    public static SignupResponse from(User user) {
        return new SignupResponse(
                // User에서 값을 꺼내 채우기 (id는 문자열로)
                user.getId().toString(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }

}
