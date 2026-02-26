package com.pdrehab.handwritinglab.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AddressItem(
    val id: String,
    val text: String,
    val writeUnits: Int,
    val strokeCount: Int,
    val difficulty: String
)