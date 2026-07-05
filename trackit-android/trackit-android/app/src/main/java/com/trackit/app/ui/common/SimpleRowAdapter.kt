package com.trackit.app.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.trackit.app.databinding.ItemDashboardRowBinding

data class SimpleRow(val title: String, val subtitle: String)

/**
 * Adapter genérico de dos líneas (título/subtítulo), usado para las listas
 * cortas del Dashboard (recordatorios próximos, objetos recientes) sin
 * necesitar un adapter dedicado para cada una.
 */
class SimpleRowAdapter(private var items: List<SimpleRow> = emptyList()) :
    RecyclerView.Adapter<SimpleRowAdapter.RowViewHolder>() {

    fun submitList(newItems: List<SimpleRow>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val binding = ItemDashboardRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textTitle.text = item.title
        holder.binding.textSubtitle.text = item.subtitle
    }

    override fun getItemCount(): Int = items.size

    class RowViewHolder(val binding: ItemDashboardRowBinding) : RecyclerView.ViewHolder(binding.root)
}
