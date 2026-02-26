package com.pdrehab.handwritinglab

import com.pdrehab.handwritinglab.core.isValidParticipantCode
import com.pdrehab.handwritinglab.core.normalizeParticipantCode
import org.junit.Assert.*
import org.junit.Test

class IdsTest {

    @Test fun normalize_upper_trim() {
        assertEquals("AB12CD34", normalizeParticipantCode(" ab12cd34 "))
    }

    @Test fun validate_ok() {
        assertTrue(isValidParticipantCode("AB12CD34"))
    }

    @Test fun validate_fail_short() {
        assertFalse(isValidParticipantCode("AB12CD3"))
    }

    @Test fun validate_fail_symbols() {
        assertFalse(isValidParticipantCode("AB12-CD3"))
    }
}