package com.pdrehab.handwritinglab.domain.usecase

fun formatPrompt(prompt: String, assignedAddressText: String): String {
    return prompt.replace("{{assignedAddressText}}", assignedAddressText)
}