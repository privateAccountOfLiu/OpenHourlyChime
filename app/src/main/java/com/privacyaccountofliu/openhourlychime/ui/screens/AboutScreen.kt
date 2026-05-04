package com.privacyaccountofliu.openhourlychime.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyaccountofliu.openhourlychime.R

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(128.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.msg3),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.msg2), fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Image(
                        painter = painterResource(id = R.drawable.hatsune_miku),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(stringResource(R.string.msg4), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.msg5), fontSize = 13.sp)
                Text(stringResource(R.string.msg6), fontSize = 13.sp)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    stringResource(R.string.msg7),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(stringResource(R.string.msg8), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.msg9), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.msg10), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}
