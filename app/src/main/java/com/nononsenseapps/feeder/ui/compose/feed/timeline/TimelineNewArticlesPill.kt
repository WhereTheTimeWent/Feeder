package com.nononsenseapps.feeder.ui.compose.feed.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.ui.compose.feed.FeedListItem
import com.nononsenseapps.feeder.ui.compose.feed.FeedOrTag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Floating "↑ N new" pill shown at the top of the timeline while unread articles are listed above
 * the visible ones. Tapping it jumps to the newest article. Nothing is marked as read by the jump:
 * the skipped articles stay unread, and so do the ones which were on screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun TimelineNewArticlesPill(
    enabled: Boolean,
    feedOrTag: FeedOrTag,
    pagedFeedItems: LazyPagingItems<FeedListItem>,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    countUnreadAbove: (itemId: Long) -> Flow<Int>,
    modifier: Modifier = Modifier,
) {
    // The list state instance changes once items have loaded, see rememberLazyListState
    val currentListState by rememberUpdatedState(listState)
    val currentGridState by rememberUpdatedState(gridState)
    val currentCountUnreadAbove by rememberUpdatedState(countUnreadAbove)

    val unreadAbove by remember(enabled, feedOrTag, pagedFeedItems) {
        if (!enabled) {
            flowOf(0)
        } else {
            snapshotFlow {
                currentFirstVisibleIndex(currentListState, currentGridState)
                    ?.takeIf { index -> index < pagedFeedItems.itemCount }
                    ?.let { index -> pagedFeedItems.peek(index)?.id }
            }.distinctUntilChanged()
                .flatMapLatest { itemId ->
                    when (itemId) {
                        null -> flowOf(0)
                        else -> currentCountUnreadAbove(itemId)
                    }
                }
        }
    }.collectAsState(initial = 0)
    // Keeps showing the last count while the pill fades out instead of "0 new"
    val shownCount = remember { IntArray(1) }
    if (unreadAbove > 0) {
        shownCount[0] = unreadAbove
    }

    val coroutineScope = rememberCoroutineScope()
    val scrollLabel = stringResource(R.string.timeline_scroll_to_newest)

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = enabled && unreadAbove > 0,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 4.dp,
                modifier =
                    Modifier
                        .padding(top = 8.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClickLabel = scrollLabel,
                            role = Role.Button,
                        ) {
                            coroutineScope.launch {
                                when {
                                    currentListState.layoutInfo.viewportSize.height > 0 -> currentListState.scrollToItem(0)
                                    currentGridState.layoutInfo.viewportSize.height > 0 -> currentGridState.scrollToItem(0)
                                }
                            }
                        },
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.timeline_new_articles, shownCount[0]),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/**
 * Index of the first visible item of whichever layout is in use. The timeline list has no extra
 * items in front, so the index matches the paged items.
 */
private fun currentFirstVisibleIndex(
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
): Int? =
    when {
        listState.layoutInfo.viewportSize.height > 0 -> listState.firstVisibleItemIndex
        gridState.layoutInfo.viewportSize.height > 0 -> gridState.firstVisibleItemIndex
        else -> null
    }
