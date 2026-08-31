package com.saasplatform.ai.controller;

import com.saasplatform.ai.dto.*;
import com.saasplatform.ai.search.dto.*;
import com.saasplatform.ai.search.service.WebSearchService;
import com.saasplatform.ai.service.AiService;
import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final WebSearchService webSearchService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<AiGenerateResponse>> generate(
            @Valid @RequestBody AiGenerateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        AiGenerateResponse response = aiService.generateText(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "AI generation completed successfully"));
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiGenerateResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        AiGenerateResponse response = aiService.chat(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "AI chat response completed successfully"));
    }

    @PostMapping("/image/generate")
    public ResponseEntity<ApiResponse<AiImageResponse>> generateImage(
            @Valid @RequestBody AiImageRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        AiImageResponse response = aiService.generateImage(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Image generated successfully"));
    }

    @PostMapping(value = "/multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AiGenerateResponse>> multimodal(
            @RequestParam(value = "prompt", required = false, defaultValue = "") String prompt,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "model", required = false) String model,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        AiGenerateResponse response = aiService.processMultimodal(userPrincipal.getId(), prompt, files, model, Map.of());
        return ResponseEntity.ok(ApiResponse.success(response, "Multimodal inference completed successfully"));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<WebSearchResult>> search(
            @Valid @RequestBody WebSearchRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        WebSearchResult result = webSearchService.search(request.getQuery(), request.getMaxResults());
        return ResponseEntity.ok(ApiResponse.success(result, "Web search completed successfully"));
    }

    @PostMapping("/search/generate")
    public ResponseEntity<ApiResponse<AiSearchGenerateResponse>> searchAndGenerate(
            @Valid @RequestBody AiSearchGenerateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        AiSearchGenerateResponse response = webSearchService.searchAndSynthesize(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Web search and synthesis completed successfully"));
    }

    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getModels() {
        List<Map<String, String>> models = aiService.getAvailableModels();
        return ResponseEntity.ok(ApiResponse.success(models, "Available AI models retrieved"));
    }

    @GetMapping("/image/models")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getImageModels() {
        List<Map<String, String>> models = aiService.getAvailableImageModels();
        return ResponseEntity.ok(ApiResponse.success(models, "Available image models retrieved"));
    }
}
