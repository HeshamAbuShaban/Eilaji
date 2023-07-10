package dev.anonymous.eilaji.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import dev.anonymous.eilaji.adapters.MessagesAdapter

class MyScrollToBottomObserver(
    private val recycler: RecyclerView,
    private val adapter: MessagesAdapter,
) : AdapterDataObserver() {
    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        super.onItemRangeInserted(positionStart, itemCount)
        val count = adapter.itemCount
        val lastVisiblePosition =
            (recycler.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
        // If the recycler view is initially being loaded or the user is at the bottom of the list,
        // scroll to the bottom of the list to show the newly added message.
        val loading = lastVisiblePosition == -1
        val atBottom = positionStart >= count - 1 && lastVisiblePosition == positionStart - 1
        if (loading || atBottom) {
            recycler.scrollToPosition(positionStart)
        }
    }
}