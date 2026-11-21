package com.nononsenseapps.feeder.archmodel

import com.nononsenseapps.feeder.ui.compose.feedarticle.FeedListFilter

/**
 * Timeline mode turns the article list into one continuous, chronological timeline: newest
 * arrivals on top, read articles stay visible, and articles are marked as read once they have been
 * seen and leave the screen at the bottom.
 */
const val PREF_TIMELINE_MODE = "pref_timeline_mode"

/**
 * Timeline mode lists everything - read articles are dimmed, never hidden.
 */
val timelineFeedListFilter: FeedListFilter =
    object : FeedListFilter {
        override val unread: Boolean = true
        override val saved: Boolean = false
        override val recentlyRead: Boolean = true
        override val read: Boolean = true
    }

/**
 * Orders articles by when they arrived on the device rather than by publication date. All items
 * fetched in one sync share the same first_synced_time, so within one sync they are ordered by
 * their publication date as usual.
 *
 * Sorting by publication date alone would place an article with an old date that is fetched late
 * below the position the user has already read up to - and it would never be seen.
 */
const val FEED_ITEM_LIST_SORT_ORDER_TIMELINE =
    "first_synced_time DESC, primary_sort_time DESC, pub_date DESC, feed_items.id DESC"

/**
 * The oldest unread article in the timeline and its position in the list.
 */
data class TimelineReadingEdge(
    val index: Int,
    val itemId: Long,
)
