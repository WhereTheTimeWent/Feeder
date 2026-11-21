package com.nononsenseapps.feeder.archmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.nononsenseapps.feeder.db.room.AppDatabase
import com.nononsenseapps.feeder.db.room.BlocklistDao
import com.nononsenseapps.feeder.db.room.Feed
import com.nononsenseapps.feeder.db.room.FeedItem
import com.nononsenseapps.feeder.db.room.FeedItemDao
import com.nononsenseapps.feeder.db.room.ID_UNSET
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.net.URL
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
@MediumTest
class FeedItemStoreTimelineTest : DIAware {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase

    override val di =
        DI.lazy {
            bind<AppDatabase>() with instance(db)
            bind<FeedItemDao>() with singleton { db.feedItemDao() }
            bind<BlocklistDao>() with singleton { db.blocklistDao() }
            bind<FeedItemStore>() with singleton { FeedItemStore(di) }
        }

    private val store: FeedItemStore by instance()

    private val firstSync = Instant.parse("2026-11-20T10:00:00Z")
    private val secondSync = Instant.parse("2026-11-21T10:00:00Z")

    private var feedId = ID_UNSET

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        feedId =
            runBlocking {
                db.feedDao().insertFeed(Feed(title = "Timeline", url = URL("https://example.com/timeline")))
            }
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * Inserts four items. In timeline order they are: D, C, B, A. C arrived in the second sync
     * with the oldest publication date, so sorting by date alone would put it last.
     */
    private fun insertItems(
        unreadA: Boolean,
        unreadB: Boolean,
        unreadC: Boolean,
        unreadD: Boolean,
    ): Map<String, Long> =
        runBlocking {
            mapOf(
                "A" to insert("A", firstSync, hour = 10, unread = unreadA),
                "B" to insert("B", firstSync, hour = 11, unread = unreadB),
                "C" to insert("C", secondSync, hour = 9, unread = unreadC),
                "D" to insert("D", secondSync, hour = 12, unread = unreadD),
            )
        }

    private suspend fun insert(
        guid: String,
        firstSyncedTime: Instant,
        hour: Int,
        unread: Boolean,
    ): Long {
        val pubDate = ZonedDateTime.of(2026, 11, 19, hour, 0, 0, 0, ZoneOffset.UTC)
        return db.feedItemDao().insertFeedItem(
            FeedItem(
                guid = guid,
                plainTitle = guid,
                feedId = feedId,
                pubDate = pubDate,
                firstSyncedTime = firstSyncedTime,
                primarySortTime = minOf(firstSyncedTime, pubDate.toInstant()),
                readTime = if (unread) null else Instant.EPOCH,
            ),
        )
    }

    private fun isUnread(id: Long): Boolean = runBlocking { db.feedItemDao().loadFeedItem(id)!!.readTime == null }

    private suspend fun readingEdge() =
        store.getTimelineReadingEdge(
            feedId = feedId,
            tag = "",
            minReadTime = Instant.EPOCH,
            filter = timelineFeedListFilter,
            search = "",
        )

    @Test
    fun readingEdgeIsOldestUnreadInArrivalOrder() {
        val ids = insertItems(unreadA = false, unreadB = true, unreadC = true, unreadD = false)

        val edge = runBlocking { readingEdge() }

        assertEquals(TimelineReadingEdge(index = 2, itemId = ids.getValue("B")), edge)
    }

    @Test
    fun readingEdgeIsNullWhenEverythingIsRead() {
        insertItems(unreadA = false, unreadB = false, unreadC = false, unreadD = false)

        assertNull(runBlocking { readingEdge() })
    }

    @Test
    fun unreadCountAboveFollowsTimelineOrder() {
        val ids = insertItems(unreadA = true, unreadB = true, unreadC = true, unreadD = false)

        val counts =
            runBlocking {
                ids.mapValues { (_, id) ->
                    store
                        .getTimelineUnreadCountAbove(
                            feedId = feedId,
                            tag = "",
                            minReadTime = Instant.EPOCH,
                            filter = timelineFeedListFilter,
                            search = "",
                            itemId = id,
                        ).first()
                }
            }

        assertEquals(mapOf("D" to 0, "C" to 0, "B" to 1, "A" to 2), counts)
    }

    @Test
    fun markAboveAsReadFollowsTimelineOrder() {
        val ids = insertItems(unreadA = true, unreadB = true, unreadC = true, unreadD = true)

        runBlocking {
            store.markAsReadRaw(
                feedId = feedId,
                tag = "",
                filter = timelineFeedListFilter,
                search = "",
                minReadTime = Instant.EPOCH,
                descending = false,
                cursor = db.feedItemDao().loadFeedItem(ids.getValue("B"))!!,
                timeline = true,
            )
        }

        assertEquals(
            mapOf("A" to true, "B" to true, "C" to false, "D" to false),
            ids.mapValues { isUnread(it.value) },
        )
    }

    @Test
    fun markBelowAsReadFollowsTimelineOrder() {
        val ids = insertItems(unreadA = true, unreadB = true, unreadC = true, unreadD = true)

        runBlocking {
            store.markAsReadRaw(
                feedId = feedId,
                tag = "",
                filter = timelineFeedListFilter,
                search = "",
                minReadTime = Instant.EPOCH,
                descending = true,
                cursor = db.feedItemDao().loadFeedItem(ids.getValue("C"))!!,
                timeline = true,
            )
        }

        assertEquals(
            mapOf("A" to false, "B" to false, "C" to true, "D" to true),
            ids.mapValues { isUnread(it.value) },
        )
    }
}
