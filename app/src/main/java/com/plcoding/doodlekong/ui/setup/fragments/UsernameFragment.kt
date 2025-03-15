package com.plcoding.doodlekong.ui.setup.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.plcoding.doodlekong.R
import com.plcoding.doodlekong.databinding.FragmentUsernameBinding
import com.plcoding.doodlekong.ui.setup.UsernameViewModel
import com.plcoding.doodlekong.utils.Constants.MAX_USERNAME_LENGTH
import com.plcoding.doodlekong.utils.Constants.MIN_USERNAME_LENGTH
import com.plcoding.doodlekong.utils.navigateSafely
import com.plcoding.doodlekong.utils.snackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UsernameFragment : Fragment() {

    private var _binding: FragmentUsernameBinding? = null
    private val binding: FragmentUsernameBinding
        get() = _binding!!

    private val viewModel: UsernameViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentUsernameBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        listenToEvents()
        binding.btnNext.setOnClickListener {
            viewModel.validateUsernameAndNavigateToSelectRoom(
                binding.etUsername.text.toString()
            )
        }
    }

    private fun listenToEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.setUpEvent.collect { event ->
                    when (event) {
                        UsernameViewModel.SetUpEvent.InputEmptyError -> {
                            snackBar(getString(R.string.error_field_empty))
                        }

                        UsernameViewModel.SetUpEvent.InputTooLongError -> {
                            snackBar(getString(R.string.error_username_too_long, MAX_USERNAME_LENGTH))
                        }

                        UsernameViewModel.SetUpEvent.InputTooShortError -> {
                            snackBar(getString(R.string.error_username_too_short, MIN_USERNAME_LENGTH))
                        }

                        is UsernameViewModel.SetUpEvent.NavigateToSelectRoomEvent -> {
                            findNavController().navigateSafely(
                                resId = R.id.action_usernameFragment_to_selectRoomFragment,
                                args = Bundle().apply {
                                    putString("username", event.username)
                                }
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}