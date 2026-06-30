package com.focusguard.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.focusguard.app.ui.screens.auth.LoginScreen
import com.focusguard.app.ui.screens.auth.RegisterScreen
import com.focusguard.app.ui.screens.dashboard.DashboardScreen
import com.focusguard.app.ui.screens.home.HomeScreen
import com.focusguard.app.ui.screens.schedule.DailyScheduleScreen
import com.focusguard.app.ui.screens.schedule.EmergencyRecoveryScreen
import com.focusguard.app.ui.screens.schedule.FocusSprintScreen
import com.focusguard.app.ui.screens.settings.CalendarSyncScreen
import com.focusguard.app.ui.screens.settings.HabitInsightsScreen
import com.focusguard.app.ui.screens.settings.SettingsScreen
import com.focusguard.app.ui.screens.splash.SplashScreen
import com.focusguard.app.ui.screens.task.AddTaskScreen
import com.focusguard.app.ui.screens.task.TaskDetailScreen
import com.focusguard.app.ui.screens.task.VoiceTaskScreen

sealed class Screen(val route: String) {
    object Splash            : Screen("splash")
    object Login             : Screen("login")
    object Register          : Screen("register")
    object Home              : Screen("home")
    object AddTask           : Screen("add_task")
    object VoiceTask         : Screen("voice_task")
    object TaskDetail        : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }
    object Schedule          : Screen("schedule")
    object FocusSprint       : Screen("focus_sprint/{taskId}") {
        fun createRoute(taskId: String) = "focus_sprint/$taskId"
    }
    object EmergencyRecovery : Screen("emergency_recovery/{taskId}") {
        fun createRoute(taskId: String) = "emergency_recovery/$taskId"
    }
    object Dashboard         : Screen("dashboard")
    object HabitInsights     : Screen("habit_insights")
    object Settings          : Screen("settings")
    object CalendarSync      : Screen("calendar_sync")
}

@Composable
fun FocusGuardNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
    ) {

        // ── Auth ──────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Home ──────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToAddTask = { navController.navigate(Screen.AddTask.route) },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onBottomNavClick = { route ->
                    when (route) {
                        BottomNavRoute.Schedule  -> navController.navigate(Screen.Schedule.route)
                        BottomNavRoute.Dashboard -> navController.navigate(Screen.Dashboard.route)
                        BottomNavRoute.Settings  -> navController.navigate(Screen.Settings.route)
                        BottomNavRoute.Home      -> { /* already here */ }
                    }
                },
            )
        }

        // ── Tasks ─────────────────────────────────────────────────────────
        composable(Screen.AddTask.route) {
            AddTaskScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVoice = { navController.navigate(Screen.VoiceTask.route) },
                onTaskSaved = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.VoiceTask.route) {
            VoiceTaskScreen(
                onNavigateBack = { navController.popBackStack() },
                onTaskCaptured = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            TaskDetailScreen(
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() },
                onStartSprint = { navController.navigate(Screen.FocusSprint.createRoute(taskId)) },
                onGenerateRecovery = { navController.navigate(Screen.EmergencyRecovery.createRoute(taskId)) },
            )
        }

        // ── Schedule ──────────────────────────────────────────────────────
        composable(Screen.Schedule.route) {
            DailyScheduleScreen(
                onNavigateBack = { navController.popBackStack() },
                onBottomNavClick = { route ->
                    when (route) {
                        BottomNavRoute.Home      -> navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                        BottomNavRoute.Dashboard -> navController.navigate(Screen.Dashboard.route)
                        BottomNavRoute.Settings  -> navController.navigate(Screen.Settings.route)
                        BottomNavRoute.Schedule  -> { }
                    }
                },
            )
        }

        composable(
            route = Screen.FocusSprint.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            FocusSprintScreen(
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() },
                onEmergency = { navController.navigate(Screen.EmergencyRecovery.createRoute(taskId)) },
            )
        }

        composable(
            route = Screen.EmergencyRecovery.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            EmergencyRecoveryScreen(
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() },
                onAccepted = {
                    navController.navigate(Screen.FocusSprint.createRoute(taskId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
            )
        }

        // ── Dashboard ─────────────────────────────────────────────────────
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onViewHabits = { navController.navigate(Screen.HabitInsights.route) },
                onBottomNavClick = { route ->
                    when (route) {
                        BottomNavRoute.Home      -> navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                        BottomNavRoute.Schedule  -> navController.navigate(Screen.Schedule.route)
                        BottomNavRoute.Settings  -> navController.navigate(Screen.Settings.route)
                        BottomNavRoute.Dashboard -> { }
                    }
                },
            )
        }

        composable(Screen.HabitInsights.route) {
            HabitInsightsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Settings ──────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCalendar = { navController.navigate(Screen.CalendarSync.route) },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBottomNavClick = { route ->
                    when (route) {
                        BottomNavRoute.Home      -> navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                        BottomNavRoute.Schedule  -> navController.navigate(Screen.Schedule.route)
                        BottomNavRoute.Dashboard -> navController.navigate(Screen.Dashboard.route)
                        BottomNavRoute.Settings  -> { }
                    }
                },
            )
        }

        composable(Screen.CalendarSync.route) {
            CalendarSyncScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
