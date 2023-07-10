package dev.anonymous.eilaji.adapters.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.R

class SearchAdapter<T>(private var anyList: ArrayList<T>) :
    RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search, parent, false)
        return SearchViewHolder(view)
    }

    override fun getItemCount(): Int {
        return anyList.size
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val any:T = anyList[position]
        holder.bind(any)
    }

    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val searchItemText:TextView = itemView.findViewById(R.id.search_item_text_view)
        fun <T> bind(any: T) {
            searchItemText.text = any.toString()
        }
    }
}