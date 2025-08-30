package com.example.reviewapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reviewapp.R


@Composable
fun PermissionBanner(
    showRationale: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = if (showRationale)
                    stringResource(R.string.perm_rationale)
                else
                    stringResource(R.string.perm_denied_permanent)
            )
            Spacer(Modifier.height(8.dp))
            Row {
                if (showRationale) {
                    Button(onClick = onRequest) { Text(stringResource(R.string.action_allow_location)) }
                } else {
                    OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.action_open_settings)) }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = onRequest) { Text(stringResource(R.string.action_try_again)) }
                }
            }
        }
    }
}