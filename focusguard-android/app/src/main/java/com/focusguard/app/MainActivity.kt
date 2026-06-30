package com.focusguard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.focusguard.app.notifications.CheckInNotifier
import com.focusguard.app.notifications.EveningAlertNotifier
import com.focusguard.app.ui.navigation.FocusGuardNavGraph
import com.focusguard.app.ui.theme.FocusGuardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Schedule daily 9 AM check-in notification
        CheckInNotifier.schedule(this)
        // Schedule daily 9 PM alert — calls user if they haven't updated tasks
        EveningAlertNotifier.schedule(this)
        setContent {
            FocusGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FocusGuardTheme.colors.background
                ) {
                    FocusGuardNavGraph()
                }
            }
        }
    }
}
