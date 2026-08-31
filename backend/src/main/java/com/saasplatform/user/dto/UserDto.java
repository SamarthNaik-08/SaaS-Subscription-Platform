package com.saasplatform.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.saasplatform.common.enums.GlobalRole;
import com.saasplatform.common.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private GlobalRole role;
    private boolean emailVerified;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
