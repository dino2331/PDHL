package com.pdrehab.handwritinglab

import com.pdrehab.handwritinglab.core.PdhlConstants
import org.junit.Assert.*
import org.junit.Test

class NextPolicyTest {

    @Test fun right_edge_enables() {
        val w = 1000
        val thr = (PdhlConstants.NEXT_RIGHT_EDGE_RATIO * w).toInt()
        assertTrue(thr == 920)
    }

    @Test fun all_boxes_requires_12() {
        val filled = BooleanArray(12) { false }
        filled[0] = true
        assertFalse(filled.all { it })
        for (i in 0 until 12) filled[i] = true
        assertTrue(filled.all { it })
    }
}