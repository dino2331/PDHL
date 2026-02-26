package com.pdrehab.handwritinglab.ui.screens.admin

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdrehab.handwritinglab.ui.components.GuideHeaderBar
import com.pdrehab.handwritinglab.ui.components.PrimaryButton
import java.io.File

@Composable
fun AdminToolsScreen(
    onRequireLogin: () -> Unit,
    vm: AdminToolsViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.load() }
    val ui by vm.ui.collectAsState()

    if (!ui.isAdmin) {
        onRequireLogin()
        return
    }

    fun shareZip(zip: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", zip)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "내보내기"))
    }

    Column(Modifier.fillMaxSize()) {
        GuideHeaderBar("Admin Tools", "Export", "")
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("세션 선택", fontSize = 24.sp)
            Spacer(Modifier.height(8.dp))

            ui.sessions.forEach { s ->
                val selected = (ui.selectedSessionId == s.sessionId)
                Text(
                    text = (if (selected) "✓ " else "") + "${s.participantCode}  ${s.sessionId.take(8)}  ${s.createdAtMs}",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.select(s.sessionId) }
                        .padding(vertical = 8.dp)
                )
            }

            ui.message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, fontSize = 18.sp)
            }

            Spacer(Modifier.weight(1f))

            PrimaryButton(
                text = if (ui.exporting) "Export 중..." else "Export 실행",
                enabled = !ui.exporting && ui.selectedSessionId != null,
                modifier = Modifier.fillMaxWidth(),
                onClick = { vm.runExport() }
            )

            Spacer(Modifier.height(10.dp))

            ui.exportedZip?.let { zip ->
                PrimaryButton(
                    text = "ZIP 공유하기",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { shareZip(zip) }
                )
            }
        }
    }
}