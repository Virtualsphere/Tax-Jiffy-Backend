package com.gst_reconsilation.apiusage;

import com.gst_reconsilation.apiusage.entity.ApiUsageCounter;
import com.gst_reconsilation.apiusage.exception.ApiUsageLimitExceededException;
import com.gst_reconsilation.apiusage.repository.ApiUsageCounterRepository;
import com.gst_reconsilation.apiusage.service.ApiUsageService;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApiUsageServiceTest {

    private final ApiUsageCounterRepository counters = mock(ApiUsageCounterRepository.class);
    private final CompanyGSTRepository gsts = mock(CompanyGSTRepository.class);
    private final ApiUsageService service = new ApiUsageService(counters, gsts);

    @BeforeEach
    void setUp() {
        CompanyGST gst = new CompanyGST();
        gst.setId(7);
        gst.setGstNumber("27ABCDE1234F1Z5");
        gst.setApiCallLimitOverride(100);
        when(gsts.findById(7)).thenReturn(Optional.of(gst));
    }

    @Test
    void limitIsOnTheTotalAcrossAllApiTypes() {
        // 0 IMS calls so far, but 100 calls in total across other types -> IMS is blocked too
        when(counters.findForUpdate(eq(7), eq("IMS"), anyString())).thenReturn(Optional.empty());
        when(counters.sumCalls(eq(7), anyString(), eq(ApiUsageService.LIMITED_TYPES))).thenReturn(100L);

        var ex = assertThrows(ApiUsageLimitExceededException.class, () -> service.recordAndEnforce(7, ApiUsageService.IMS));
        assertTrue(ex.getMessage().contains("100/100"), ex.getMessage());
        verify(counters, never()).save(any());
    }

    @Test
    void countsTheCallWhenUnderTheLimit() {
        ApiUsageCounter existing = ApiUsageCounter.builder().apiType("GSTR1").periodKey("x").callCount(4).build();
        when(counters.findForUpdate(eq(7), eq("GSTR1"), anyString())).thenReturn(Optional.of(existing));
        when(counters.sumCalls(eq(7), anyString(), any())).thenReturn(99L);

        service.recordAndEnforce(7, ApiUsageService.GSTR1);

        ArgumentCaptor<ApiUsageCounter> saved = ArgumentCaptor.forClass(ApiUsageCounter.class);
        verify(counters).save(saved.capture());
        assertEquals(5, saved.getValue().getCallCount());
    }

    @Test
    void ensureRemainingBlocksAMultiCallOperationThatCannotFinish() {
        when(counters.sumCalls(eq(7), anyString(), any())).thenReturn(90L); // 10 left, GSTR-1 needs 13
        var ex = assertThrows(ApiUsageLimitExceededException.class,
                () -> service.ensureRemaining(7, 13, "GSTR-1 sync"));
        assertTrue(ex.getMessage().contains("only 10 of 100"), ex.getMessage());

        when(counters.sumCalls(eq(7), anyString(), any())).thenReturn(87L); // exactly 13 left
        assertDoesNotThrow(() -> service.ensureRemaining(7, 13, "GSTR-1 sync"));
    }

    @Test
    void noPlanAndNoOverrideMeansNoCalls() {
        CompanyGST noPlan = new CompanyGST();
        assertEquals(0, service.effectiveLimit(noPlan));
    }
}
