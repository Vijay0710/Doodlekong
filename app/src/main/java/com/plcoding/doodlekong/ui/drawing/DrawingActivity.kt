package com.plcoding.doodlekong.ui.drawing


import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.plcoding.doodlekong.R
import com.plcoding.doodlekong.databinding.ActivityDrawingBinding
import com.plcoding.doodlekong.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.plcoding.doodlekong.adapters.ChatMessageAdapter
import com.plcoding.doodlekong.data.remote.ws.models.DrawAction
import com.plcoding.doodlekong.data.remote.ws.models.GameError
import com.plcoding.doodlekong.data.remote.ws.models.JoinRoomHandShake
import com.tinder.scarlet.WebSocket
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DrawingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding
    private val viewModel: DrawingViewModel by viewModels()

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var rvPlayers: RecyclerView

    @Inject
    lateinit var clientId: String

    private lateinit var chatMessageAdapter: ChatMessageAdapter

    private val args: DrawingActivityArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        subscribeToUiStateUpdates()

        listenToConnectionEvents()
        listenToSocketEvents()

        setUpRecyclerView()

        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)

        binding.drawingView.roomName = args.roomName

        val header = layoutInflater.inflate(R.layout.nav_drawer_header, binding.navView)
        rvPlayers = header.findViewById(R.id.rvPlayers)

        handleDrawer()

        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.checkRadioButton(
                DrawingViewModel.ColorResourceId(checkedId)
            )
        }

        binding.drawingView.setOnDrawListener {
            if (binding.drawingView.isUserDrawing) {
                viewModel.sendBaseModel(it)
            }
        }

        binding.ibUndo.setOnClickListener {
            if (binding.drawingView.isUserDrawing) {
                binding.drawingView.undo()
                viewModel.sendBaseModel(
                    DrawAction(
                        action = DrawAction.ACTION_UNDO
                    )
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    private fun handleDrawer() {

        binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        binding.ibPlayers.setOnClickListener {
            binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            binding.root.openDrawer(GravityCompat.START)
        }

        binding.root.addDrawerListener(
            object : DrawerLayout.DrawerListener {
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

                override fun onDrawerOpened(drawerView: View) {}

                override fun onDrawerClosed(drawerView: View) {
                    binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }

                override fun onDrawerStateChanged(newState: Int) {}
            }
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setColor(color: Int) {
        binding.drawingView.setColor(color)
        binding.drawingView.setThickness(Constants.DEFAULT_PAINT_THICKNESS)

    }

    private fun listenToConnectionEvents() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.connectionEvent.collect { event ->
                when (event) {
                    is WebSocket.Event.OnConnectionClosed -> {
                        viewModel.setConnectionProgressBarVisibility(false)
                    }

                    is WebSocket.Event.OnConnectionFailed -> {
                        viewModel.setConnectionProgressBarVisibility(false)
                        Snackbar.make(
                            binding.root,
                            R.string.error_connection_failed,
                            Snackbar.LENGTH_LONG
                        )
                        event.throwable.printStackTrace()
                    }

                    is WebSocket.Event.OnConnectionOpened<*> -> {
                        Timber.tag("VIJ").d("Connection is Opened sending Base Model")
                        viewModel.sendBaseModel(
                            JoinRoomHandShake(
                                username = args.username,
                                roomName = args.roomName,
                                clientId = clientId
                            )
                        )

                        viewModel.setConnectionProgressBarVisibility(false)
                    }

                    else -> Unit
                }

            }
        }
    }

    private fun listenToSocketEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.socketEvent.collect { event ->
                    when (event) {
                        is DrawingViewModel.SocketEvent.AnnouncementEvent -> TODO()
                        is DrawingViewModel.SocketEvent.CheckMessageEvent -> TODO()
                        is DrawingViewModel.SocketEvent.ChosenWordEvent -> TODO()
                        is DrawingViewModel.SocketEvent.DrawDataEvent -> {
                            val drawData = event.data
                            if (!binding.drawingView.isUserDrawing) {
                                when (drawData.motionEvent) {
                                    MotionEvent.ACTION_DOWN -> {
                                        binding.drawingView.startedTouchExternally(drawData)
                                    }

                                    MotionEvent.ACTION_MOVE -> {
                                        binding.drawingView.movedTouchExternally(drawData)
                                    }

                                    MotionEvent.ACTION_UP -> {
                                        binding.drawingView.releaseTouchExternally(drawData)
                                    }
                                }
                            }
                        }

                        is DrawingViewModel.SocketEvent.GameErrorEvent -> {
                            when (event.data.errorType) {
                                GameError.ERROR_ROOM_NOT_FOUND -> {
                                    finish()
                                }
                            }
                        }

                        is DrawingViewModel.SocketEvent.GameStateEvent -> TODO()
                        is DrawingViewModel.SocketEvent.NewWordsEvent -> TODO()
                        is DrawingViewModel.SocketEvent.RoundDrawInfoEvent -> TODO()
                        DrawingViewModel.SocketEvent.UndoEvent -> {
                            binding.drawingView.undo()
                        }
                    }
                }
            }
        }
    }

    private fun subscribeToUiStateUpdates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedColorButtonId.collect {
                    binding.colorGroup.check(it.id)

                    when (it.id) {
                        R.id.rbRed -> setColor(Color.RED)
                        R.id.rbGreen -> setColor(Color.GREEN)
                        R.id.rbBlue -> setColor(Color.BLUE)
                        R.id.rbYellow -> setColor(Color.YELLOW)
                        R.id.rbOrange -> {
                            setColor(ContextCompat.getColor(this@DrawingActivity, R.color.orange))
                        }

                        R.id.rbBlack -> setColor(Color.BLACK)
                        R.id.rbEraser -> {
                            binding.drawingView.setColor(Color.WHITE)
                            binding.drawingView.setThickness(40f)
                        }
                    }
                }
            }
        }


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connectionProgressBarVisible.collect { isVisible ->
                    binding.connectionProgressBar.isVisible = isVisible
                }
            }
        }


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.chooseWordOverlayVisible.collect { isVisible ->
                    binding.chooseWordOverlay.isVisible = isVisible
                }
            }
        }
    }

    private fun setUpRecyclerView() {
        binding.rvChat.apply {
            chatMessageAdapter = ChatMessageAdapter(args.username)
            adapter = chatMessageAdapter
            layoutManager = LinearLayoutManager(this@DrawingActivity)
        }
    }
}