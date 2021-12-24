package com.composeissue

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberHomeAnimationState(
    currentScreen: HomeScreen,
    initialMenuState: MenuState = MenuState.Closed,
    initialCardState: CardState = CardState.Collapsed
): HomeAnimationState {
    val horizontalSwipeableState = rememberSwipeableState(initialValue = initialMenuState)
    val verticalSwipeableState = rememberSwipeableState(initialValue = initialCardState)
    val coroutineScope = rememberCoroutineScope()
    return remember(initialCardState, initialMenuState, currentScreen, coroutineScope) {
        HomeAnimationState(
            currentScreen = currentScreen,
            horizontalSwipeState = horizontalSwipeableState,
            verticalSwipeState = verticalSwipeableState,
            coroutineScope = coroutineScope
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
class HomeAnimationState(
    val currentScreen: HomeScreen,
    val horizontalSwipeState: SwipeableState<MenuState>,
    val verticalSwipeState: SwipeableState<CardState>,
    private val coroutineScope: CoroutineScope
) {
    private var previousCardState = verticalSwipeState.currentValue

    init {
        coroutineScope.launch {
            if (currentScreen == HomeScreen.Favorites) {
                verticalSwipeState.animateTo(CardState.Expanded)
            }
            horizontalSwipeState.animateTo(MenuState.Closed)
        }
    }

    val allowSwipeState by derivedStateOf {
        horizontalSwipeState.currentValue != MenuState.Opened &&
                currentScreen == HomeScreen.Home
    }

    val collapsedCardProgress by derivedStateOf {
        when (verticalSwipeState.progress.from) {
            CardState.Collapsed -> if (verticalSwipeState.progress.fraction == 1f) 1f
            else 1 - verticalSwipeState.progress.fraction
            CardState.Expanded -> if (verticalSwipeState.progress.fraction == 1f) 0f
            else verticalSwipeState.progress.fraction
        }
    }

    val toolbarState by derivedStateOf {
        val targetValue = if (horizontalSwipeState.isAnimationRunning)
            horizontalSwipeState.targetValue
        else
            horizontalSwipeState.currentValue

        when {
            targetValue == MenuState.Closed && currentScreen == HomeScreen.Home ->
                ToolbarState.Main
            targetValue == MenuState.Closed && currentScreen == HomeScreen.Favorites ->
                ToolbarState.Favorites
            targetValue == MenuState.Closed && currentScreen == HomeScreen.Search ->
                ToolbarState.Search
            targetValue == MenuState.Closed && currentScreen == HomeScreen.FavoritesSearch ->
                ToolbarState.FavoritesSearch
            targetValue == MenuState.Opened && currentScreen == HomeScreen.Home ->
                ToolbarState.MainClosed
            targetValue == MenuState.Opened && currentScreen == HomeScreen.Favorites ->
                ToolbarState.Favorites
            targetValue == MenuState.Opened && currentScreen == HomeScreen.Search ->
                ToolbarState.MainClosed
            targetValue == MenuState.Opened && currentScreen == HomeScreen.FavoritesSearch ->
                ToolbarState.MainClosed
            else -> {
                ToolbarState.Main
            }
        }
    }

    init {
        coroutineScope.launch {
            snapshotFlow { horizontalSwipeState.isAnimationRunning }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    transition()
                }
        }
    }

    fun transitionMenu() {
        coroutineScope.launch {
            when (horizontalSwipeState.currentValue) {
                MenuState.Opened -> horizontalSwipeState.animateTo(MenuState.Closed)
                MenuState.Closed -> horizontalSwipeState.animateTo(MenuState.Opened)
            }
        }
    }

    private suspend fun transition() {
        val targetHorizontalState = horizontalSwipeState.targetValue
        val currentVerticalState = verticalSwipeState.currentValue

        when {
            targetHorizontalState == MenuState.Opened &&
                    currentVerticalState == CardState.Expanded -> {
                previousCardState = CardState.Expanded
                verticalSwipeState.animateTo(CardState.Collapsed)
            }
            targetHorizontalState == MenuState.Closed &&
                    previousCardState == CardState.Expanded -> {
                verticalSwipeState.animateTo(CardState.Expanded)
            }
            targetHorizontalState == MenuState.Opened &&
                    currentVerticalState == CardState.Collapsed -> {
                previousCardState = CardState.Collapsed
            }
        }
    }
}

enum class MenuState {
    Opened, Closed
}

enum class CardState {
    Collapsed,
    Expanded
}

enum class ToolbarState {
    Main,
    MainClosed,
    Favorites,
    FavoritesSearch,
    Search
}

enum class HomeScreen {
    Home,
    Favorites,
    Search,
    FavoritesSearch
}