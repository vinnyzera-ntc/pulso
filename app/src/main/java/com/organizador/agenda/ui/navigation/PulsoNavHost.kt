package com.organizador.agenda.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.organizador.agenda.R
import com.organizador.agenda.ui.agenda.AgendaScreen
import com.organizador.agenda.ui.capture.CaptureScreen
import com.organizador.agenda.ui.focus.FocusScreen
import com.organizador.agenda.ui.newitem.NewItemPlaceholderScreen
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoMotion
import com.organizador.agenda.ui.pulso.components.BottomNavPill
import com.organizador.agenda.ui.pulso.components.NavItem
import com.organizador.agenda.ui.settings.SettingsScreen
import com.organizador.agenda.ui.tasks.TasksScreen
import com.organizador.agenda.ui.today.TodayScreen

@Composable
fun PulsoNavHost(
    startRoute: Any? = null,
    navController: NavHostController = rememberNavController(),
) {
    val c = Pulso.colors
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val currentTab = TopLevel.entries.firstOrNull { tab ->
        destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }

    val reduced = Pulso.reducedMotion
    val slidePx = with(LocalDensity.current) { 24.dp.roundToPx() }
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (reduced) fadeIn(tween(PulsoMotion.QUICK))
        else fadeIn(tween(PulsoMotion.SHEET, easing = PulsoMotion.Standard)) +
            slideInVertically(tween(PulsoMotion.SHEET, easing = PulsoMotion.Standard)) { slidePx }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(tween(if (reduced) PulsoMotion.QUICK else 180, easing = PulsoMotion.Exit))
    }

    Scaffold(
        containerColor = c.bg,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) {
                BottomNavPill(
                    items = TopLevel.entries.map { NavItem(stringResource(it.label), it.icon) },
                    selectedIndex = currentTab.ordinal,
                    onSelect = { i -> navController.navigateToTab(TopLevel.entries[i]) },
                    onCreate = { navController.navigate(NewItemRoute) },
                    createLabel = stringResource(R.string.action_new_item),
                    onCreateLongPress = { navController.navigate(CaptureRoute) },
                    createLongPressLabel = stringResource(R.string.action_quick_capture),
                )
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = startRoute ?: TopLevel.Today.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = { fadeIn(tween(if (reduced) PulsoMotion.QUICK else PulsoMotion.BASE)) },
            popExitTransition = exit,
        ) {
            val bottom = PaddingValues(bottom = inner.calculateBottomPadding())
            composable<TodayRoute> {
                TodayScreen(
                    contentPadding = bottom,
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenCapture = { navController.navigate(CaptureRoute) },
                )
            }
            composable<AgendaRoute> { AgendaScreen(contentPadding = bottom) }
            composable<TasksRoute> { TasksScreen(contentPadding = bottom) }
            composable<FocusRoute> { FocusScreen(contentPadding = bottom) }
            composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable<CaptureRoute> { CaptureScreen(onClose = { navController.popBackStack() }) }
            composable<NewItemRoute> { NewItemPlaceholderScreen(onClose = { navController.popBackStack() }) }
        }
    }
}

private fun NavHostController.navigateToTab(tab: TopLevel) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
