package com.saasplatform.user.controller;

import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.security.UserPrincipal;
import com.saasplatform.user.dto.ChangePasswordRequest;
import com.saasplatform.user.dto.SessionDto;
import com.saasplatform.user.dto.UpdateProfileRequest;
import com.saasplatform.user.dto.UserDto;
import com.saasplatform.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UserDto userDto = userService.getProfile(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(userDto, "User profile retrieved successfully"));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UserDto updated = userService.updateProfile(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Profile updated successfully"));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        userService.changePassword(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully. All other sessions revoked."));
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<ApiResponse<List<SessionDto>>> getSessions(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<SessionDto> sessions = userService.getActiveSessions(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(sessions, "Active sessions retrieved"));
    }

    @PostMapping("/me/sessions/revoke-all")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        userService.revokeAllSessions(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "All active sessions revoked"));
    }
}
