package com.trackit.app.ui.reminders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.trackit.app.R
import com.trackit.app.data.ServiceLocator
import com.trackit.app.data.remote.dto.ReminderDto
import com.trackit.app.databinding.DialogAddReminderBinding
import com.trackit.app.databinding.FragmentRemindersBinding
import com.trackit.app.ui.common.SimpleViewModelFactory

class RemindersFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: RemindersViewModel
    private lateinit var adapter: RemindersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        viewModel = ViewModelProvider(
            this,
            SimpleViewModelFactory { RemindersViewModel(ServiceLocator.reminderRepository(context)) }
        )[RemindersViewModel::class.java]

        adapter = RemindersAdapter(
            onToggleActive = { reminder, active -> viewModel.toggleActive(reminder.id, active) },
            onDelete = { confirmDelete(it) }
        )

        binding.recyclerReminders.layoutManager = LinearLayoutManager(context)
        binding.recyclerReminders.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        binding.fabAdd.setOnClickListener { showAddDialog() }

        viewModel.reminders.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.textEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }

        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.consumeError()
            }
        }

        viewModel.load()
    }

    private fun showAddDialog() {
        val dialogBinding = DialogAddReminderBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.reminders_add))
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val message = dialogBinding.editMessage.text.toString().trim()
                val hour = dialogBinding.editHour.text.toString().trim()
                val recurring = dialogBinding.checkRecurring.isChecked

                if (message.isEmpty() || !hour.matches(Regex("^\\d{2}:\\d{2}$"))) {
                    Snackbar.make(binding.root, "Usa el formato HH:mm, ej. 07:30", Snackbar.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                viewModel.create(message, hour, recurring)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmDelete(item: ReminderDto) {
        AlertDialog.Builder(requireContext())
            .setTitle(item.message)
            .setMessage("¿Eliminar este recordatorio?")
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete(item.id) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
