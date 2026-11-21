package com.nononsenseapps.feeder.ui.compose.feed.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.db.room.ID_UNSET
import com.nononsenseapps.feeder.model.MediaImage
import com.nononsenseapps.feeder.ui.compose.coil.RestrainedCropScaling
import com.nononsenseapps.feeder.ui.compose.coil.rememberTintedVectorPainter
import com.nononsenseapps.feeder.ui.compose.feed.FeedItemDropdownMenu
import com.nononsenseapps.feeder.ui.compose.feed.FeedListItem
import com.nononsenseapps.feeder.ui.compose.feedarticle.wordsToReadTimeSecs
import com.nononsenseapps.feeder.ui.compose.minimumTouchSize
import com.nononsenseapps.feeder.ui.compose.text.WithBidiDeterminedLayoutDirection
import com.nononsenseapps.feeder.ui.compose.theme.FeedListItemDateStyle
import com.nononsenseapps.feeder.ui.compose.theme.FeedListItemSnippetTextStyle
import com.nononsenseapps.feeder.ui.compose.theme.FeedListItemTitleTextStyle
import com.nononsenseapps.feeder.ui.compose.theme.LocalDimens
import com.nononsenseapps.feeder.ui.compose.theme.PreviewTheme
import com.nononsenseapps.feeder.ui.compose.theme.titleFontWeight
import com.nononsenseapps.feeder.ui.compose.utils.PreviewThemes
import com.nononsenseapps.feeder.util.logDebug
import java.time.Instant

/**
 * Article style for the timeline: the feed icon on the left, next to it the feed name and time,
 * then title, text and the image in full width. No card - the list draws a divider between items.
 */
@Composable
fun FeedItemTimeline(
    item: FeedListItem,
    showThumbnail: Boolean,
    onOpenFeedItemInReader: () -> Unit,
    onOpenFeedItemInCustomTab: () -> Unit,
    onOpenFeedItemInBrowser: () -> Unit,
    onMarkAboveAsRead: () -> Unit,
    onMarkBelowAsRead: () -> Unit,
    onShareItem: () -> Unit,
    onToggleBookmark: () -> Unit,
    dropDownMenuExpanded: Boolean,
    onDismissDropdown: () -> Unit,
    bookmarkIndicator: Boolean,
    maxLines: Int,
    showOnlyTitle: Boolean,
    showReadingTime: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier
                    .requiredHeightIn(min = minimumTouchSize)
                    .alpha(if (!item.unread) 0.75f else 1.0f)
                    .padding(horizontal = LocalDimens.current.margin, vertical = 12.dp),
        ) {
            FeedAvatar(
                feedTitle = item.feedTitle,
                feedImageUrl = item.feedImageUrl?.toString(),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                // The menu opens below the header
                Box {
                    TimelineHeader(
                        item = item,
                        bookmarkIndicator = bookmarkIndicator,
                        showReadingTime = showReadingTime,
                    )
                    FeedItemDropdownMenu(
                        item = item,
                        dropDownMenuExpanded = dropDownMenuExpanded,
                        onDismissDropdown = onDismissDropdown,
                        onOpenFeedItemInReader = onOpenFeedItemInReader,
                        onOpenFeedItemInCustomTab = onOpenFeedItemInCustomTab,
                        onOpenFeedItemInBrowser = onOpenFeedItemInBrowser,
                        onMarkAboveAsRead = onMarkAboveAsRead,
                        onMarkBelowAsRead = onMarkBelowAsRead,
                        onShareItem = onShareItem,
                        onToggleBookmark = onToggleBookmark,
                    )
                }
                TimelineText(
                    item = item,
                    maxLines = maxLines,
                    showOnlyTitle = showOnlyTitle,
                )
                if (showThumbnail) {
                    TimelineImage(item = item)
                }
            }
        }
    }
}

/**
 * "Feed name · time · reading time" in a single line. The feed name is shortened first.
 */
@Composable
private fun TimelineHeader(
    item: FeedListItem,
    bookmarkIndicator: Boolean,
    showReadingTime: Boolean,
) {
    val details =
        buildList {
            if (item.pubDate.isNotBlank()) {
                add(item.pubDate)
            }
            if (showReadingTime) {
                val readTimeSecs = remember(item.wordCount) { wordsToReadTimeSecs(item.wordCount) }
                if (readTimeSecs > 0) {
                    val seconds = "%02d".format(readTimeSecs % 60)
                    add(
                        pluralStringResource(id = R.plurals.n_minutes, count = readTimeSecs / 60)
                            .format("${readTimeSecs / 60}:$seconds"),
                    )
                }
            }
        }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        WithBidiDeterminedLayoutDirection(paragraph = item.feedTitle) {
            Text(
                text = item.feedTitle,
                style = FeedListItemDateStyle(),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (details.isNotEmpty()) {
            Text(
                text = details.joinToString(separator = " · ", prefix = "· "),
                style = FeedListItemDateStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
        if (item.bookmarked && bookmarkIndicator) {
            Icon(
                Icons.Default.Star,
                contentDescription = stringResource(id = R.string.saved_article),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TimelineText(
    item: FeedListItem,
    maxLines: Int,
    showOnlyTitle: Boolean,
) {
    val snippetStyle = FeedListItemSnippetTextStyle()
    val text =
        remember(item, showOnlyTitle) {
            buildAnnotatedString {
                if (item.title.isNotBlank()) {
                    append(item.title)
                    if (!showOnlyTitle && item.snippet.isNotBlank()) {
                        withStyle(snippetStyle.toSpanStyle().copy(fontWeight = FontWeight.Normal)) {
                            append('\n')
                            append(item.snippet)
                        }
                    }
                } else {
                    // Some feeds have no titles - always show the text then
                    append(item.snippet)
                }
            }
        }
    WithBidiDeterminedLayoutDirection(paragraph = text.text) {
        Text(
            text = text,
            style = FeedListItemTitleTextStyle(),
            fontWeight = titleFontWeight(item.unread),
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TimelineImage(item: FeedListItem) {
    val image = item.image ?: return
    // Known tiny images are tracking pixels or spacers
    if ((image.width ?: 1000) < 10 || (image.height ?: 1000) < 10) {
        return
    }
    val pixelDensity = LocalDensity.current.density
    val contentScale = remember(pixelDensity) { RestrainedCropScaling(pixelDensity) }
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
    ) {
        val pixels =
            with(LocalDensity.current) {
                val pxWidth = maxWidth.roundToPx()
                Size(pxWidth, (pxWidth * 9) / 16)
            }
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalContext.current)
                    .data(image.url)
                    .listener(
                        onError = { a, b ->
                            logDebug(LOG_TAG, "error ${a.data}", b.throwable)
                        },
                    ).scale(Scale.FIT)
                    .size(pixels)
                    .precision(Precision.INEXACT)
                    .build(),
            placeholder = rememberTintedVectorPainter(Icons.Outlined.Terrain),
            error = rememberTintedVectorPainter(Icons.Outlined.ErrorOutline),
            contentDescription = stringResource(id = R.string.article_image),
            contentScale = contentScale,
            alignment = Alignment.Center,
            modifier =
                Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .fillMaxWidth()
                    .aspectRatio(16.0f / 9.0f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

/**
 * The feed icon in a circle. The first letter of the feed name shows while the icon loads, and
 * stays when the feed has no icon.
 */
@Composable
private fun FeedAvatar(
    feedTitle: String,
    feedImageUrl: String?,
) {
    val pixels = with(LocalDensity.current) { AVATAR_SIZE.roundToPx() }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Text(
            text = feedTitle.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        if (feedImageUrl != null) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(feedImageUrl)
                        .listener(
                            onError = { a, b ->
                                logDebug(LOG_TAG, "error ${a.data}", b.throwable)
                            },
                        ).scale(Scale.FIT)
                        .size(pixels)
                        .precision(Precision.INEXACT)
                        .build(),
                // Not listed in screen readers - the feed name is next to it
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(AVATAR_SIZE),
            )
        }
    }
}

private val AVATAR_SIZE = 40.dp
private const val LOG_TAG = "FEEDER_TIMELINE"

@Composable
@PreviewThemes
@Suppress("ktlint:standard:max-line-length")
private fun PreviewUnreadWithImage() {
    PreviewTheme {
        Box(modifier = Modifier.width(400.dp)) {
            FeedItemTimeline(
                item =
                    FeedListItem(
                        title = "A very interesting article about something which happened today",
                        snippet = "The text of the article starts here and goes on for quite a while, much longer than fits into the few lines the list shows of it.",
                        feedTitle = "Some News Feed",
                        pubDate = "17:09",
                        unread = true,
                        image = MediaImage("blabla"),
                        link = null,
                        id = ID_UNSET,
                        bookmarked = true,
                        feedImageUrl = null,
                        primarySortTime = Instant.EPOCH,
                        rawPubDate = null,
                        wordCount = 900,
                    ),
                showThumbnail = true,
                onOpenFeedItemInReader = {},
                onOpenFeedItemInCustomTab = {},
                onOpenFeedItemInBrowser = {},
                onMarkAboveAsRead = {},
                onMarkBelowAsRead = {},
                onShareItem = {},
                onToggleBookmark = {},
                dropDownMenuExpanded = false,
                onDismissDropdown = {},
                bookmarkIndicator = true,
                maxLines = 4,
                showOnlyTitle = false,
                showReadingTime = true,
            )
        }
    }
}

@Composable
@PreviewThemes
@Suppress("ktlint:standard:max-line-length")
private fun PreviewRead() {
    PreviewTheme {
        Box(modifier = Modifier.width(400.dp)) {
            FeedItemTimeline(
                item =
                    FeedListItem(
                        title = "An article which has been read already",
                        snippet = "Its text is dimmed together with the rest of the item.",
                        feedTitle = "A feed with a really long name which does not fit into the header",
                        pubDate = "21.11.2026",
                        unread = false,
                        image = null,
                        link = null,
                        id = ID_UNSET,
                        bookmarked = false,
                        feedImageUrl = null,
                        primarySortTime = Instant.EPOCH,
                        rawPubDate = null,
                        wordCount = 0,
                    ),
                showThumbnail = true,
                onOpenFeedItemInReader = {},
                onOpenFeedItemInCustomTab = {},
                onOpenFeedItemInBrowser = {},
                onMarkAboveAsRead = {},
                onMarkBelowAsRead = {},
                onShareItem = {},
                onToggleBookmark = {},
                dropDownMenuExpanded = false,
                onDismissDropdown = {},
                bookmarkIndicator = true,
                maxLines = 4,
                showOnlyTitle = false,
                showReadingTime = false,
            )
        }
    }
}
