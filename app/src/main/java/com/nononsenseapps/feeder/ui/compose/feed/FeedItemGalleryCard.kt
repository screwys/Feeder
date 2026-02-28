package com.nononsenseapps.feeder.ui.compose.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.nononsenseapps.feeder.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.nononsenseapps.feeder.ui.compose.coil.rememberTintedVectorPainter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedItemGalleryCard(
    item: FeedListItem,
    onItemClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onLongClick: () -> Unit,
    onOpenFeedItemInReader: () -> Unit,
    onOpenFeedItemInCustomTab: () -> Unit,
    onOpenFeedItemInBrowser: () -> Unit,
    onMarkAboveAsRead: () -> Unit,
    onMarkBelowAsRead: () -> Unit,
    onShareItem: () -> Unit,
    dropDownMenuExpanded: Boolean,
    onDismissDropdown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageCount = remember(item.title) {
        Regex("""\((\d+) pages?\)""").find(item.title)?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    val imageUrl = item.image?.url ?: item.feedImageUrl?.toString()

    // Use Box so we can layer the bookmark button on top of the card
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(if (!item.unread) 0.75f else 1.0f),
    ) {
        ElevatedCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onItemClick,
                        onLongClick = onLongClick,
                    ),
        ) {
            Column {
                Box {
                    if (imageUrl != null) {
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .size(Size(512, 512))
                                    .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            placeholder = rememberTintedVectorPainter(Icons.Outlined.Terrain),
                            error = rememberTintedVectorPainter(Icons.Outlined.ErrorOutline),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Terrain,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    }

                    if (imageCount > 1) {
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "$imageCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    if (item.snippet.isNotBlank()) {
                        Text(
                            text = item.snippet,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (item.author != null) {
                        Text(
                            text = item.author,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                GalleryDropdownMenu(
                    expanded = dropDownMenuExpanded,
                    onDismiss = onDismissDropdown,
                    onOpenInReader = onOpenFeedItemInReader,
                    onOpenInCustomTab = onOpenFeedItemInCustomTab,
                    onOpenInBrowser = onOpenFeedItemInBrowser,
                    onMarkAboveAsRead = onMarkAboveAsRead,
                    onMarkBelowAsRead = onMarkBelowAsRead,
                    onShare = onShareItem,
                )
            }
        }

    }
}

@Composable
private fun GalleryDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpenInReader: () -> Unit,
    onOpenInCustomTab: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onMarkAboveAsRead: () -> Unit,
    onMarkBelowAsRead: () -> Unit,
    onShare: () -> Unit,
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.open_article_in_reader)) },
            onClick = {
                onDismiss()
                onOpenInReader()
            },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.open_article_in_default_browser)) },
            onClick = {
                onDismiss()
                onOpenInBrowser()
            },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.open_article_in_custom_tab)) },
            onClick = {
                onDismiss()
                onOpenInCustomTab()
            },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.mark_items_above_as_read)) },
            onClick = {
                onDismiss()
                onMarkAboveAsRead()
            },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.mark_items_below_as_read)) },
            onClick = {
                onDismiss()
                onMarkBelowAsRead()
            },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.share)) },
            onClick = {
                onDismiss()
                onShare()
            },
        )
    }
}
