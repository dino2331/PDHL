package com.pdrehab.handwritinglab.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.pdrehab.handwritinglab.ui.screens.admin.AdminLoginScreen
import com.pdrehab.handwritinglab.ui.screens.admin.AdminToolsScreen
import com.pdrehab.handwritinglab.ui.screens.participant.TrendScreen
import com.pdrehab.handwritinglab.ui.screens.task.TaskResultScreen
import com.pdrehab.handwritinglab.ui.screens.task.TaskRunScreen

@Composable
fun NavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.Splash) {

        composable(Routes.Splash) {
            // 기존 SplashScreen이 있으면 교체해서 사용
            com.pdrehab.handwritinglab.ui.screens.splash.SplashScreen(
                onStart = { nav.navigate(Routes.ParticipantEntry) { popUpTo(Routes.Splash) { inclusive = true } } }
            )
        }

        composable(Routes.ParticipantEntry) {
            com.pdrehab.handwritinglab.ui.screens.participant.ParticipantEntryScreen(
                onDone = { code -> nav.navigate(Routes.home(code)) }
            )
        }

        composable(
            route = Routes.ParticipantHome,
            arguments = listOf(navArgument("participantCode") { type = NavType.StringType })
        ) { backStack ->
            val code = backStack.arguments?.getString("participantCode")!!
            com.pdrehab.handwritinglab.ui.screens.participant.ParticipantHomeScreen(
                participantCode = code,
                onStartSession = { nav.navigate(Routes.SessionNew) },
                onHistory = { nav.navigate(Routes.history(code)) },
                onAdmin = { nav.navigate(Routes.AdminLogin) }
            )
        }

        composable(Routes.SessionNew) {
            com.pdrehab.handwritinglab.ui.screens.session.SessionNewScreen(
                onGoPressure = { nav.navigate(Routes.CalibrationPressure) }
            )
        }

        composable(Routes.CalibrationPressure) {
            com.pdrehab.handwritinglab.ui.screens.calibration.PressureCalibrationScreen(
                onDone = { nav.navigate(Routes.CalibrationSize) }
            )
        }

        composable(Routes.CalibrationSize) {
            com.pdrehab.handwritinglab.ui.screens.calibration.SizeCalibrationScreen(
                onDoneStartFirstTask = { firstTaskInstanceId ->
                    nav.navigate(Routes.run(firstTaskInstanceId)) {
                        popUpTo(Routes.ParticipantEntry) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.TaskRun,
            arguments = listOf(navArgument("taskInstanceId") { type = NavType.StringType })
        ) { backStack ->
            val taskInstanceId = backStack.arguments?.getString("taskInstanceId")!!
            TaskRunScreen(
                taskInstanceId = taskInstanceId,
                onNavigateToTrial = { nextId -> nav.navigate(Routes.run(nextId)) },
                onNavigateToResult = { sessionId, taskId -> nav.navigate(Routes.result(sessionId, taskId)) }
            )
        }

        composable(
            route = Routes.TaskResult,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("taskId") { type = NavType.StringType }
            )
        ) { backStack ->
            val sessionId = backStack.arguments?.getString("sessionId")!!
            val taskId = backStack.arguments?.getString("taskId")!!

            // ✅ (중요) TaskResultScreen 시그니처에 맞춰 호출
            TaskResultScreen(
                sessionId = sessionId,
                taskId = taskId,
                onNextTask = { nextId: String -> nav.navigate(Routes.run(nextId)) },
                onSessionDone = { sid: String -> nav.navigate(Routes.summary(sid)) }
            )
        }

        composable(
            route = Routes.SessionSummary,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStack ->
            val sid = backStack.arguments?.getString("sessionId")!!
            com.pdrehab.handwritinglab.ui.screens.session.SessionSummaryScreen(
                sessionId = sid,
                onDone = { nav.popBackStack(Routes.ParticipantEntry, false) },
                onAdminExport = { nav.navigate(Routes.AdminLogin) }
            )
        }

        composable(
            route = Routes.HistoryList,
            arguments = listOf(navArgument("participantCode") { type = NavType.StringType })
        ) { backStack ->
            val code = backStack.arguments?.getString("participantCode")!!
            com.pdrehab.handwritinglab.ui.screens.participant.HistoryListScreen(
                participantCode = code,
                onTrend = { nav.navigate(Routes.trend(code)) }
            )
        }

        composable(
            route = Routes.Trend,
            arguments = listOf(navArgument("participantCode") { type = NavType.StringType })
        ) { backStack ->
            val code = backStack.arguments?.getString("participantCode")!!
            TrendScreen(participantCode = code)
        }

        composable(Routes.AdminTools) {
            AdminToolsScreen(
                onRequireLogin = { nav.navigate(Routes.AdminLogin) }
            )
        }
    }
}