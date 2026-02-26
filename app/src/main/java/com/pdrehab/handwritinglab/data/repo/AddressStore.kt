package com.pdrehab.handwritinglab.data.repo

import android.content.Context
import com.pdrehab.handwritinglab.core.JsonUtil
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class AddressItem(
    val id: String,
    val text: String,
    val writeUnits: Int,
    val strokeCount: Int,
    val difficulty: String
)

class AddressStore(private val context: Context) {
    @Volatile private var cached: List<AddressItem>? = null

    fun load(): List<AddressItem> {
        val c = cached
        if (c != null) return c
        val text = context.assets.open("addresses_ko_12_nospace_100.json").bufferedReader().use { it.readText() }
        val list = JsonUtil.json.decodeFromString(ListSerializer(AddressItem.serializer()), text)
        cached = list
        return list
    }

    fun assign(participantCode: String): AddressItem {
        val list = load()
        val idx = abs(participantCode.hashCode()) % list.size
        return list[idx]
    }
}

// helper for kotlinx.serialization List
private class ListSerializer<T>(private val s: kotlinx.serialization.KSerializer<T>) :
    kotlinx.serialization.KSerializer<List<T>> {
    override val descriptor = kotlinx.serialization.builtins.ListSerializer(s).descriptor
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: List<T>) =
        kotlinx.serialization.builtins.ListSerializer(s).serialize(encoder, value)
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): List<T> =
        kotlinx.serialization.builtins.ListSerializer(s).deserialize(decoder)
}