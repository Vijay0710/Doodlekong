package com.plcoding.doodlekong.ui.setup.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavArgs
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.plcoding.doodlekong.R
import com.plcoding.doodlekong.adapters.RoomAdapter
import com.plcoding.doodlekong.databinding.FragmentSelectRoomBinding
import com.plcoding.doodlekong.ui.setup.SelectRoomViewModel
import com.plcoding.doodlekong.utils.Constants.SEARCH_DELAY
import com.plcoding.doodlekong.utils.navigateSafely
import com.plcoding.doodlekong.utils.snackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SelectRoomFragment : Fragment() {
    private var _binding: FragmentSelectRoomBinding? = null
    private val binding: FragmentSelectRoomBinding
        get() = _binding!!

    private val viewModel: SelectRoomViewModel by viewModels()

    private val args: SelectRoomFragmentArgs by navArgs()

    @Inject
    lateinit var roomAdapter: RoomAdapter

    private var updateRoomsJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSelectRoomBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setUpRecyclerView()
        subscribeToObservers()
        listenToEvents()


        viewModel.getRooms("")

        var searchJobs: Job? = null
        binding.etRoomName.addTextChangedListener {
            searchJobs?.cancel()

            searchJobs = lifecycleScope.launch {
                delay(SEARCH_DELAY)
                viewModel.getRooms(it.toString())
            }
        }

        binding.ibReload.setOnClickListener {
            binding.roomsProgressBar.isVisible = true
            binding.ivNoRoomsFound.isVisible = false
            binding.tvNoRoomsFound.isVisible = false
            viewModel.getRooms(binding.etRoomName.text.toString())
        }

        binding.btnCreateRoom.setOnClickListener {
            findNavController().navigateSafely(
                resId = R.id.action_selectRoomFragment_to_createRoomFragment,
                args = Bundle().apply {
                    putString("username", args.username)
                }
            )
        }

        roomAdapter.setOnClickListener {
            viewModel.joinRoom(args.username, it.name)
        }
    }

    private fun listenToEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.setUpEvent.collect {
                    when(it) {
                        is SelectRoomViewModel.SetUpEvent.JoinRoomEvent -> {
                            findNavController().navigateSafely(
                                R.id.action_selectRoomFragment_to_drawingActivity,
                                args = Bundle().apply {
                                    putString("username", args.username)
                                    putString("roomName", it.roomName)
                                }
                            )
                        }

                        is SelectRoomViewModel.SetUpEvent.JoinRoomErrorEvent -> {
                            snackBar(it.error)
                        }

                        is SelectRoomViewModel.SetUpEvent.GetRoomErrorEvent -> {
                            binding.apply {
                                roomsProgressBar.isVisible = false
                                tvNoRoomsFound.isVisible = false
                                ivNoRoomsFound.isVisible = false
                            }

                            snackBar(it.error)
                        }

                        else -> Unit
                    }
                }
            }
        }
    }


    private fun subscribeToObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rooms.collect { event ->
                    when(event) {
                        is SelectRoomViewModel.SetUpEvent.GetRoomLoadingEvent -> {
                            binding.roomsProgressBar.isVisible = true
                        }

                        is SelectRoomViewModel.SetUpEvent.GetRoomEvent -> {
                            binding.roomsProgressBar.isVisible = false
                            val isRoomsEmpty = event.rooms.isEmpty()
                            binding.tvNoRoomsFound.isVisible = isRoomsEmpty
                            binding.ivNoRoomsFound.isVisible = isRoomsEmpty

                            updateRoomsJob?.cancel()
                            updateRoomsJob = lifecycleScope.launch {
                                roomAdapter.updateDataSet(event.rooms)
                            }
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

    private fun setUpRecyclerView() {
        binding.rvRooms.apply {
            adapter = roomAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
}