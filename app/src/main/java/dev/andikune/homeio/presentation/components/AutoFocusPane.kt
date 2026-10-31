package dev.andikune.homeio.presentation.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay

@Composable
fun AutoFocusPane(
    paneKey: Any,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(paneKey) {
        delay(150) // Jeda dikit biar UI siap
        runCatching { focusRequester.requestFocus() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
    ) {
        content()
    }
}
