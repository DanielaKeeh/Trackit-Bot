package com.trackit.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.trackit.app.data.ServiceLocator
import com.trackit.app.databinding.FragmentChatBinding
import com.trackit.app.ui.common.SimpleViewModelFactory

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatViewModel
    private val adapter = ChatAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        viewModel = ViewModelProvider(
            this,
            SimpleViewModelFactory {
                ChatViewModel(
                    ServiceLocator.objectRepository(context),
                    ServiceLocator.reminderRepository(context),
                    ServiceLocator.predictRepository(context)
                )
            }
        )[ChatViewModel::class.java]

        val layoutManager = LinearLayoutManager(context)
        binding.recyclerMessages.layoutManager = layoutManager
        binding.recyclerMessages.adapter = adapter

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            binding.recyclerMessages.scrollToPosition(messages.size - 1)
        }

        viewModel.typing.observe(viewLifecycleOwner) { isTyping ->
            binding.textTyping.visibility = if (isTyping) View.VISIBLE else View.GONE
        }

        binding.buttonSend.setOnClickListener { sendCurrentText() }
        binding.editMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentText()
                true
            } else {
                false
            }
        }
    }

    private fun sendCurrentText() {
        val text = binding.editMessage.text.toString()
        if (text.isBlank()) return
        viewModel.sendMessage(text)
        binding.editMessage.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
