package com.pdrehab.handwritinglab.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeFormat {
    private val zone = ZoneId.of("Asia/Seoul")
    private val iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun iso(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(zone).format(iso)

    fun yyyyMMdd(ms: Long): String =
        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(zone)
            .format(Instant.ofEpochMilli(ms))
}