package com.example.tutoringapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class Chat(
    val chatId: String = "",
    val participantId: String = "", // Teacher ID for students, Student ID for teachers
    val participantName: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0
)

class ChatListFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private var recyclerView: RecyclerView? = null
    private var adapter: ChatListAdapter? = null
    private var progressBar: ProgressBar? = null
    private val tag = "ChatListFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            val view = inflater.inflate(R.layout.fragment_chat_list, container, false)
            db = FirebaseFirestore.getInstance()

            recyclerView = view.findViewById(R.id.chatListRecyclerView)
            progressBar = view.findViewById(R.id.progressBar)
            if (recyclerView == null || progressBar == null) {
                Log.e(tag, "RecyclerView or ProgressBar is null, check fragment_chat_list.xml")
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: UI components missing", Toast.LENGTH_SHORT).show()
                }
                return view
            }

            recyclerView?.layoutManager = LinearLayoutManager(context)
            adapter = ChatListAdapter { chat ->
                val bundle = Bundle().apply {
                    putString("teacherId", chat.participantId)
                }
                findNavController().navigate(R.id.action_chatListFragment_to_chatFragment, bundle)
            }
            recyclerView?.adapter = adapter
            Log.d(tag, "Adapter set for RecyclerView")

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                Log.e(tag, "User not authenticated")
                if (isAdded) {
                    Toast.makeText(requireContext(), "Please log in", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_chatListFragment_to_loginFragment)
                }
                return view
            }

            progressBar?.visibility = View.VISIBLE
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (!isAdded) return@addOnSuccessListener
                    if (!document.exists()) {
                        progressBar?.visibility = View.GONE
                        Log.e(tag, "User document does not exist for UID: $userId")
                        Toast.makeText(requireContext(), "User profile not found. Please sign up again.", Toast.LENGTH_SHORT).show()
                        FirebaseAuth.getInstance().signOut()
                        findNavController().navigate(R.id.action_chatListFragment_to_loginFragment)
                        return@addOnSuccessListener
                    }
                    loadChats(userId)
                }
                .addOnFailureListener { e ->
                    if (!isAdded) return@addOnFailureListener
                    progressBar?.visibility = View.GONE
                    Log.e(tag, "Failed to verify user document: ${e.message}", e)
                    Toast.makeText(requireContext(), "Failed to load chats: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            return view
        } catch (e: Exception) {
            Log.e(tag, "ChatListFragment onCreateView crashed: ${e.message}", e)
            if (isAdded) {
                Toast.makeText(requireContext(), "Error loading chats", Toast.LENGTH_SHORT).show()
            }
            return null
        }
    }

    private fun loadChats(userId: String) {
        db.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("created", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (!isAdded) return@addSnapshotListener
                progressBar?.visibility = View.GONE
                if (e != null) {
                    Log.e(tag, "Failed to load chats: ${e.message}, userId=$userId", e)
                    Toast.makeText(requireContext(), "Failed to load chats: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val chats = mutableListOf<Chat>()
                snapshot?.documents?.forEach { doc ->
                    val chatId = doc.id
                    val otherId = chatId.split("_").find { it != userId } ?: return@forEach
                    db.collection("users").document(otherId).get()
                        .addOnSuccessListener userDocListener@{ userDoc ->
                            if (!isAdded) return@userDocListener
                            val name = userDoc.getString("name") ?: "Unknown"
                            db.collection("chats").document(chatId).collection("messages")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener messageSnapshotListener@{ messageSnapshot ->
                                    if (!isAdded) return@messageSnapshotListener
                                    val lastMessage = messageSnapshot.documents.firstOrNull()?.getString("content") ?: ""
                                    val timestamp = messageSnapshot.documents.firstOrNull()?.getLong("timestamp") ?: 0
                                    chats.add(Chat(chatId, otherId, name, lastMessage, timestamp))
                                    adapter?.submitList(chats.sortedByDescending { it.timestamp })
                                }
                        }
                }
            }
    }
}

class ChatListAdapter(
    private val onClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {
    private var chats: List<Chat> = emptyList()

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView? = itemView.findViewById(R.id.participantNameTextView)
        val messageTextView: TextView? = itemView.findViewById(R.id.lastMessageTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        if (holder.nameTextView == null || holder.messageTextView == null) {
            Log.e("ChatListAdapter", "nameTextView or messageTextView is null at position=$position")
            return
        }
        holder.nameTextView.text = chat.participantName
        holder.messageTextView.text = chat.lastMessage
        holder.itemView.setOnClickListener { onClick(chat) }
    }

    override fun getItemCount(): Int = chats.size

    fun submitList(newChats: List<Chat>) {
        val diffCallback = ChatDiffCallback(chats, newChats)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        chats = newChats.toList()
        diffResult.dispatchUpdatesTo(this)
    }

    private class ChatDiffCallback(
        private val oldList: List<Chat>,
        private val newList: List<Chat>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].chatId == newList[newItemPosition].chatId
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}