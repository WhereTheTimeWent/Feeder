package com.nononsenseapps.feeder.ui.compose.feed.timeline

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.paging.compose.LazyPagingItems
import com.nononsenseapps.feeder.archmodel.TimelineReadingEdge
import com.nononsenseapps.feeder.ui.compose.feed.FeedListItem
import com.nononsenseapps.feeder.ui.compose.feed.FeedOrTag
import com.nononsenseapps.feeder.util.logDebug
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Opens the timeline at the reading edge: the oldest unread article is placed at the bottom of the
 * screen with the newer unread articles above it. With nothing unread the list opens at the top.
 *
 * The position is derived from the read status instead of being stored, so it survives the process
 * being killed and articles being marked as read elsewhere. It is applied once per feed or tag -
 * [isPositioned] and [setPositioned] decide when it has been done, so returning from an article
 * keeps the position the user scrolled to.
 */
@Composable
fun TimelineReadingEdgeEffect(
    enabled: Boolean,
    feedOrTag: FeedOrTag,
    pagedFeedItems: LazyPagingItems<FeedListItem>,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    isPositioned: (FeedOrTag) -> Boolean,
    setPositioned: (FeedOrTag) -> Unit,
    findReadingEdge: suspend () -> TimelineReadingEdge?,
) {
    // The list state instance changes once items have loaded, see rememberLazyListState
    val currentListState by rememberUpdatedState(listState)
    val currentGridState by rememberUpdatedState(gridState)
    val currentIsPositioned by rememberUpdatedState(isPositioned)
    val currentSetPositioned by rememberUpdatedState(setPositioned)
    val currentFindReadingEdge by rememberUpdatedState(findReadingEdge)

    LaunchedEffect(enabled, feedOrTag) {
        if (!enabled || currentIsPositioned(feedOrTag)) {
            return@LaunchedEffect
        }

        val index = findLoadedReadingEdge(pagedFeedItems, currentFindReadingEdge)
        logDebug(LOG_TAG, "Reading edge for $feedOrTag at $index")

        val positioned =
            withTimeoutOrNull(LOAD_TIMEOUT_MS) {
                while (true) {
                    when {
                        currentListState.layoutInfo.viewportSize.height > 0 -> {
                            currentListState.scrollToBottomEdge(index)
                            break
                        }

                        currentGridState.layoutInfo.viewportSize.height > 0 -> {
                            currentGridState.scrollToBottomEdge(index)
                            break
                        }

                        else -> delay(POLL_INTERVAL_MS)
                    }
                }
            }

        if (positioned != null) {
            currentSetPositioned(feedOrTag)
        }
    }
}

/**
 * Returns the index of the reading edge once the paged list shows it, or null if nothing is
 * unread. The list may still show the previous feed for a moment, and a sync may shift the edge,
 * so the edge is looked up again if the loaded list does not match.
 */
private suspend fun findLoadedReadingEdge(
    pagedFeedItems: LazyPagingItems<FeedListItem>,
    findReadingEdge: suspend () -> TimelineReadingEdge?,
): Int? {
    var lastIndex: Int? = null
    repeat(MAX_ATTEMPTS) {
        val edge = findReadingEdge() ?: return null
        lastIndex = edge.index
        val loaded =
            withTimeoutOrNull(LOAD_TIMEOUT_MS) {
                while (!pagedFeedItems.isLoadedAround(edge)) {
                    delay(POLL_INTERVAL_MS)
                }
                true
            }
        if (loaded != null) {
            return edge.index
        }
    }
    // Best effort
    return lastIndex
}

/**
 * True when the edge item is at the expected index and the items just above it are loaded too -
 * placeholders have a different height than the real items and would shift the edge out of view.
 */
private fun LazyPagingItems<FeedListItem>.isLoadedAround(edge: TimelineReadingEdge): Boolean {
    if (edge.index >= itemCount) {
        return false
    }
    // Accessing an item makes paging load the pages around it
    if (get(edge.index)?.id != edge.itemId) {
        return false
    }
    val firstAbove = (edge.index - ITEMS_ABOVE_EDGE_TO_LOAD).coerceAtLeast(0)
    get(firstAbove)
    return (firstAbove until edge.index).all { peek(it) != null }
}

private suspend fun LazyListState.scrollToBottomEdge(index: Int?) {
    if (index == null) {
        scrollToItem(0)
        return
    }
    scrollToItem(index)
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val gap = (info.viewportEndOffset - info.afterContentPadding) - (item.offset + item.size)
    if (gap > 0) {
        scrollBy(-gap.toFloat())
    }
}

private suspend fun LazyStaggeredGridState.scrollToBottomEdge(index: Int?) {
    if (index == null) {
        scrollToItem(0)
        return
    }
    scrollToItem(index)
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val gap = (info.viewportEndOffset - info.afterContentPadding) - (item.offset.y + item.size.height)
    if (gap > 0) {
        scrollBy(-gap.toFloat())
    }
}

private const val LOG_TAG = "FEEDER_TIMELINE"
private const val MAX_ATTEMPTS = 3
private const val LOAD_TIMEOUT_MS = 2_000L
private const val POLL_INTERVAL_MS = 50L
private const val ITEMS_ABOVE_EDGE_TO_LOAD = 20
