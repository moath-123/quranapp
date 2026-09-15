package com.codesteem.quranapp.sdktest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codesteem.quranapp.R

class SdkApiActionAdapter(
    private val onSelected: (SdkApiAction) -> Unit
) : RecyclerView.Adapter<SdkApiActionAdapter.VH>() {

    private val items = SdkApiCatalog.all
    private var selectedIndex = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sdk_api_action, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position == selectedIndex)
        holder.itemView.setOnClickListener {
            val old = selectedIndex
            selectedIndex = holder.bindingAdapterPosition
            notifyItemChanged(old)
            notifyItemChanged(selectedIndex)
            onSelected(items[selectedIndex])
        }
    }

    override fun getItemCount(): Int = items.size

    fun getSelected(): SdkApiAction = items[selectedIndex]

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val method: TextView = itemView.findViewById(R.id.txtApiMethod)
        private val desc: TextView = itemView.findViewById(R.id.txtApiDesc)
        private val innerLayout: LinearLayout = itemView.findViewById(R.id.layoutApiItem)

        fun bind(action: SdkApiAction, selected: Boolean) {
            method.text = action.methodName
            desc.text = action.description
            innerLayout.setBackgroundResource(
                if (selected) R.drawable.sdk_api_item_bg_selected
                else R.drawable.sdk_api_item_bg
            )
        }
    }
}
