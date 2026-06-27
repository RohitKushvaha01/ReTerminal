package com.rk.terminal.ui.navHosts

import android.content.res.Configuration
import android.os.Build
import android.view.Window
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rk.settings.Settings
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.animations.NavigationAnimationTransitions
import com.rk.terminal.ui.routes.MainActivityRoutes
import com.rk.terminal.ui.screens.customization.Customization
import com.rk.terminal.ui.screens.downloader.SetupScreen
import com.rk.terminal.ui.screens.settings.Settings
import com.rk.terminal.ui.screens.terminal.Rootfs
import com.rk.terminal.ui.screens.terminal.TerminalScreen

@Composable
fun MainActivityNavHost(
    navController: NavHostController,
    mainActivity: MainActivity,
    modifier: Modifier = Modifier
) {
    val showStatusBar by remember { mutableStateOf(Settings.statusBar) }
    val horizontalStatusBar by remember { mutableStateOf(Settings.horizontal_statusBar) }

    NavHost(
        navController = navController,
        startDestination = MainActivityRoutes.MainScreen.route,
        enterTransition = { NavigationAnimationTransitions.enterTransition },
        exitTransition = { NavigationAnimationTransitions.exitTransition },
        popEnterTransition = { NavigationAnimationTransitions.popEnterTransition },
        popExitTransition = { NavigationAnimationTransitions.popExitTransition },
        modifier = modifier
    ) {
        composable(MainActivityRoutes.MainScreen.route) {
            if (Rootfs.isInstalled.value) {
                val config = LocalConfiguration.current
                val show = if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    horizontalStatusBar
                } else {
                    showStatusBar
                }
                UpdateStatusBar(mainActivity.window, show)
                TerminalScreen(mainActivity = mainActivity, navController = navController)
            } else {
                SetupScreen(mainActivity = mainActivity, navController = navController)
            }
        }
        
        composable(MainActivityRoutes.Settings.route) {
            UpdateStatusBar(mainActivity.window, true)
            Settings(navController = navController, mainActivity = mainActivity)
        }
        
        composable(MainActivityRoutes.Customization.route) {
            UpdateStatusBar(mainActivity.window, true)
            Customization(mainActivity = mainActivity, navController = navController)
        }
    }
}

@Composable
private fun UpdateStatusBar(window: Window, show: Boolean) {
    LaunchedEffect(show) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            val controller = window.decorView.windowInsetsController
            if (show) {
                controller?.show(android.view.WindowInsets.Type.statusBars())
            } else {
                controller?.hide(android.view.WindowInsets.Type.statusBars())
            }
        } else {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (show) {
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            } else {
                controller.hide(WindowInsetsCompat.Type.statusBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}
