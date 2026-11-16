package com.gst_reconsilation.apiusage.service;

import com.gst_reconsilation.apiusage.entity.GstOtpEvent;
import com.gst_reconsilation.apiusage.repository.GstOtpEventRepository;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Logs every GST OTP request/verification for the admin "OTP Usage" page. Never blocks anything. */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpUsageService {

    private final OtpEventWriter writer;

    /**
     * Never throws: monitoring must not break the OTP flow itself. The write runs in its own
     * transaction ({@link OtpEventWriter}) so failed attempts are kept even though the caller's
     * transaction rolls back; catching here also covers errors raised at that commit.
     */
    public void record(Integer companyGstId, String gstin, Integer userId, String action, boolean success, String message) {
        try {
            writer.write(companyGstId, GstOtpEvent.builder()
                    .gstin(gstin)
                    .userId(userId)
                    .action(action)
                    .success(success)
                    .message(truncate(message))
                    .build());
        } catch (Exception e) {
            log.warn("Could not log GST OTP {} for companyGst {}: {}", action, companyGstId, e.getMessage());
        }
    }

    private static String truncate(String s) {
        return s == null || s.length() <= 500 ? s : s.substring(0, 500);
    }

    /** Separate bean so the REQUIRES_NEW proxy applies (self-invocation would bypass it). */
    @Service
    @RequiredArgsConstructor
    public static class OtpEventWriter {
        private final GstOtpEventRepository repository;
        private final CompanyGSTRepository companyGSTRepository;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void write(Integer companyGstId, GstOtpEvent event) {
            event.setCompanyGST(companyGSTRepository.getReferenceById(companyGstId));
            repository.saveAndFlush(event);
        }
    }
}
