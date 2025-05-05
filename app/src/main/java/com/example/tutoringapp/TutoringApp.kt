package com.example.tutoringapp

import android.app.Application
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.app.NotificationCompat

class TutoringApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}

// Data Models
data class User(
    val uid: String = "",
    val name: String = "",
    val role: String = "", // "Teacher" or "Student"
    val subjects: List<String> = emptyList(), // For teachers
    val availability: List<Availability> = emptyList(), // For teachers
    val bio: String = "" // For both
)

data class Availability(
    val date: String = "", // e.g., "2025-05-03"
    val timeSlots: List<String> = emptyList() // e.g., ["10:00-11:00", "14:00-15:00"]
)

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0
)

// Sign-Up Fragment (View-based for compatibility)
class SignupFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signup, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailEditText = view.findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = view.findViewById<EditText>(R.id.passwordEditText)
        val nameEditText = view.findViewById<EditText>(R.id.nameEditText)
        val roleRadioGroup = view.findViewById<android.widget.RadioGroup>(R.id.roleRadioGroup)
        val signupButton = view.findViewById<Button>(R.id.signupButton)

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val name = nameEditText.text.toString()
            val role = if (roleRadioGroup.checkedRadioButtonId == R.id.teacherRadioButton) {
                "Teacher"
            } else {
                "Student"
            }

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = User(
                                uid = auth.currentUser?.uid ?: "",
                                name = name,
                                role = role
                            )
                            db.collection("users").document(user.uid).set(user)
                                .addOnSuccessListener {
                                    findNavController().navigate(
                                        if (role == "Teacher") R.id.teacherProfileFragment
                                        else R.id.dashboardFragment
                                    )
                                }
                        } else {
                            Toast.makeText(context, "Sign-up failed", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        return view
    }
}

// Teacher Profile Fragment (View-based)
class TeacherProfileFragment : Fragment() {
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_teacher_profile, container, false)
        db = FirebaseFirestore.getInstance()

        val subjectsEditText = view.findViewById<EditText>(R.id.subjectsEditText)
        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val timeSlotsEditText = view.findViewById<EditText>(R.id.timeSlotsEditText)
        val bioEditText = view.findViewById<EditText>(R.id.bioEditText)
        val saveButton = view.findViewById<Button>(R.id.saveProfileButton)

        var selectedDate = ""
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth)
        }

        saveButton.setOnClickListener {
            val subjects = subjectsEditText.text.toString().split(",").map { it.trim() }
            val timeSlots = timeSlotsEditText.text.toString().split(",").map { it.trim() }
            val bio = bioEditText.text.toString()

            if (selectedDate.isEmpty()) {
                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val availability = listOf(Availability(selectedDate, timeSlots))

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            db.collection("users").document(userId)
                .update(
                    mapOf(
                        "subjects" to subjects,
                        "availability" to availability,
                        "bio" to bio
                    )
                )
                .addOnSuccessListener {
                    findNavController().navigate(R.id.dashboardFragment)
                }
        }

        return view
    }
}

// Profile Fragment (View-based)
class ProfileFragment : Fragment() {
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        db = FirebaseFirestore.getInstance()

        val userId = arguments?.getString("userId") ?: return view
        val nameTextView = view.findViewById<TextView>(R.id.profileNameTextView)
        val bioTextView = view.findViewById<TextView>(R.id.profileBioTextView)
        val subjectsTextView = view.findViewById<TextView>(R.id.profileSubjectsTextView)
        val availabilityTextView = view.findViewById<TextView>(R.id.profileAvailabilityTextView)
        val chatButton = view.findViewById<Button>(R.id.startChatButton)

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java) ?: return@addOnSuccessListener
                nameTextView.text = user.name
                bioTextView.text = user.bio
                subjectsTextView.text = if (user.subjects.isNotEmpty()) {
                    user.subjects.joinToString(", ")
                } else {
                    "N/A"
                }
                availabilityTextView.text = if (user.availability.isNotEmpty()) {
                    user.availability.joinToString("\n") { "${it.date}: ${it.timeSlots.joinToString(", ")}" }
                } else {
                    "No availability"
                }
                chatButton.setOnClickListener {
                    val bundle = Bundle().apply { putString("teacherId", userId) }
                    findNavController().navigate(R.id.chatFragment, bundle)
                }
            }

        return view
    }
}

// Dashboard Fragment (View-based)
class DashboardFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TeacherAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        db = FirebaseFirestore.getInstance()
        recyclerView = view.findViewById(R.id.teacherRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = TeacherAdapter { teacher ->
            val bundle = Bundle().apply { putString("userId", teacher.uid) }
            findNavController().navigate(R.id.profileFragment, bundle)
        }
        recyclerView.adapter = adapter

        // Load teachers
        db.collection("users")
            .whereEqualTo("role", "Teacher")
            .addSnapshotListener { snapshot: QuerySnapshot?, _ ->
                val teachers = snapshot?.toObjects(User::class.java) ?: emptyList()
                adapter.submitList(teachers)
            }

        // Filter FAB
        view.findViewById<FloatingActionButton>(R.id.filterFab).setOnClickListener {
            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_filter)
            val subjectEditText = dialog.findViewById<EditText>(R.id.filterSubjectEditText)
            dialog.findViewById<Button>(R.id.applyFilterButton).setOnClickListener {
                val subject = subjectEditText.text.toString().trim()
                if (subject.isNotEmpty()) {
                    db.collection("users")
                        .whereEqualTo("role", "Teacher")
                        .whereArrayContains("subjects", subject)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val filteredTeachers = snapshot.toObjects(User::class.java)
                            adapter.submitList(filteredTeachers)
                        }
                } else {
                    db.collection("users")
                        .whereEqualTo("role", "Teacher")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val allTeachers = snapshot.toObjects(User::class.java)
                            adapter.submitList(allTeachers)
                        }
                }
                dialog.dismiss()
            }
            dialog.show()
        }

        return view
    }
}

// Teacher Adapter for RecyclerView
class TeacherAdapter(private val onClick: (User) -> Unit) :
    RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder>() {
    private var teachers: List<User> = emptyList()

    class TeacherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.teacherNameTextView)
        val subjectsTextView: TextView = itemView.findViewById(R.id.subjectsTextView)
        val cardView: CardView = itemView.findViewById(R.id.teacherCardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_teacher, parent, false)
        return TeacherViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeacherViewHolder, position: Int) {
        val teacher = teachers[position]
        holder.nameTextView.text = teacher.name
        holder.subjectsTextView.text = teacher.subjects.joinToString(", ")
        holder.cardView.setOnClickListener { onClick(teacher) }
    }

    override fun getItemCount(): Int = teachers.size

    fun submitList(newTeachers: List<User>) {
        teachers = newTeachers
        notifyDataSetChanged()
    }
}

// Chat Fragment (View-based)
class ChatFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        db = FirebaseFirestore.getInstance()
        recyclerView = view.findViewById(R.id.chatRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = MessageAdapter()
        recyclerView.adapter = adapter

        val teacherId = arguments?.getString("teacherId") ?: return view
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        val chatId = if (userId < teacherId) "$userId$teacherId" else "$teacherId$userId"

        // Load messages
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot: QuerySnapshot?, _ ->
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                adapter.submitList(messages)
            }

        // Send message
        val messageEditText = view.findViewById<EditText>(R.id.messageEditText)
        view.findViewById<Button>(R.id.sendButton).setOnClickListener {
            val content = messageEditText.text.toString()
            if (content.isNotEmpty()) {
                val message = Message(
                    senderId = userId,
                    receiverId = teacherId,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
                db.collection("chats").document(chatId).collection("messages")
                    .add(message)
                    .addOnSuccessListener {
                        // Trigger notification (handled by Firebase Function or backend)
                    }
                messageEditText.text.clear()
            }
        }

        return view
    }
}

// Message Adapter for RecyclerView
class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private var messages: List<Message> = emptyList()

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        val cardView: CardView = itemView.findViewById(R.id.messageCardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == 0) R.layout.item_message_sent else R.layout.item_message_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageTextView.text = message.content
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid) 0 else 1
    }

    fun submitList(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}

// Firebase Messaging Service for Notifications
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.notification?.title ?: "New Message"
        val body = remoteMessage.notification?.body ?: "You have a new message!"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "tutoring_app_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tutoring App Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save token to Firestore for sending targeted notifications
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update("fcmToken", token)
    }
}