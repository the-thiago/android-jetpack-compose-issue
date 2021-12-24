package com.composeissue

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy

@Composable
fun HomeScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    homeMenu: @Composable (contentOffset: Int) -> Unit,
    content: @Composable (constraints: Constraints) -> Unit
) {
    val menuOffset = with(LocalDensity.current) { 24.dp.roundToPx() }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        layout(layoutWidth, layoutHeight) {
            val topBarPlaceables = subcompose(HomeScaffoldLayoutContent.TopBar, topBar).fastMap {
                it.measure(looseConstraints)
            }

            val topBarHeight = topBarPlaceables.fastMaxBy { it.height }?.height ?: 0

            val bodyContentMinHeight = layoutHeight - topBarHeight
            val bodyContentMaxHeight = layoutHeight - (topBarHeight / 2)
            val bodyContentConstraints = constraints.copy(
                minHeight = bodyContentMinHeight,
                maxHeight = bodyContentMaxHeight
            )

            val bodyContentPlaceables = subcompose(HomeScaffoldLayoutContent.MainContent) {
                content(bodyContentConstraints)
            }.fastMap { it.measure(bodyContentConstraints) }

            val menuPlaceables = subcompose(HomeScaffoldLayoutContent.Menu) {
                homeMenu(topBarHeight)
            }.fastMap {
                it.measure(bodyContentConstraints.copy(maxHeight = bodyContentMaxHeight))
            }

            topBarPlaceables.fastForEach {
                it.place(0, 0)
            }
            menuPlaceables.fastForEach {
                it.place(0, 0)
            }
            bodyContentPlaceables.fastForEach {
                it.place(0, 0)
            }
        }
    }
}

private enum class HomeScaffoldLayoutContent { TopBar, MainContent, Menu }