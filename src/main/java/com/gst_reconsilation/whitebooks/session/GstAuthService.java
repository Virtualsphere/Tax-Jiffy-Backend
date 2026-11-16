package com.gst_reconsilation.whitebooks.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.config.GstApiCredentialsProperties;
import com.gst_reconsilation.whitebooks.WhitebooksApiException;
import com.gst_reconsilation.whitebooks.WhitebooksClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

/**
 * GSTN authentication through Whitebooks:
 *   1. GET /authentication/otprequest  -> GSTN sends an OTP to the taxpayer, Whitebooks returns a txn
 *   2. GET /authentication/authtoken?otp=... (with that txn) -> session becomes active
 *   3. every other GST API call sends the same txn header (see {@link #headersFor(Integer)})
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GstAuthService {

    private final GstApiSessionRepository sessionRepository;
    private final CompanyGSTRepository companyGSTRepository;
    private final GstApiCredentialsProperties props;
    private final WhitebooksClient client;

    @Transactional
    public GstSessionDtos.SessionStatus requestOtp(Integer companyGstId, String gstUsername, Integer userId) {
        CompanyGST companyGST = companyGSTRepository.findById(companyGstId)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + companyGstId));
        String gstin = companyGST.getGstNumber();
        if (gstin == null || gstin.length() < 2) throw new RuntimeException("Company GST has no valid GSTIN");

        GstApiSession session = sessionRepository.findByCompanyGST_Id(companyGstId)
                .orElseGet(() -> GstApiSession.builder().companyGST(companyGST).build());

        String username = (gstUsername != null && !gstUsername.isBlank()) ? gstUsername.trim() : session.getGstUsername();
        if (username == null || username.isBlank()) {
            throw new RuntimeException("GST portal username is required to request an OTP");
        }

        session.setGstin(gstin);
        session.setGstUsername(username);
        session.setStateCd(gstin.substring(0, 2));

        String url = UriComponentsBuilder.fromHttpUrl(props.getBaseUrl() + "/authentication/otprequest")
                .queryParam("email", props.getEmail())
                .toUriString();
        JsonNode resp = client.get(url, baseHeaders(session));

        String txn = extractTxn(resp);
        if (txn == null) {
            throw new WhitebooksApiException("WB_NO_TXN", "OTP was requested but Whitebooks did not return a txn");
        }

        session.setTxn(txn);
        session.setStatus(GstApiSession.OTP_SENT);
        session.setOtpRequestedAt(LocalDateTime.now());
        session.setAuthenticatedAt(null);
        session.setExpiresAt(null);
        session.setUpdatedBy(userId);
        sessionRepository.save(session);

        log.info("GST OTP requested for GSTIN {}", gstin);
        return toStatus(session, companyGstId);
    }

    @Transactional
    public GstSessionDtos.SessionStatus verifyOtp(Integer companyGstId, String otp, Integer userId) {
        if (otp == null || otp.isBlank()) throw new RuntimeException("OTP is required");

        GstApiSession session = sessionRepository.findByCompanyGST_Id(companyGstId)
                .filter(s -> s.getTxn() != null)
                .orElseThrow(() -> new RuntimeException("Request an OTP first"));

        String url = UriComponentsBuilder.fromHttpUrl(props.getBaseUrl() + "/authentication/authtoken")
                .queryParam("email", props.getEmail())
                .queryParam("otp", otp.trim())
                .toUriString();
        HttpHeaders headers = baseHeaders(session);
        headers.set("txn", session.getTxn());
        JsonNode resp = client.get(url, headers);

        String txn = extractTxn(resp);
        if (txn != null) session.setTxn(txn);

        LocalDateTime now = LocalDateTime.now();
        session.setStatus(GstApiSession.ACTIVE);
        session.setAuthenticatedAt(now);
        session.setExpiresAt(now.plusMinutes(props.getSessionMinutes()));
        session.setUpdatedBy(userId);
        sessionRepository.save(session);

        log.info("GST session active for GSTIN {} until {}", session.getGstin(), session.getExpiresAt());
        return toStatus(session, companyGstId);
    }

    @Transactional(readOnly = true)
    public GstSessionDtos.SessionStatus status(Integer companyGstId) {
        return sessionRepository.findByCompanyGST_Id(companyGstId)
                .map(s -> toStatus(s, companyGstId))
                .orElseGet(() -> GstSessionDtos.SessionStatus.builder()
                        .companyGstId(companyGstId).status("NONE").active(false).build());
    }

    /**
     * Headers for a GST API call on behalf of this GSTIN, including the session txn.
     * Throws {@link GstSessionRequiredException} (HTTP 428) if there is no active session.
     */
    @Transactional(readOnly = true)
    public HttpHeaders headersFor(Integer companyGstId) {
        GstApiSession session = activeSession(companyGstId);
        HttpHeaders headers = baseHeaders(session);
        headers.set("txn", session.getTxn());
        return headers;
    }

    @Transactional(readOnly = true)
    public GstApiSession activeSession(Integer companyGstId) {
        GstApiSession session = sessionRepository.findByCompanyGST_Id(companyGstId).orElse(null);
        if (session == null || !session.isActive()) {
            throw new GstSessionRequiredException(
                    "GST portal session is not active for this GSTIN. Request an OTP and verify it first.");
        }
        return session;
    }

    /**
     * If a GST call failed because GSTN no longer accepts the session, mark it expired and
     * rethrow as {@link GstSessionRequiredException}; otherwise rethrow the original error.
     */
    @Transactional
    public RuntimeException handleCallFailure(Integer companyGstId, WhitebooksApiException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("auth token") || msg.contains("session expired") || msg.contains("invalid session")) {
            sessionRepository.findByCompanyGST_Id(companyGstId).ifPresent(s -> {
                s.setExpiresAt(LocalDateTime.now());
                sessionRepository.save(s);
            });
            return new GstSessionRequiredException("GST portal session has expired. Request a new OTP and verify it.");
        }
        return e;
    }

    private HttpHeaders baseHeaders(GstApiSession session) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("gst_username", session.getGstUsername());
        headers.set("state_cd", session.getStateCd());
        headers.set("ip_address", props.getIpAddress());
        headers.set("client_id", props.getClientId());
        headers.set("client_secret", props.getClientSecret());
        return headers;
    }

    /** Whitebooks returns the txn in the echoed "header" block; fall back to other plausible spots. */
    private String extractTxn(JsonNode resp) {
        for (JsonNode node : new JsonNode[]{resp.get("header"), resp, resp.get("data")}) {
            if (node != null && node.isObject() && node.hasNonNull("txn")) {
                String txn = node.get("txn").asText();
                if (!txn.isBlank()) return txn;
            }
        }
        return null;
    }

    private GstSessionDtos.SessionStatus toStatus(GstApiSession s, Integer companyGstId) {
        String status = s.getStatus();
        if (GstApiSession.ACTIVE.equals(status) && !s.isActive()) status = "EXPIRED";
        return GstSessionDtos.SessionStatus.builder()
                .companyGstId(companyGstId)
                .gstin(s.getGstin())
                .gstUsername(s.getGstUsername())
                .status(status)
                .active(s.isActive())
                .authenticatedAt(s.getAuthenticatedAt())
                .expiresAt(s.getExpiresAt())
                .build();
    }
}
