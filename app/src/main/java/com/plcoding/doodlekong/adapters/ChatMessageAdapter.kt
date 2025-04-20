package com.plcoding.doodlekong.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.plcoding.doodlekong.data.remote.ws.Room
import com.plcoding.doodlekong.data.remote.ws.models.Announcement
import com.plcoding.doodlekong.data.remote.ws.models.BaseModel
import com.plcoding.doodlekong.data.remote.ws.models.ChatMessage
import com.plcoding.doodlekong.databinding.ItemAnnouncementBinding
import com.plcoding.doodlekong.databinding.ItemChatMessageIncomingBinding
import com.plcoding.doodlekong.databinding.ItemChatMessageOutgoingBinding
import com.plcoding.doodlekong.databinding.ItemRoomBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

private const val VIEW_TYPE_INCOMING_CHAT_MESSAGE = 0
private const val VIEW_TYPE_OUTGOING_CHAT_MESSAGE = 1
private const val VIEW_TYPE_ANNOUNCEMENT = 2

class ChatMessageAdapter(
    private val username: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class IncomingChatMessageViewHolder(val binding: ItemChatMessageIncomingBinding) :
        RecyclerView.ViewHolder(binding.root)

    class OutgoingChatMessageViewHolder(val binding: ItemChatMessageOutgoingBinding) :
        RecyclerView.ViewHolder(binding.root)

    class AnnouncementViewHolder(val binding: ItemAnnouncementBinding) :
        RecyclerView.ViewHolder(binding.root)

    var chatObjects = listOf<BaseModel>()

    suspend fun updateDataSet(newDataSet: List<BaseModel>) {
        withContext(Dispatchers.Default) {
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return chatObjects.size
                }

                override fun getNewListSize(): Int {
                    return newDataSet.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return chatObjects[oldItemPosition] == newDataSet[newItemPosition]
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return chatObjects[oldItemPosition] == newDataSet[newItemPosition]
                }
            })

            withContext(Dispatchers.Main) {
                chatObjects = newDataSet
                diff.dispatchUpdatesTo(this@ChatMessageAdapter)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(val obj = chatObjects[position]) {
            is Announcement -> VIEW_TYPE_ANNOUNCEMENT
            is ChatMessage -> {
                if(username == obj.from) {
                    VIEW_TYPE_OUTGOING_CHAT_MESSAGE
                } else {
                    VIEW_TYPE_INCOMING_CHAT_MESSAGE
                }
            }
            else -> {
                throw kotlin.IllegalStateException("Unknown view type")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_INCOMING_CHAT_MESSAGE -> {
                IncomingChatMessageViewHolder(
                    ItemChatMessageIncomingBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_OUTGOING_CHAT_MESSAGE -> {
                OutgoingChatMessageViewHolder(
                    ItemChatMessageOutgoingBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_ANNOUNCEMENT -> {
                AnnouncementViewHolder(
                    ItemAnnouncementBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> throw IllegalStateException("View Type doesn't match")
        }
    }

    override fun getItemCount(): Int {
        return chatObjects.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is AnnouncementViewHolder -> {
                val announcement = chatObjects[position] as Announcement
                holder.binding.apply {
                    tvAnnouncement.text = announcement.message
                    val dateFormat = SimpleDateFormat("kk:mm:ss dd.MM.yyyy", Locale.getDefault())
                    val date = dateFormat.format(announcement.timestamp)
                    tvTime.text = date

                    when(announcement.announcementType) {
                        Announcement.TYPE_EVERYBODY_GUESSED_IT -> {
                            root.setBackgroundColor(Color.LTGRAY)
                            tvAnnouncement.setTextColor(Color.BLACK)
                            tvTime.setTextColor(Color.BLACK)
                        }

                        Announcement.TYPE_PLAYER_GUESSED_WORD -> {
                            root.setBackgroundColor(Color.YELLOW)
                            tvAnnouncement.setTextColor(Color.BLACK)
                            tvTime.setTextColor(Color.BLACK)
                        }

                        Announcement.TYPE_PLAYER_JOINED -> {
                            root.setBackgroundColor(Color.GREEN)
                            tvAnnouncement.setTextColor(Color.BLACK)
                            tvTime.setTextColor(Color.BLACK)
                        }

                        Announcement.TYPE_PLAYER_LEFT -> {
                            root.setBackgroundColor(Color.RED)
                            tvAnnouncement.setTextColor(Color.WHITE)
                            tvTime.setTextColor(Color.WHITE)
                        }
                    }
                }
            }

            is IncomingChatMessageViewHolder -> {
                val chat = chatObjects[position] as ChatMessage

                holder.binding.apply {
                    tvMessage.text = chat.message
                    tvUsername.text = chat.from

                    val dateFormat = SimpleDateFormat("kk:mm:ss dd.MM.yyyy", Locale.getDefault())
                    val date = dateFormat.format(chat.timestamp)

                    tvTime.text = date
                }
            }


            is OutgoingChatMessageViewHolder -> {
                val chat = chatObjects[position] as ChatMessage

                holder.binding.apply {
                    tvMessage.text = chat.message
                    tvUsername.text = chat.from

                    val dateFormat = SimpleDateFormat("kk:mm:ss dd.MM.yyyy", Locale.getDefault())
                    val date = dateFormat.format(chat.timestamp)

                    tvTime.text = date
                }
            }
        }
    }
}