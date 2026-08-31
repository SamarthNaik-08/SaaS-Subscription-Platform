package com.saasplatform.user.dto;

import com.saasplatform.refresh.entity.RefreshToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    private UUID id;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean revoked;

    public static SessionDto fromEntity(RefreshToken token) {
        if (token == null) return null;
        return SessionDto.builder()
                .id(token.getId())
                .createdAt(token.getCreatedAt())
                .expiresAt(token.getExpiresAt())
                .revoked(token.isRevoked())
                .build();
    }
}
