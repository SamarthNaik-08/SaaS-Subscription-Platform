package com.saasplatform.refresh.repository;

import com.saasplatform.refresh.entity.RefreshToken;
import com.saasplatform.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
    void revokeAllUserTokens(@Param("user") User user);

    java.util.List<RefreshToken> findByUserIdOrderByCreatedAtDesc(UUID userId);

    java.util.List<RefreshToken> findByUserIdAndRevokedFalseOrderByCreatedAtDesc(UUID userId);
}
