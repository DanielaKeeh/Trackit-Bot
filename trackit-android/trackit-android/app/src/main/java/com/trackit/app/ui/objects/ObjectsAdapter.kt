package com.trackit.app.ui.objects

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.trackit.app.data.remote.dto.TrackedObjectDto
import com.trackit.app.databinding.ItemObjectBinding

class ObjectsAdapter(
    private val onPredict: (TrackedObjectDto) -> Unit,
    private val onEdit: (TrackedObjectDto) -> Unit,
    private val onDelete: (TrackedObjectDto) -> Unit
) : ListAdapter<TrackedObjectDto, ObjectsAdapter.ObjectViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObjectViewHolder {
        val binding = ItemObjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ObjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ObjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ObjectViewHolder(private val binding: ItemObjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TrackedObjectDto) {
            binding.textName.text = item.name
            binding.textPlace.text = "Lugar: ${item.place}"
            binding.buttonPredict.setOnClickListener { onPredict(item) }
            binding.buttonEdit.setOnClickListener { onEdit(item) }
            binding.buttonDelete.setOnClickListener { onDelete(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TrackedObjectDto>() {
            override fun areItemsTheSame(oldItem: TrackedObjectDto, newItem: TrackedObjectDto) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TrackedObjectDto, newItem: TrackedObjectDto) =
                oldItem == newItem
        }
    }
}
