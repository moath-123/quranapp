package com.codesteem.quranapp.Adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codesteem.quranapp.R
import com.codesteem.quranapp.UIModels.AyahUiModel
import com.codesteem.quranapp.helper.ArabicHighlighter

class AyahAdapter : ListAdapter<AyahUiModel, AyahAdapter.VH>(DiffCallback) {

    private var query: String? = null

    fun setQuery(q: String?) {
        query = q?.trim().takeUnless { it.isNullOrEmpty() }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_ayah, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), query)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val txtAyahArabic: TextView = itemView.findViewById(R.id.txtAyahArabic)

        fun bind(item: AyahUiModel, query: String?) {
            val color = ContextCompat.getColor(itemView.context, R.color.highlight_yellow) // create this color

            txtAyahArabic.text = if (query.isNullOrEmpty()) {
                item.textUthmani
            } else {
                ArabicHighlighter.highlightUthmani(item.textUthmani, query, color)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<AyahUiModel>() {
            override fun areItemsTheSame(oldItem: AyahUiModel, newItem: AyahUiModel) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: AyahUiModel, newItem: AyahUiModel) = oldItem == newItem
        }
    }
}
