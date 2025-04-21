package com.plcoding.doodlekong.ui.setup.fragments

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.plcoding.doodlekong.R
import com.plcoding.doodlekong.data.remote.ws.Room
import com.plcoding.doodlekong.databinding.FragmentCreateRoomBinding
import com.plcoding.doodlekong.ui.setup.CreateRoomViewModel
import com.plcoding.doodlekong.utils.Constants.MAX_ROOM_NAME_LENGTH
import com.plcoding.doodlekong.utils.Constants.MIN_ROOM_NAME_LENGTH
import com.plcoding.doodlekong.utils.hideKeyboard
import com.plcoding.doodlekong.utils.navigateSafely
import com.plcoding.doodlekong.utils.snackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateRoomFragment : Fragment(R.layout.fragment_create_room) {
    private var _binding: FragmentCreateRoomBinding? = null
    private val binding: FragmentCreateRoomBinding
        get() = _binding!!

    private val viewModel: CreateRoomViewModel by viewModels()
    private val args: CreateRoomFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateRoomBinding.bind(view)
        requireActivity().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setUpRoomSizeSpinner()
        listenToEvents()

        binding.btnCreateRoom.setOnClickListener {
            binding.createRoomProgressBar.isVisible = true
            viewModel.createRoom(
                Room(
                    binding.etRoomName.text.toString(),
                    binding.tvMaxPersons.text.toString().toInt()
                )
            )
            requireActivity().hideKeyboard(binding.root)
        }

    }

    private fun setUpRoomSizeSpinner() {
        val roomSizes = resources.getStringArray(R.array.room_size_array)
        val adapter = ArrayAdapter(requireContext(), R.layout.textview_room_size, roomSizes)
        binding.tvMaxPersons.setAdapter(adapter)
    }

    private fun listenToEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.setUpEvent.collect { event ->
                    when (event) {


                        is CreateRoomViewModel.SetUpEvent.CreateRoomEvent -> {
                            viewModel.joinRoom(
                                args.username,
                                event.room.name
                            )
                        }

                        is CreateRoomViewModel.SetUpEvent.InputEmptyError -> {
                            binding.createRoomProgressBar.isVisible = false
                            snackBar(R.string.error_field_empty)
                        }

                        is CreateRoomViewModel.SetUpEvent.InputTooShortError -> {
                            binding.createRoomProgressBar.isVisible = false
                            snackBar(
                                getString(
                                    R.string.error_room_name_too_short,
                                    MIN_ROOM_NAME_LENGTH
                                )
                            )
                        }

                        is CreateRoomViewModel.SetUpEvent.InputTooLongError -> {
                            binding.createRoomProgressBar.isVisible = false
                            snackBar(
                                getString(
                                    R.string.error_room_name_too_long,
                                    MAX_ROOM_NAME_LENGTH
                                )
                            )
                        }

                        is CreateRoomViewModel.SetUpEvent.CreateRoomErrorEvent -> {
                            binding.createRoomProgressBar.isVisible = false
                            snackBar(event.error)
                        }

                        is CreateRoomViewModel.SetUpEvent.JoinRoomEvent -> {
                            binding.createRoomProgressBar.isVisible = false
                            findNavController().navigateSafely(
                                resId = R.id.action_createRoomFragment_to_drawingActivity,
                                args = Bundle().apply {
                                    putString("username", args.username)
                                    putString("roomName", event.roomName)
                                }
                            )
                        }

                        is CreateRoomViewModel.SetUpEvent.JoinRoomErrorEvent -> {
                            binding.createRoomProgressBar.isVisible = false
                            snackBar(event.error)
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