package com.composeissue

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlin.math.roundToInt

@Composable
fun Home() {
    val currentScreen by rememberSaveable { mutableStateOf(HomeScreen.Home) }
    val homeAnimationState = rememberHomeAnimationState(currentScreen)

    HomeScaffold(
        topBar = { HomeToolbar() },
        homeMenu = { maxOffset ->
            val offset by derivedStateOf {
                Offset(
                    x = 0f,
                    y = maxOffset.toFloat() / (2 - homeAnimationState.collapsedCardProgress)
                )
            }
            val paddingDistance = with(LocalDensity.current) { maxOffset.toDp() }
            HomeMenu(
                modifier = Modifier.offset {
                    IntOffset(x = 0, y = offset.y.roundToInt())
                },
                maxOffset = paddingDistance
            )
        }
    ) { constraints ->
        HomeContent(constraints = constraints, homeAnimationState = homeAnimationState)
    }
}

@Composable
fun HomeToolbar() {
    Column {
        repeat(6) {
            Text(text = "This is the toolbar $it")
        }
    }
}

@Composable
fun HomeMenu(
    modifier: Modifier = Modifier,
    maxOffset: Dp
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 72.dp, top = 6.dp, bottom = maxOffset / 2)
    ) {
        repeat(20) {
            Text(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .background(Color.LightGray, RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                text = "Menu Item $it"
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeContent(
    constraints: Constraints,
    homeAnimationState: HomeAnimationState
) {
    val toolBarHeight = (constraints.maxHeight - constraints.minHeight).toFloat() * 2
    val listState = rememberLazyListState()
    val nestedScrollConnection = remember {
        getNestedScrollConnection(listState, homeAnimationState.verticalSwipeState)
    }

    SwipeRefresh(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = homeAnimationState.horizontalSwipeState.offset.value.roundToInt(),
                    y = homeAnimationState.verticalSwipeState.offset.value.roundToInt()
                )
            }
            .swipeable(
                state = homeAnimationState.horizontalSwipeState,
                anchors = mapOf(
                    0f to MenuState.Closed,
                    (constraints.maxWidth * 0.95f) to MenuState.Opened
                ),
                orientation = Orientation.Horizontal,
                resistance = null
            )
            .swipeable(
                enabled = listState.firstVisibleItemIndex < 3 && homeAnimationState.allowSwipeState,
                state = homeAnimationState.verticalSwipeState,
                orientation = Orientation.Vertical,
                anchors = mapOf(
                    toolBarHeight to CardState.Collapsed,
                    toolBarHeight / 2 to CardState.Expanded,
                )
            )
            .nestedScroll(nestedScrollConnection)
            .background(color = Color.Blue, shape = RoundedCornerShape(topStart = 36.dp))
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        state = rememberSwipeRefreshState(isRefreshing = false),
        onRefresh = { /* ... */ },
        indicator = { refreshState, trigger ->
            SwipeRefreshIndicator(
                state = refreshState,
                refreshTriggerDistance = trigger,
                contentColor = MaterialTheme.colors.primary
            )
        }
    ) {
        FakeList(modifier = Modifier.fillMaxSize(), listState = listState)
    }
}

@Composable
private fun FakeList(
    modifier: Modifier = Modifier,
    listState: LazyListState,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(top = 8.dp)
    ) {
        items(150) {
            Text(text = "Fake item $it")
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
private fun getNestedScrollConnection(
    listState: LazyListState,
    swipeableState: SwipeableState<CardState>
) = object : NestedScrollConnection {

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.y
        return if (delta < 0) {
            swipeableState.performDrag(delta).toOffset()
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.y
        return swipeableState.performDrag(delta).toOffset()
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return if (available.y < 0 && listState.firstVisibleItemIndex == 0 &&
            listState.firstVisibleItemScrollOffset == 0
        ) {
            swipeableState.performFling(available.y)
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        swipeableState.performFling(velocity = available.y)
        return super.onPostFling(consumed, available)
    }

    private fun Float.toOffset() = Offset(0f, this)
}