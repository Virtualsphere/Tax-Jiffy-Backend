package com.gst_reconsilation.whitebooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Single place that talks HTTP to Whitebooks. Every response is checked for status_cd and
 * the payload is read from the "data" wrapper Whitebooks puts around results.
 * Raw bodies are never logged in full because Whitebooks echoes client_secret back in "header".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WhitebooksClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** GET and return the full (already status-checked) response tree. */
    public JsonNode get(String url, HttpHeaders headers) {
        return exchange(url, HttpMethod.GET, new HttpEntity<>(headers));
    }

    /** PUT a JSON body and return the full (already status-checked) response tree. */
    public JsonNode put(String url, HttpHeaders headers, Object body) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        return exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers));
    }

    /** GET and map the "data" node to {@code type}. Returns null when the response has no data. */
    public <T> T getData(String url, HttpHeaders headers, Class<T> type) {
        return dataAs(get(url, headers), type);
    }

    public <T> T dataAs(JsonNode root, Class<T> type) {
        JsonNode data = root.get("data");
        if (data == null || data.isNull()) return null;
        try {
            return objectMapper.treeToValue(data, type);
        } catch (Exception e) {
            throw new WhitebooksApiException("WB_PARSE", "Unexpected response format from Whitebooks: " + e.getMessage());
        }
    }

    public <T> T rootAs(JsonNode root, Class<T> type) {
        try {
            return objectMapper.treeToValue(root, type);
        } catch (Exception e) {
            throw new WhitebooksApiException("WB_PARSE", "Unexpected response format from Whitebooks: " + e.getMessage());
        }
    }

    private JsonNode exchange(String url, HttpMethod method, HttpEntity<?> entity) {
        String body;
        try {
            body = restTemplate.exchange(url, method, entity, String.class).getBody();
        } catch (HttpStatusCodeException e) {
            body = e.getResponseBodyAsString();
            if (body == null || body.isBlank()) {
                throw new WhitebooksApiException("HTTP_" + e.getStatusCode().value(),
                        "Whitebooks returned HTTP " + e.getStatusCode().value());
            }
        }
        JsonNode root = parse(body);
        ensureSuccess(root);
        return root;
    }

    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            throw new WhitebooksApiException("WB_EMPTY", "Empty response from Whitebooks");
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new WhitebooksApiException("WB_PARSE", "Whitebooks response is not valid JSON");
        }
    }

    private void ensureSuccess(JsonNode root) {
        String statusCd = text(root, "status_cd");
        if ("1".equals(statusCd)) return;

        String code = null;
        String message = null;
        JsonNode error = root.get("error");
        if (error != null && error.isArray() && !error.isEmpty()) error = error.get(0);
        if (error != null && error.isObject()) {
            code = text(error, "errorCode");
            message = text(error, "errorMessage");
            if (message == null) message = text(error, "message");
        }
        if (message == null || message.equals(code)) {
            String desc = text(root, "status_desc");
            message = desc != null ? desc : (code != null ? "Error " + code : "Whitebooks request failed");
        }
        log.warn("Whitebooks call failed: status_cd={} code={} message={}", statusCd, code, message);
        throw new WhitebooksApiException(code, message);
    }

    static String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}
