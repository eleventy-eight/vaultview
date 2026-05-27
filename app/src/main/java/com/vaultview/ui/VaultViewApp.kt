package com.vaultview.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.vaultview.model.MediaItem
import com.vaultview.model.MediaType

@Composable
fun VaultViewApp(viewModel: BrowseViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BrowseScreen(
            state = state,
            onOpenItem = viewModel::openItem,
            onOpenHome = { viewModel.openFolder("/") }
        )

        val selected = state.selectedItem
        val streamUrl = state.streamUrl
        if (selected != null && streamUrl != null) {
            when (selected.type) {
                MediaType.Image -> ImageViewer(
                    item = selected,
                    imageUrl = streamUrl,
                    onClose = viewModel::closeViewer,
                    onPrevious = viewModel::showPreviousImage,
                    onNext = viewModel::showNextImage
                )

                MediaType.Video -> VideoPlayer(
                    item = selected,
                    streamUrl = streamUrl,
                    onClose = viewModel::closeViewer
                )

                MediaType.Folder -> Unit
            }
        }
    }
}

@Composable
private fun BrowseScreen(
    state: BrowseUiState,
    onOpenItem: (MediaItem) -> Unit,
    onOpenHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 40.dp)
    ) {
        Header(state = state, onOpenHome = onOpenHome)

        Spacer(modifier = Modifier.height(28.dp))

        when {
            state.isLoading -> LoadingState()
            state.errorMessage != null -> ErrorState(message = state.errorMessage)
            state.items.isEmpty() -> EmptyState()
            else -> MediaGrid(items = state.items, onOpenItem = onOpenItem)
        }
    }
}

@Composable
private fun Header(state: BrowseUiState, onOpenHome: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "VaultView",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${state.providerName} / ${state.breadcrumbs.joinToString(" / ")}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Button(
            onClick = onOpenHome,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text("Home")
        }
    }
}

@Composable
private fun MediaGrid(items: List<MediaItem>, onOpenItem: (MediaItem) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 196.dp),
        contentPadding = PaddingValues(bottom = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        items(items, key = { it.id }) { item ->
            MediaCard(item = item, onClick = { onOpenItem(item) })
        }
    }
}

@Composable
private fun MediaCard(item: MediaItem, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val border = if (isFocused) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    }

    Column(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(shape)
                .border(border, shape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when (item.type) {
                MediaType.Folder -> FolderTile(item.name)
                MediaType.Image,
                MediaType.Video -> AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (item.type == MediaType.Video) {
                Text(
                    text = "VIDEO",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.name,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FolderTile(name: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF26364D), Color(0xFF101722))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ImageViewer(
    item: MediaItem,
    imageUrl: String,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        color = Color.Black,
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onPrevious()
                        true
                    }

                    Key.DirectionRight -> {
                        onNext()
                        true
                    }

                    Key.Back, Key.Escape -> {
                        onClose()
                        true
                    }

                    else -> false
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            Text(
                text = item.name,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun VideoPlayer(item: MediaItem, streamUrl: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Back && event.type == KeyEventType.KeyUp) {
                    onClose()
                    true
                } else {
                    false
                }
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Text(
            text = item.name,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(36.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp)
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "No media in this folder", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp)
    }
}
