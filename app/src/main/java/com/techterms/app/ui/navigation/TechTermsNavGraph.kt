package com.techterms.app.ui.navigation

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.techterms.app.AppContainer
import com.techterms.app.R
import com.techterms.app.data.db.Term
import com.techterms.app.ui.browse.BrowseScreen
import com.techterms.app.ui.browse.BrowseViewModel
import com.techterms.app.ui.detail.TermDetailBottomSheet
import com.techterms.app.ui.home.HomeScreen
import com.techterms.app.ui.home.HomeViewModel
import com.techterms.app.ui.settings.SettingsScreen
import kotlinx.coroutines.CancellationException

private const val TAG = "TechTerms"
private const val ROUTE_BROWSE = "browse"

private enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    SETTINGS("settings", R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun TechTermsNavGraph(
    container: AppContainer,
    deepLinkTermId: Int?,
    onDeepLinkConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            appContext = context.applicationContext,
            termRepository = container.termRepository,
            settingsDataStore = container.settingsDataStore
        )
    )
    val browseViewModel: BrowseViewModel = viewModel(
        factory = BrowseViewModel.provideFactory(
            termRepository = container.termRepository
        )
    )

    var selectedTermId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedTerm by remember { mutableStateOf<Term?>(null) }

    LaunchedEffect(deepLinkTermId) {
        if (deepLinkTermId != null && deepLinkTermId > 0) {
            selectedTermId = deepLinkTermId
            onDeepLinkConsumed()
        }
    }

    LaunchedEffect(selectedTermId) {
        val id = selectedTermId
        if (id != null && id > 0) {
            val loaded = try {
                container.termRepository.getTermById(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load selected term id=$id", e)
                null
            }
            selectedTerm = loaded
            if (loaded == null) {
                selectedTermId = null
            }
        } else {
            selectedTerm = null
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != ROUTE_BROWSE

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        TopLevelDestination.entries.forEach { destination ->
                            val selected = currentRoute == destination.route
                            val label = stringResource(destination.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) {
                                            destination.selectedIcon
                                        } else {
                                            destination.unselectedIcon
                                        },
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(durationMillis = 200)) },
            exitTransition = { fadeOut(animationSpec = tween(durationMillis = 200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(durationMillis = 200)) },
            popExitTransition = { fadeOut(animationSpec = tween(durationMillis = 200)) }
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onTermClick = { term -> selectedTermId = term.id },
                    onBrowse = { request ->
                        browseViewModel.applyFilter(
                            category = request.category,
                            difficulty = request.difficulty,
                            devOnly = request.devOnly
                        )
                        navController.navigate(ROUTE_BROWSE) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(TopLevelDestination.SETTINGS.route) {
                SettingsScreen(viewModel = homeViewModel)
            }
            composable(ROUTE_BROWSE) {
                BrowseScreen(
                    viewModel = browseViewModel,
                    onTermClick = { term -> selectedTermId = term.id },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    selectedTerm?.let { term ->
        TermDetailBottomSheet(
            term = term,
            onDismiss = {
                selectedTermId = null
                selectedTerm = null
            }
        )
    }
}
