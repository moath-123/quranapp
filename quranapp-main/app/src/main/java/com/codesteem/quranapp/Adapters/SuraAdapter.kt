package com.codesteem.quranapp.Adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codesteem.quranapp.R
import com.codesteem.quranapp.UIModels.SuraUiModel
import com.codesteem.quranapp.helper.QuranMetaDataHelper

class SuraAdapter(
    private val onClick: (SuraUiModel) -> Unit
) : ListAdapter<SuraUiModel, SuraAdapter.SuraVH>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuraVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_surah, parent, false)
        return SuraVH(view, onClick)
    }

    override fun onBindViewHolder(holder: SuraVH, position: Int) {
        holder.bind(getItem(position))
    }

    class SuraVH(itemView: View, private val onClick: (SuraUiModel) -> Unit) : RecyclerView.ViewHolder(itemView) {

        private val txtSurahNumber: TextView = itemView.findViewById(R.id.txtSurahNumber)
        private val txtSurahArabicName: TextView = itemView.findViewById(R.id.txtSurahArabicName)
        private val txtSurahEngName: TextView = itemView.findViewById(R.id.txtSurahEngName)
        private val txtSurahAyaCountNazol: TextView = itemView.findViewById(R.id.txtSurahAyaCountNazol)

        fun bind(item: SuraUiModel) {
            txtSurahNumber.text = item.suraNumber.toString()
            txtSurahArabicName.text = item.nameAr
            txtSurahEngName.text = item.nameEn

            val nazoolType = QuranMetaDataHelper.revelationTypeFor(item.suraNumber)

            txtSurahAyaCountNazol.text = "$nazoolType . ${item.ayahCount} Ayahs"

            itemView.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<SuraUiModel>() {
            override fun areItemsTheSame(oldItem: SuraUiModel, newItem: SuraUiModel): Boolean =
                oldItem.suraNumber == newItem.suraNumber

            override fun areContentsTheSame(oldItem: SuraUiModel, newItem: SuraUiModel): Boolean =
                oldItem == newItem
        }
    }
}
