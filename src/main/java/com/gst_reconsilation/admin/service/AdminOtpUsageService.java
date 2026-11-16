package com.gst_reconsilation.admin.service;

import com.gst_reconsilation.admin.dto.OtpUsageDtos;
import com.gst_reconsilation.apiusage.repository.GstOtpEventRepository;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.user.entity.UserDetails;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import com.gst_reconsilation.whitebooks.session.GstApiSession;
import com.gst_reconsilation.whitebooks.session.GstApiSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Super-admin view of GST portal OTP usage (each OTP request makes GSTN send an SMS). */
@Service
@RequiredArgsConstructor
public class AdminOtpUsageService {

    private final GstOtpEventRepository eventRepository;
    private final CompanyGSTRepository companyGSTRepository;
    private final GstApiSessionRepository sessionRepository;
    private final UserDetailsRepository userDetailsRepository;

    /** Every GSTIN with its OTP counts for the month (zeros included), busiest first. */
    @Transactional(readOnly = true)
    public List<OtpUsageDtos.Summary> summary(String period) {
        YearMonth month = parsePeriod(period);
        Map<Integer, Object[]> counts = eventRepository
                .summarize(month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay())
                .stream().collect(Collectors.toMap(r -> (Integer) r[0], r -> r));
        Map<Integer, GstApiSession> sessions = sessionRepository.findAll().stream()
                .filter(s -> s.getCompanyGST() != null)
                .collect(Collectors.toMap(s -> s.getCompanyGST().getId(), Function.identity(), (a, b) -> a));

        return companyGSTRepository.findAll().stream()
                .map(gst -> toSummary(gst, month.toString(), counts.get(gst.getId()), sessions.get(gst.getId())))
                .sorted(Comparator.comparingLong(OtpUsageDtos.Summary::getOtpRequests).reversed()
                        .thenComparing(OtpUsageDtos.Summary::getGstNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /** Individual OTP requests/verifications of one GSTIN in the month, newest first. */
    @Transactional(readOnly = true)
    public List<OtpUsageDtos.Event> events(Integer companyGstId, String period) {
        YearMonth month = parsePeriod(period);
        var events = eventRepository.findByCompanyGST_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                companyGstId, month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());

        Set<Integer> userIds = events.stream().map(e -> e.getUserId()).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, UserDetails> users = userDetailsRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserDetails::getId, Function.identity()));

        return events.stream().map(e -> {
            UserDetails u = e.getUserId() != null ? users.get(e.getUserId()) : null;
            return OtpUsageDtos.Event.builder()
                    .id(e.getId())
                    .createdAt(e.getCreatedAt())
                    .action(e.getAction())
                    .success(e.isSuccess())
                    .message(e.getMessage())
                    .userId(e.getUserId())
                    .userName(u != null ? u.getUserName() : null)
                    .userEmail(u != null ? u.getUserEmail() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    private OtpUsageDtos.Summary toSummary(CompanyGST gst, String periodKey, Object[] row, GstApiSession session) {
        String sessionStatus = "NONE";
        if (session != null) {
            sessionStatus = GstApiSession.ACTIVE.equals(session.getStatus()) && !session.isActive()
                    ? "EXPIRED" : session.getStatus();
        }
        return OtpUsageDtos.Summary.builder()
                .companyGstId(gst.getId())
                .gstNumber(gst.getGstNumber())
                .companyName(gst.getCompany() != null ? gst.getCompany().getCompanyName() : null)
                .periodKey(periodKey)
                .otpRequests(row != null ? toLong(row[1]) : 0)
                .failedRequests(row != null ? toLong(row[2]) : 0)
                .verifications(row != null ? toLong(row[3]) : 0)
                .failedVerifications(row != null ? toLong(row[4]) : 0)
                .lastOtpAt(row != null ? (LocalDateTime) row[5] : null)
                .sessionStatus(sessionStatus)
                .sessionExpiresAt(session != null ? session.getExpiresAt() : null)
                .build();
    }

    private static long toLong(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }

    private static YearMonth parsePeriod(String period) {
        if (period == null || period.isBlank()) return YearMonth.now();
        try {
            return YearMonth.parse(period.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("period must be yyyy-MM, e.g. 2026-11");
        }
    }
}
