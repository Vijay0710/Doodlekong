package com.plcoding.doodlekong.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.plcoding.doodlekong.data.remote.ws.Room
import com.plcoding.doodlekong.databinding.ItemRoomBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoomAdapter @Inject constructor() : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    var rooms = listOf<Room>()
        private set

    class RoomViewHolder(val binding: ItemRoomBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    suspend fun updateDataSet(newDataSet: List<Room>) {
        withContext(Dispatchers.Default) {
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return rooms.size
                }

                override fun getNewListSize(): Int {
                    return newDataSet.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return rooms[oldItemPosition] == newDataSet[newItemPosition]
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return rooms[oldItemPosition] == newDataSet[newItemPosition]
                }
            })

            withContext(Dispatchers.Main) {
                rooms = newDataSet
                diff.dispatchUpdatesTo(this@RoomAdapter)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        return RoomViewHolder(
            ItemRoomBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return rooms.size
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]

        holder.binding.apply {
            tvRoomName.text = room.name
            val playerCountText = "${room.playerCount}/ ${room.maximumPlayers}"
            tvRoomPersonCount.text = playerCountText

            root.setOnClickListener {
                onRoomClickListener?.let { click ->
                    click(room)
                }
            }
        }
    }


    private var onRoomClickListener: ((Room) -> Unit)? = null

    fun setOnClickListener(listener: ((Room) -> Unit)) {
        onRoomClickListener = listener
    }
}