package com.pdrehab.handwritinglab

import com.pdrehab.handwritinglab.core.Ids
import com.pdrehab.handwritinglab.feature.taskrunner.NextButtonPolicy
import org.junit.Assert.*
import org.junit.Test

class CoreAndNextPolicyTest {

    @Test fun normalize_uppercase_trim() {
        assertEquals("AB12CD34", Ids.normalizeParticipantCode(" ab12cd34 "))
    }

    @Test fun valid_code_ok() {
        assertTrue(Ids.isValidParticipantCode("AB12CD34"))
    }

    @Test fun valid_code_fail_length() {
        assertFalse(Ids.isValidParticipantCode("AB12"))
    }

    @Test fun valid_code_fail_lower() {
        assertFalse(Ids.isValidParticipantCode("ab12cd34"))
    }

    @Test fun valid_code_fail_symbol() {
        assertFalse(Ids.isValidParticipantCode("AB12CD3!"))
    }

    @Test fun next_right_edge_disabled_initial() {
        val p = NextButtonPolicy(NextButtonPolicy.Rule.RIGHT_EDGE, canvasWidthPx = 1000)
        assertFalse(p.enabled)
    }

    @Test fun next_right_edge_enable_when_over_threshold() {
        val p = NextButtonPolicy(NextButtonPolicy.Rule.RIGHT_EDGE, canvasWidthPx = 1000, rightEdgeRatio = 0.92)
        p.onPenDown(xPx = 919f, boxId = -1)
        assertFalse(p.enabled)
        p.onPenDown(xPx = 920f, boxId = -1)
        assertTrue(p.enabled)
    }

    @Test fun next_right_edge_latch() {
        val p = NextButtonPolicy(NextButtonPolicy.Rule.RIGHT_EDGE, 1000)
        p.onPenDown(930f, -1)
        assertTrue(p.enabled)
        p.onPenDown(100f, -1)
        assertTrue(p.enabled)
    }

    @Test fun next_right_edge_reset() {
        val p = NextButtonPolicy(NextButtonPolicy.Rule.RIGHT_EDGE, 1000)
        p.onPenDown(930f, -1)
        assertTrue(p.enabled)
        p.resetForNewPage()
        assertFalse(p.enabled)
    }

    @Test fun next_boxes_initial_disabled() {
        val p = NextButtonPolicy(NextButtonPolicy.Rule.ALL_BOXES_12, 1000, boxCount = 12)
        assertFalse(p.enabled)
    }

    @Test fun next_boxes_enable_after_all_filled() {
        val p = NextButtonPolicy(NextButtonPolicy.Rule.ALL_BOXES_12, 1000, boxCount = 12)
        for (i in 0..10) {
            p.onPenDown(xPx = 10f, boxId = i)
            assertFalse(p.enabled)
        }
        p.onPenDown(10f, 11)
        assertTrue(p.enabled)
    }

    @Test fun next_boxes_ignore_invalid_box() {
        val p = NextButtonPolicy(NextButtonPolicy.Rule.ALL_BOXES_12, 1000, boxCount = 12)
        p.onPenDown(10f, -1)
        assertFalse(p.enabled)
        p.onPenDown(10f, 999)
        assertFalse(p.enabled)
    }

    @Test fun next_boxes_reset_clears_progress() {
        val p = NextButtonPolicy(NextButtonPolicy.Rule.ALL_BOXES_12, 1000, boxCount = 12)
        for (i in 0..11) p.onPenDown(10f, i)
        assertTrue(p.enabled)
        p.resetForNewPage()
        assertFalse(p.enabled)
        for (i in 0..10) p.onPenDown(10f, i)
        assertFalse(p.enabled)
    }
}