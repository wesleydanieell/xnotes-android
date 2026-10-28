package com.xnotes.ui

import androidx.compose.runtime.compositionLocalOf

/** Optional per-bar swatch count; null retains the shared ink palette count. */
internal val LocalToolbarColorCount = compositionLocalOf<Int?> { null }
