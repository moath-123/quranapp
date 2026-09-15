package com.codesteem.quranapp.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codesteem.quranapp.DAO.SearchRow
import com.codesteem.quranapp.R

class SearchAdapter(
    private val onClick: (SearchRow) -> Unit
) : RecyclerView.Adapter<SearchAdapter.VH>() {

    private val items = ArrayList<SearchRow>()

    fun submit(newItems: List<SearchRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_item_row_search, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    class VH(itemView: View, val onClick: (SearchRow) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvMeta = itemView.findViewById<TextView>(R.id.tvMeta)
        private val tvText = itemView.findViewById<TextView>(R.id.tvText)

        fun bind(row: SearchRow) {
            tvMeta.text = "${row.suraNameAr}  •  ${row.suraNumber}:${row.ayaNumber}  •  Page ${row.page}  •  Juz ${row.chapterId}"
            tvText.text = row.textUthmani

            itemView.setOnClickListener { onClick(row) }
        }
    }
}
