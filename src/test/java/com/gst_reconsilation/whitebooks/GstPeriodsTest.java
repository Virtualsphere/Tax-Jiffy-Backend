package com.gst_reconsilation.whitebooks;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class GstPeriodsTest {

    @Test
    void monthNamesUseTheRightCalendarYear() {
        assertThat(GstPeriods.toRetPeriod("2026-27", "JUNE")).isEqualTo("062026");
        assertThat(GstPeriods.toRetPeriod("2026-27", "December")).isEqualTo("122026");
        assertThat(GstPeriods.toRetPeriod("2026-27", "JANUARY")).isEqualTo("012027");
        assertThat(GstPeriods.toRetPeriod("2026-27", "Mar")).isEqualTo("032027");
    }

    @Test
    void numericAndAlreadyFormattedPeriods() {
        assertThat(GstPeriods.toRetPeriod("2026-27", "04")).isEqualTo("042026");
        assertThat(GstPeriods.toRetPeriod(null, "102024")).isEqualTo("102024");
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> GstPeriods.toRetPeriod("2026-27", "SOMETHING"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GstPeriods.toRetPeriod("bad", "JUNE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
