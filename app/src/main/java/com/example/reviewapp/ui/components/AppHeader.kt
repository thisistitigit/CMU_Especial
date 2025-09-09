package com.example.reviewapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.reviewapp.R
import com.example.reviewapp.ui.theme.BrandBlack

/**
 * Cabeçalho da aplicação com **logo** e **título** centralizados.
 *
 * Características:
 * - Suporte opcional para ação de navegação "voltar".
 * - Integração com o esquema de cores do tema (claro/escuro).
 * - Truncagem do título com `Ellipsis` para prevenir _overflows_.
 *
 * Acessibilidade:
 * - O botão de voltar não tem `contentDescription` explícito porque o ícone é
 *   semântico; se necessário, ajustar para leitores de ecrã.
 *
 * @param title texto do cabeçalho.
 * @param onBack callback opcional para ação de voltar; se `null`, oculta o botão.
 * @param actions *slot* para ações à direita (ex.: ícones de pesquisa).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    val dark = isSystemInDarkTheme()

    val containerColor = if (dark) BrandBlack else MaterialTheme.colorScheme.background
    val contentColor   = if (dark) MaterialTheme.colorScheme.onBackground.copy(alpha = 1f) else MaterialTheme.colorScheme.onBackground

    CenterAlignedTopAppBar(
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null, // decorativo
                    modifier = Modifier.size(62.dp)
                )
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor             = containerColor,
            titleContentColor          = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor     = contentColor
        )
    )
}
