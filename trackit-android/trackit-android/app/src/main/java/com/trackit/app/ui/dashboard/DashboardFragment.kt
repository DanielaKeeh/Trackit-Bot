package com.trackit.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.trackit.app.data.ServiceLocator
import com.trackit.app.databinding.FragmentDashboardBinding
import com.trackit.app.ui.common.SimpleRow
import com.trackit.app.ui.common.SimpleRowAdapter
import com.trackit.app.ui.common.SimpleViewModelFactory

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel
    private val remindersAdapter = SimpleRowAdapter()
    private val objectsAdapter = SimpleRowAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        viewModel = ViewModelProvider(
            this,
            SimpleViewModelFactory {
                DashboardViewModel(ServiceLocator.objectRepository(context), ServiceLocator.reminderRepository(context))
            }
        )[DashboardViewModel::class.java]

        binding.recyclerReminders.layoutManager = LinearLayoutManager(context)
        binding.recyclerReminders.adapter = remindersAdapter

        binding.recyclerObjects.layoutManager = LinearLayoutManager(context)
        binding.recyclerObjects.adapter = objectsAdapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }

        binding.textLogout.setOnClickListener {
            ServiceLocator.userRepository(context).logout()
            val intent = android.content.Intent(context, com.trackit.app.ui.auth.LoginActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.textObjectsCount.text = state.objectsCount.toString()
            binding.textRemindersCount.text = state.remindersCount.toString()

            remindersAdapter.submitList(
                state.upcomingReminders.map { SimpleRow(it.message, "${it.hour} · ${if (it.recurring) "Todos los días" else "Una vez"}") }
            )
            objectsAdapter.submitList(
                state.recentObjects.map { SimpleRow(it.name, "Lugar: ${it.place}") }
            )

            binding.textEmpty.visibility =
                if (state.objectsCount == 0 && state.remindersCount == 0) View.VISIBLE else View.GONE
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

    override fun onResume() {
        super.onResume()
        // Refresca cada vez que el usuario vuelve al Dashboard (ej. tras registrar un objeto).
        if (::viewModel.isInitialized) viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
