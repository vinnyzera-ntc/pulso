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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.organizador.agenda.MainActivity
import com.organizador.agenda.R
import com.organizador.agenda.ui.agenda.AgendaScreen
import com.organizador.agenda.ui.capture.CaptureScreen
import com.organizador.agenda.ui.common.PulsoSnackbarHost
import com.organizador.agenda.ui.focus.FocusScreen
import com.organizador.agenda.ui.pulso.LocalGlassBackdrop
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoMotion
import com.organizador.agenda.ui.pulso.components.BottomNavPill
import com.organizador.agenda.ui.pulso.components.NavItem
import com.organizador.agenda.ui.settings.SettingsScreen
import com.organizador.agenda.ui.tasks.TaskDetailScreen
import com.organizador.agenda.ui.tasks.TasksScreen
import com.organizador.agenda.ui.today.TodayScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PulsoNavHost(
    startRoute: Any? = null,
    seedMode: String? = null,
    /** Muda a cada toque na notificação do foco com o app já aberto: leva para a aba Foco. */
    openFocusSignal: Int = 0,
    /** Toque num lembrete: abre o detalhe da tarefa por cima da aba atual. */
    openTask: OpenTaskRequest? = null,
    /** Atalho do widget: Captura rápida, Agenda ou Tarefas. */
    openScreen: OpenScreenRequest? = null,
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val c = Pulso.colors
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val currentTab = TopLevel.entries.firstOrNull { tab ->
        destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }
    LaunchedEffect(seedMode) { seedMode?.let(appViewModel::seedOnce) }
    LaunchedEffect(openFocusSignal) { if (openFocusSignal > 0) navController.navigateToTab(TopLevel.Focus) }
    LaunchedEffect(openScreen) {
        when (openScreen?.target) {
            MainActivity.OPEN_CAPTURE -> navController.navigate(CaptureRoute) { launchSingleTop = true }
            MainActivity.OPEN_AGENDA -> navController.navigateToTab(TopLevel.Agenda)
            MainActivity.OPEN_TASKS -> navController.navigateToTab(TopLevel.Tasks)
        }
    }
    LaunchedEffect(openTask) {
        openTask?.let { navController.navigate(TaskDetailRoute(it.taskId)) { launchSingleTop = true } }
    }
    val focusActive by appViewModel.focusActive.collectAsStateWithLifecycle()
    // Durante uma sessão, a aba Foco é tela cheia (sem nav pill); "voltar" leva ao Hoje e o timer segue.
    val showNav = currentTab != null && !(currentTab == TopLevel.Focus && focusActive)

    // Mensagens (snackbar): uma nova substitui a que estiver na tela.
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        appViewModel.messenger.messages.collectLatest { message ->
            var actionTaken = false
            try {
                val result = snackbar.showSnackbar(
                    message = context.getString(message.text, *message.args.toTypedArray()),
                    actionLabel = message.actionLabel?.let(context::getString),
                    duration = if (message.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
                )
                actionTaken = result == SnackbarResult.ActionPerformed
                if (actionTaken) appViewModel.messenger.runAction(message)
            } finally {
                // Também quando um aviso novo substitui este (a coleta é cancelada).
                if (!actionTaken) appViewModel.messenger.runDismissed(message)
            }
        }
    }

    // Folha "Novo item" (tarefa ou compromisso) ou edição de compromisso; null = fechada.
    var sheet by remember { mutableStateOf<SheetRequest?>(null) }

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

    // Liquid Glass: o conteúdo das telas (com o fundo da tela por baixo) é o que o nav pill e
    // os avisos refratam. As folhas usam o desfoque do sistema (outra janela).
    val screenBg = if (currentTab == TopLevel.Focus) c.deep else c.bg
    val backdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }
    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
    Scaffold(
        // O Foco usa o fundo `deep`; igualar aqui evita uma faixa de outra cor ao lado do recorte da câmera.
        containerColor = screenBg,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            // Com o nav pill, o Scaffold já põe o snackbar acima dele; sem ele, sobe da barra de gestos.
            PulsoSnackbarHost(snackbar, if (!showNav) Modifier.navigationBarsPadding().imePadding() else Modifier)
        },
        bottomBar = {
            if (showNav) {
                BottomNavPill(
                    items = TopLevel.entries.map { NavItem(stringResource(it.label), it.icon) },
                    selectedIndex = currentTab.ordinal,
                    onSelect = { i -> navController.navigateToTab(TopLevel.entries[i]) },
                    // Na Agenda o + começa em compromisso; nas outras abas, em tarefa.
                    onCreate = {
                        sheet = SheetRequest.Create(if (currentTab == TopLevel.Agenda) NewItemKind.Event else NewItemKind.Task)
                    },
                    createLabel = stringResource(R.string.action_new_item),
                    onCreateLongPress = { navController.navigate(CaptureRoute) },
                    createLongPressLabel = stringResource(R.string.action_quick_capture),
                )
            }
        },
    ) { inner ->
        // O construtor do grafo roda uma vez só (antes de o nav pill existir), então as telas
        // leem a altura do pill por este estado, que acompanha o valor atual.
        val pillHeight by rememberUpdatedState(inner.calculateBottomPadding())
        NavHost(
            navController = navController,
            startDestination = startRoute ?: TopLevel.Today.route,
            // Em paisagem a câmera frontal fica na lateral: o conteúdo desvia dela.
            modifier = Modifier.fillMaxSize().layerBackdrop(backdrop).windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = { fadeIn(tween(if (reduced) PulsoMotion.QUICK else PulsoMotion.BASE)) },
            popExitTransition = exit,
        ) {
            val openTask: (Long) -> Unit = { navController.navigate(TaskDetailRoute(it)) }
            composable<TodayRoute> {
                TodayScreen(
                    contentPadding = PaddingValues(bottom = pillHeight),
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenCapture = { navController.navigate(CaptureRoute) },
                    onOpenAgenda = { navController.navigateToTab(TopLevel.Agenda) },
                    onOpenTasks = { navController.navigateToTab(TopLevel.Tasks) },
                    onOpenTask = openTask,
                    onPlanTomorrow = { sheet = SheetRequest.Create(NewItemKind.Task, taskDayOffset = 1) },
                )
            }
            composable<AgendaRoute> {
                AgendaScreen(
                    contentPadding = PaddingValues(bottom = pillHeight),
                    onNewEvent = { start -> sheet = SheetRequest.Create(NewItemKind.Event, eventStart = start) },
                    onEditEvent = { target -> sheet = SheetRequest.EditEvent(target) },
                )
            }
            composable<TasksRoute> {
                TasksScreen(contentPadding = PaddingValues(bottom = pillHeight), onOpenTask = openTask, onNewTask = { sheet = SheetRequest.Create(NewItemKind.Task) })
            }
            composable<FocusRoute> { FocusScreen(contentPadding = PaddingValues(bottom = pillHeight)) }
            composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable<CaptureRoute> { CaptureScreen(onClose = { navController.popBackStack() }) }
            composable<TaskDetailRoute> {
                TaskDetailScreen(
                    onBack = { navController.popBackStack() },
                    onFocus = { navController.navigateToTab(TopLevel.Focus) },
                )
            }
        }
    }
    }

    sheet?.let { request -> NewItemSheet(request, onDismiss = { sheet = null }) }
}

private fun NavHostController.navigateToTab(tab: TopLevel) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
