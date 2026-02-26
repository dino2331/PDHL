package com.pdrehab.handwritinglab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.pdrehab.handwritinglab.ui.navigation.NavGraph
import com.pdrehab.handwritinglab.ui.theme.PDHLTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PDHLTheme {
                NavGraph()
            }
        }
    }
}