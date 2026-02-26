package com.pdrehab.handwritinglab

import com.pdrehab.handwritinglab.domain.model.MetricCatalog
import org.junit.Assert.*
import org.junit.Test

class DirectionTest {
    @Test fun size_reduction_lower_better() {
        val d = MetricCatalog.def("SIZE_REDUCTION_PCT")
        assertEquals(MetricDirection.LOWER_BETTER, d?.direction)
    }

    @Test fun ratio_higher_better() {
        val d = MetricCatalog.def("ON_SURFACE_TO_INAIR_RATIO")
        assertEquals(MetricDirection.HIGHER_BETTER, d?.direction)
    }
}