package org.sy.baemin.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.sy.baemin.user.domain.UserRole;

public record SignupRequest(

        @NotBlank @Email @Size(max = 255)
        String email,

        @NotBlank @Size(min = 8, max = 64)
        String password,

        // name: 비어있으면 안되고, 최대 길이는 SQL과 같게
        @NotBlank @Size(max = 50)
        String name,

        @NotNull
        UserRole role
) {
}
