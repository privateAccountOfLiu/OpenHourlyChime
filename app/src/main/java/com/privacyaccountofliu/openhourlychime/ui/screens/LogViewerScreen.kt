package com.privacyaccountofliu.openhourlychime.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyaccountofliu.openhourlychime.R
import com.privacyaccountofliu.openhourlychime.model.tools.LogBuffer
import com.privacyaccountofliu.openhourlychime.model.tools.LogEntry
import com.privacyaccountofliu.openhourlychime.model.tools.LogLevel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogViewerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entries by LogBuffer.entries.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                val text = entries.joinToString("\n") { e ->
                    val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(e.timestamp))
                    "${e.level.name} $time [${e.tag}] ${e.message}"
                }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("logs", text))
                Toast.makeText(context, context.getString(R.string.log_copied), Toast.LENGTH_SHORT).show()
            }) {
                Text(stringResource(R.string.log_copy))
            }
            TextButton(onClick = {
                LogBuffer.clear()
                Toast.makeText(context, context.getString(R.string.log_cleared), Toast.LENGTH_SHORT).show()
            }) {
                Text(stringResource(R.string.log_clear))
            }
        }

        HorizontalDivider()

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    text = stringResource(R.string.log_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(entries.reversed()) { entry ->
                    LogEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    var expanded by remember { mutableStateOf(false) }

    val levelColor = when (entry.level) {
        LogLevel.ERROR -> Color(0xFFE53935)
        LogLevel.WARN -> Color(0xFFFB8C00)
        LogLevel.INFO -> Color(0xFF43A047)
        LogLevel.DEBUG -> Color(0xFF757575)
    }

    val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(entry.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (entry.throwable != null) expanded = !expanded }
                .padding(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Level badge
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = levelColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = entry.level.name,
                        color = levelColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = time,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = entry.tag,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = entry.message,
                fontSize = 12.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
            if (expanded && entry.throwable != null) {
                Text(
                    text = entry.throwable.stackTraceToString(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
