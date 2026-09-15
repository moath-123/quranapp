package com.codesteem.quranapp.sdktest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.codesteem.quranapp.R

data class SdkTestRow(
    val apiMethod: String,
    val title: String,
    val subtitle: String,
    val body: String,
    val isRtlBody: Boolean = false,
    val bookmark: ClientBookmark? = null
)

class SdkTestResultAdapter(
    private val bookmarkStore: ClientBookmarkStore,
    private val onBookmarkChanged: () -> Unit
) : RecyclerView.Adapter<SdkTestResultAdapter.VH>() {

    private val items = ArrayList<SdkTestRow>()

    fun submit(rows: List<SdkTestRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sdk_test_result, parent, false)
        return VH(view, bookmarkStore, onBookmarkChanged)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(
        itemView: View,
        private val bookmarkStore: ClientBookmarkStore,
        private val onBookmarkChanged: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val badge: TextView = itemView.findViewById(R.id.txtItemBadge)
        private val title: TextView = itemView.findViewById(R.id.txtItemTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.txtItemSubtitle)
        private val body: TextView = itemView.findViewById(R.id.txtItemBody)
        private val btnBookmark: ImageView = itemView.findViewById(R.id.btnBookmark)

        fun bind(row: SdkTestRow) {
            badge.text = row.apiMethod
            title.text = row.title
            subtitle.text = row.subtitle
            body.text = row.body

            if (row.isRtlBody) {
                body.textDirection = View.TEXT_DIRECTION_RTL
                body.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            } else {
                body.textDirection = View.TEXT_DIRECTION_LTR
                body.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            }

            val bookmark = row.bookmark
            if (bookmark == null) {
                btnBookmark.visibility = View.GONE
            } else {
                btnBookmark.visibility = View.VISIBLE
                updateBookmarkIcon(bookmark.ayahId)
                btnBookmark.setOnClickListener {
                    bookmarkStore.toggle(bookmark)
                    updateBookmarkIcon(bookmark.ayahId)
                    onBookmarkChanged()
                }
            }
        }

        private fun updateBookmarkIcon(ayahId: Int) {
            val active = bookmarkStore.isBookmarked(ayahId)
            val color = if (active) R.color.sdk_client_bookmark_active
            else R.color.sdk_client_text_secondary
            btnBookmark.setColorFilter(
                ContextCompat.getColor(itemView.context, color)
            )
        }
    }
}
