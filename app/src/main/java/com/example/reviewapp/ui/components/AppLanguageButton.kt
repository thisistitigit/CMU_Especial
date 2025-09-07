package com.example.reviewapp.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.example.reviewapp.R

@Composable
fun AppLanguageIconButton() {
    var open by remember { mutableStateOf(false) }
    val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    IconButton(onClick = { open = true }) {
        Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = stringResource(R.string.app_language_button)
        )
    }

    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.app_language_header)) },
            enabled = false,
            onClick = {}
        )
        LangRow("", stringResource(R.string.app_language_system), currentTags) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            open = false
        }
        Divider()
        LangRow("pt", stringResource(R.string.lang_pt), currentTags) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("pt"))
            open = false
        }
        LangRow("en", stringResource(R.string.lang_en), currentTags) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
            open = false
        }
        LangRow("es", stringResource(R.string.lang_es), currentTags) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
            open = false
        }
        LangRow("fr", stringResource(R.string.lang_fr), currentTags) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fr"))
            open = false
        }
    }
}

@Composable
private fun LangRow(tag: String, label: String, current: String, onClick: () -> Unit) {
    val selected = if (tag.isEmpty()) current.isEmpty() else current.startsWith(tag)
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { RadioButton(selected = selected, onClick = null) },
        onClick = onClick
    )
}
