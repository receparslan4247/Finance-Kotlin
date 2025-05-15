package com.receparslan.finance.util

import androidx.compose.foundation.lazy.LazyListState

// This function is used to check if the user has scrolled to the end of the list.
fun LazyListState.reachedEnd(): Boolean {
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()

    lastVisibleItem?.let { return it.index >= layoutInfo.totalItemsCount - 25 } ?: return false
}