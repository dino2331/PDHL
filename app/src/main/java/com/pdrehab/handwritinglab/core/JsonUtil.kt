package com.pdrehab.handwritinglab.core

import kotlinx.serialization.json.Json

object JsonUtil {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
    }
}