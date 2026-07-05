package com.trackit.app.ui.reminders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.trackit.app.data.remote.dto.ReminderDto
import com.trackit.app.databinding.ItemReminderBinding

class RemindersAdapter(
    private val onToggleActive: (ReminderDto, Boolean) -> Unit,
    private val onDelete: (ReminderDto) -> Unit
) : ListAdapter<ReminderDto, RemindersAdapter.ReminderViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReminderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReminderViewHolder(private val binding: ItemReminderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReminderDto) {
            binding.textMessage.text = item.message
            val recurrenceLabel = if (item.recurring) "Todos los días" else "Una vez"
            binding.textHour.text = "${item.hour} · $recurrenceLabel"

            // Se limpia el listener antes de setChecked para no disparar onToggleActive al reciclar la vista.
            binding.switchActive.setOnCheckedChangeListener(null)
            binding.switchActive.isChecked = item.active
            binding.switchActive.setOnCheckedChangeListener { _, isChecked ->
                onToggleActive(item, isChecked)
            }

            binding.buttonDelete.setOnClickListener { onDelete(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ReminderDto>() {
            override fun areItemsTheSame(oldItem: ReminderDto, newItem: ReminderDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ReminderDto, newItem: ReminderDto) = oldItem == newItem
        }
    }
}
