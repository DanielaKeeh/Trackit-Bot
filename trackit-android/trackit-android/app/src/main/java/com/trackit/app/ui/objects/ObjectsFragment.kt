package com.trackit.app.ui.objects

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
import com.trackit.app.data.remote.dto.TrackedObjectDto
import com.trackit.app.databinding.DialogAddObjectBinding
import com.trackit.app.databinding.FragmentObjectsBinding
import com.trackit.app.ui.common.SimpleViewModelFactory

class ObjectsFragment : Fragment() {

    private var _binding: FragmentObjectsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ObjectsViewModel
    private lateinit var adapter: ObjectsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentObjectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        viewModel = ViewModelProvider(
            this,
            SimpleViewModelFactory {
                ObjectsViewModel(ServiceLocator.objectRepository(context), ServiceLocator.predictRepository(context))
            }
        )[ObjectsViewModel::class.java]

        adapter = ObjectsAdapter(
            onPredict = { viewModel.predict(it.name) },
            onEdit = { showEditDialog(it) },
            onDelete = { confirmDelete(it) }
        )

        binding.recyclerObjects.layoutManager = LinearLayoutManager(context)
        binding.recyclerObjects.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        binding.fabAdd.setOnClickListener { showAddDialog() }

        viewModel.objects.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.textEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }

        viewModel.events.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ObjectsUiEvent.Error -> {
                    Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                    viewModel.consumeEvent()
                }
                is ObjectsUiEvent.PredictionReady -> {
                    showPredictionResult(event)
                    viewModel.consumeEvent()
                }
                null -> Unit
            }
        }

        viewModel.load()
    }

    private fun showAddDialog() {
        val dialogBinding = DialogAddObjectBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.objects_add))
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = dialogBinding.editObjectName.text.toString().trim()
                val place = dialogBinding.editObjectPlace.text.toString().trim()
                if (name.isNotEmpty() && place.isNotEmpty()) {
                    viewModel.create(name, place)
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showEditDialog(item: TrackedObjectDto) {
        val dialogBinding = DialogAddObjectBinding.inflate(layoutInflater)
        dialogBinding.editObjectName.setText(item.name)
        dialogBinding.editObjectName.isEnabled = false
        dialogBinding.editObjectPlace.setText(item.place)

        AlertDialog.Builder(requireContext())
            .setTitle(item.name)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val newPlace = dialogBinding.editObjectPlace.text.toString().trim()
                if (newPlace.isNotEmpty()) {
                    viewModel.update(item.name, newPlace)
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmDelete(item: TrackedObjectDto) {
        AlertDialog.Builder(requireContext())
            .setTitle(item.name)
            .setMessage("¿Eliminar este objeto?")
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete(item.name) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showPredictionResult(event: ObjectsUiEvent.PredictionReady) {
        val response = event.response
        val message = if (response.place != null) {
            "${response.message} (confianza ${response.confidence}%)"
        } else {
            response.message
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Predicción: ${event.objectName}")
            .setMessage(message)
            .setPositiveButton("Entendido", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
