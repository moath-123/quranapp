package com.codesteem.quranapp.Adapters

import android.graphics.text.LineBreaker
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codesteem.quranapp.R
import com.codesteem.quranapp.UIModels.JuzPageUiModel

class ReadJuzPageAdapter : ListAdapter<JuzPageUiModel, ReadJuzPageAdapter.VH>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_read_surah_page, parent, false) // reuse same page item layout
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtSurahAr: TextView = itemView.findViewById(R.id.txtReadSuraName)
        private val txtJuzAr: TextView = itemView.findViewById(R.id.txtReadSuraJuzName)
        private val txtReadSuraPageNo: TextView = itemView.findViewById(R.id.txtReadSuraPageNo)
        private val txtPageText: TextView = itemView.findViewById(R.id.txtPageText)

        fun bind(item: JuzPageUiModel) {
            txtSurahAr.text = item.surahNameAr
            txtJuzAr.text = item.juzNameAr
            txtReadSuraPageNo.text = item.pageNumber.toString()
            txtPageText.text = item.content


            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                txtPageText.justificationMode =
                    LineBreaker.JUSTIFICATION_MODE_INTER_WORD
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<JuzPageUiModel>() {
            override fun areItemsTheSame(oldItem: JuzPageUiModel, newItem: JuzPageUiModel) =
                oldItem.pageNumber == newItem.pageNumber

            override fun areContentsTheSame(oldItem: JuzPageUiModel, newItem: JuzPageUiModel) =
                TextUtils.equals(oldItem.content, newItem.content) &&
                        oldItem.surahNameAr == newItem.surahNameAr &&
                        oldItem.surahNameEn == newItem.surahNameEn &&
                        oldItem.juzNameAr == newItem.juzNameAr &&
                        oldItem.juzNameEn == newItem.juzNameEn
        }
    }
}
