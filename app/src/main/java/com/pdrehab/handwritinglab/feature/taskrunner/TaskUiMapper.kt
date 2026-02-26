package com.pdrehab.handwritinglab.feature.taskrunner

import com.pdrehab.handwritinglab.feature.taskrunner.guides.GuideType

fun mapGuide(guide: String): GuideType = when (guide) {
    "NONE" -> GuideType.NONE
    "TWO_HORIZONTAL_LINES" -> GuideType.TWO_HORIZONTAL_LINES
    "TOP_DOTS" -> GuideType.TOP_DOTS
    "BOXES_ROW_12" -> GuideType.BOXES_ROW_12
    "VARIABLE_BOXES_LINE" -> GuideType.VARIABLE_BOXES_LINE
    "NUMBERED_BOXES" -> GuideType.NUMBERED_BOXES
    // GAME_APPLE / TRACE_PATH는 overlay로 그림(뷰 guide는 NONE로 둠)
    "GAME_APPLE" -> GuideType.NONE
    "TRACE_PATH" -> GuideType.NONE
    else -> GuideType.NONE
}