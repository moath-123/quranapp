package com.codesteem.quranapp.Adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codesteem.quranapp.R
import com.codesteem.quranapp.UIModels.ChapterUiModel
import com.codesteem.quranapp.helper.QuranMetaDataHelper

class ChapterAdapter(
    private val onClick: (ChapterUiModel) -> Unit
) : ListAdapter<ChapterUiModel, ChapterAdapter.VH>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_juz, parent, false)
        return VH(view, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(itemView: View, val onClick: (ChapterUiModel) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val txtChapterEnName: TextView = itemView.findViewById(R.id.txtChapterEnName)
        private val txtChapterArName: TextView = itemView.findViewById(R.id.txtChapterArName)
        private val txtChapterPageNo: TextView = itemView.findViewById(R.id.txtChapterPageNo)
        private val txtJuzCount: TextView = itemView.findViewById(R.id.txtJuzCount)

        fun bind(item: ChapterUiModel) {
            val chapterName = QuranMetaDataHelper.juzNameFor(item.chapterId)

            //val nameToShow = if (Locale.getDefault().language == "ar") juz.ar else juz.en

            txtChapterEnName.text = chapterName.en + " \u2022 "
            txtChapterArName.text =  chapterName.ar
            txtChapterPageNo.text = item.startPage.toString()
            txtJuzCount.text = item.chapterId.toString()

            itemView.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ChapterUiModel>() {
            override fun areItemsTheSame(oldItem: ChapterUiModel, newItem: ChapterUiModel) =
                oldItem.chapterId == newItem.chapterId

            override fun areContentsTheSame(oldItem: ChapterUiModel, newItem: ChapterUiModel) =
                oldItem == newItem
        }
    }
}
