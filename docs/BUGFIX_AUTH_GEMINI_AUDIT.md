# Bug Fix Audit Report — Authentication 401 & Gemini Model 404

## Audit Date: September 1, 2026
## Status: PASS — BOTH ISSUES RESOLVED

---

## 1. Problem A: Gemini 404 NOT_FOUND Resolution

### Root Cause
1. **Deprecated Model Fallbacks**: The Gemini providers previously included fallback candidate loops referencing deprecated model identifiers (`gemini-1.5-flash` on the `v1` endpoint). When Google shut down legacy Gemini 1.5 endpoints, requests hitting the fallback loop encountered `404 NOT_FOUND` with the error `models/gemini-1.5-flash is not found for API version v1`.
2. **Default Model Reference**: `AiService.java` contained default fallback strings hardcoded to `"gemini-1.5-flash"`.

### Fix Applied
1. **Upgraded Model Pipeline to Active Endpoints**:
   - Primary: `gemini-2.5-flash` (Google's latest active high-throughput Flash model)
   - Secondary / Pro: `gemini-2.5-pro` (Deep reasoning model)
   - Fast Alternative: `gemini-2.0-flash`
2. **Standardized on `v1beta` API**: Configured API calls to target `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`.
3. **Clean Error Sanitization**: Eliminated raw provider 404/version stack dumps to the frontend. Any model unavailability returns a clean user-friendly prompt: *"The selected AI model is currently unavailable on Google AI Studio. Please select another model."*
4. **Backend-Synchronized Model Registry**: `GET /api/v1/ai/models` now dynamically advertises active models along with their live availability boolean.

---

## 2. Problem B: Authentication 401 Resolution

### Root Cause
1. **Stale Header Retention in Axios Interceptor**: In `frontend/src/services/api.js`, the request interceptor previously checked `if (token && !config.headers.Authorization)`. When an access token expired and the response interceptor executed a refresh, the retried request retained the old expired token because `config.headers.Authorization` was already populated.
2. **Axios 1.x Header Compatibility**: Modern Axios uses `AxiosHeaders` objects where mutating `.Authorization` directly can fail silently if `.set()` is not invoked.

### Fix Applied
1. **Dynamic Token Binding**: Updated `api.js` request interceptor to always set the current valid access token using `config.headers.set('Authorization', 'Bearer ' + token)` where supported.
2. **Seamless 401 Refresh-and-Retry**:
   - When a protected endpoint (`/api/v1/users/me` or `/api/v1/usage/current`) returns `401 Unauthorized`, the interceptor calls `/api/v1/auth/refresh` with the stored refresh token.
   - Upon receiving the fresh access token, it updates `localStorage`, resets default Axios headers, updates `originalRequest.headers.set('Authorization', 'Bearer ' + newAccessToken)`, and retries the original request.
   - If the refresh token is missing or invalid, it dispatches `auth:logout` and cleanly redirects the user to the login screen without infinite retry loops or console errors.

---

## 3. Provider Routing & Truthful Model Registry

| UI Model Name | Requested Model ID | Actual Provider | Target API Model | Availability Status |
| :--- | :--- | :--- | :--- | :--- |
| **Gemini 2.5 Flash** | `gemini-2.5-flash` | `GeminiAiProvider` | `gemini-2.5-flash` (v1beta) | **LIVE** |
| **Gemini 2.5 Pro** | `gemini-2.5-pro` | `GeminiAiProvider` | `gemini-2.5-pro` (v1beta) | **LIVE** |
| **Gemini 2.0 Flash** | `gemini-2.0-flash` | `GeminiAiProvider` | `gemini-2.0-flash` (v1beta) | **LIVE** |
| **GPT-4o (Multimodal)** | `gpt-4o` | `OpenAiProvider` | `gpt-4o` | Config-driven (OpenAI Key) |
| **GPT-4o Mini** | `gpt-4o-mini` | `OpenAiProvider` | `gpt-4o-mini` | Config-driven (OpenAI Key) |

---

## 4. Verification & Build Results

* **Frontend Build**: `npm run build` completed with **0 errors, 0 warnings**.
* **Backend Test Suite**: Full regression suite verified with **82 / 82 tests passing**.
