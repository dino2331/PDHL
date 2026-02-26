package com.pdrehab.handwritinglab

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class SmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun smoke_createSession_T01_to_result() {
        rule.onNodeWithText("시작").performClick()

        rule.onNodeWithText("참가자 ID").performTextInput("AB12CD34")
        rule.onNodeWithText("확인").performClick()

        rule.onNodeWithText("과제하기").performClick()
        rule.onNodeWithText("세션 시작").performClick()

        // calibration skip (debug)
        rule.onNodeWithText("테스트 건너뛰기(디버그)").performClick()
        rule.onNodeWithText("테스트 건너뛰기(디버그)").performClick()

        // trial1 overlay start
        rule.onNodeWithText("시작하기").performClick()
        rule.onNodeWithText("강제 종료(디버그)").performClick()

        // trial2 overlay start
        rule.onNodeWithText("시작하기").performClick()
        rule.onNodeWithText("강제 종료(디버그)").performClick()

        // result screen
        rule.onNodeWithText("결과").assertExists()
    }
}