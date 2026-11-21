package com.nononsenseapps.feeder.ui.compose.feed.timeline

import com.nononsenseapps.feeder.ui.compose.modifiers.VisibilityInfo

/**
 * True once an item has left the screen at the bottom edge - or so nearly that only a sliver is
 * left. The last position reported before the list stops placing an item is rarely exactly zero.
 */
val VisibilityInfo.hasLeftAtBottom: Boolean
    get() = isScrolledOffBottom && visiblePercentage < LEFT_SCREEN_VISIBILITY

private const val LEFT_SCREEN_VISIBILITY = 0.1f
