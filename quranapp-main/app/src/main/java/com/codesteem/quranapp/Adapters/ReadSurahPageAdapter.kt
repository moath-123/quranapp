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
import com.codesteem.quranapp.UIModels.SurahPageUiModel

class ReadSurahPageAdapter : ListAdapter<SurahPageUiModel, ReadSurahPageAdapter.VH>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_read_surah_page, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtReadSuraPageNo: TextView = itemView.findViewById(R.id.txtReadSuraPageNo)
        private val txtPageText: TextView = itemView.findViewById(R.id.txtPageText)

        private val txtReadSuraName: TextView = itemView.findViewById(R.id.txtReadSuraName)
        private val txtReadSuraJuzName: TextView = itemView.findViewById(R.id.txtReadSuraJuzName)

        fun bind(item: SurahPageUiModel) {
            txtReadSuraPageNo.text = item.pageNumber.toString()
            txtPageText.text = item.content

            txtReadSuraName.text = "﴿ "+ item.surahNameAr + " ﴾"
            txtReadSuraJuzName.text ="﴿ "+ item.juzNameAr + " ﴾"


            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                txtPageText.justificationMode =
                    LineBreaker.JUSTIFICATION_MODE_INTER_WORD
            }

        }

    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<SurahPageUiModel>() {
            override fun areItemsTheSame(oldItem: SurahPageUiModel, newItem: SurahPageUiModel) =
                oldItem.pageNumber == newItem.pageNumber

            override fun areContentsTheSame(oldItem: SurahPageUiModel, newItem: SurahPageUiModel) =
                TextUtils.equals(oldItem.content, newItem.content) &&
                        oldItem.surahNameAr == newItem.surahNameAr &&
                        oldItem.surahNameEn == newItem.surahNameEn &&
                        oldItem.juzNameAr == newItem.juzNameAr &&
                        oldItem.juzNameEn == newItem.juzNameEn
        }
    }
}
