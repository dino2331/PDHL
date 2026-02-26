package com.pdrehab.handwritinglab.data.repo

import android.content.Context
import com.pdrehab.handwritinglab.core.JsonUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class AddressRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cached: List<AddressItem>? = null

    fun loadAll(): List<AddressItem> {
        val c = cached
        if (c != null) return c
        val json = context.assets.open("addresses_ko_12_nospace_100.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val items = JsonUtil.json.decodeFromString<List<AddressItem>>(json)
        cached = items
        return items
    }

    fun assignByParticipantCode(participantCode: String): AddressItem {
        val items = loadAll()
        val idx = abs(participantCode.hashCode()) % items.size
        return items[idx]
    }
}