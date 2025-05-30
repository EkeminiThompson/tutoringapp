package com.example.tutoringapp

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale
import android.app.NotificationChannel
import android.app.NotificationManager
import android.Manifest

class TutoringApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            try {
                when (item.itemId) {
                    R.id.nav_dashboard -> {
                        navController.navigate(R.id.dashboardFragment)
                        true
                    }
                    R.id.nav_bookings -> {
                        navController.navigate(R.id.bookingHistoryFragment)
                        true
                    }
                    R.id.nav_chats -> {
                        Toast.makeText(this, "Select a teacher to start a chat", Toast.LENGTH_SHORT).show()
                        false
                    }
                    R.id.nav_settings -> {
                        navController.navigate(R.id.settingsFragment)
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Navigation error: ${e.message}", e)
                Toast.makeText(this, "Navigation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        if (FirebaseAuth.getInstance().currentUser == null) {
            navController.navigate(R.id.loginFragment)
        } else {
            navController.navigate(R.id.dashboardFragment)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted")
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class User(
    val uid: String = "",
    val name: String = "",
    val role: String = "",
    val subjects: List<String> = emptyList(),
    val availability: List<Availability> = emptyList(),
    val bio: String = "",
    val fcmToken: String? = null,
    val profilePhotoUrl: String? = null,
    val averageRating: Float = 0f
)

data class Availability(
    val date: String = "",
    val timeSlots: List<String> = emptyList()
)

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0
)

data class Booking(
    val id: String = "",
    val studentId: String = "",
    val teacherId: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val status: String = "Pending",
    val timestamp: Long = 0
)

data class Rating(
    val id: String = "",
    val studentId: String = "",
    val teacherId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Long = 0
)

class LoginFragment : Fragment() {
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        auth = FirebaseAuth.getInstance()

        val emailEditText = view.findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = view.findViewById<EditText>(R.id.passwordEditText)
        val loginButton = view.findViewById<Button>(R.id.loginButton)
        val signupButton = view.findViewById<Button>(R.id.signupButton)
        val resetPasswordButton = view.findViewById<Button>(R.id.resetPasswordButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        progressBar.visibility = View.GONE
                        if (task.isSuccessful) {
                            findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
                        } else {
                            Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        signupButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }

        resetPasswordButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Password reset email sent", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(context, "Enter your email", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}

class SignupFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "SignupFragment"

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
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            val role = if (roleRadioGroup.checkedRadioButtonId == R.id.teacherRadioButton) {
                "Teacher"
            } else {
                "Student"
            }

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: run {
                                progressBar.visibility = View.GONE
                                Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                                return@addOnCompleteListener
                            }
                            val user = User(
                                uid = userId,
                                name = name,
                                role = role
                            )
                            // Retry user document creation up to 3 times
                            saveUserWithRetry(user, 3, progressBar, role)
                        } else {
                            progressBar.visibility = View.GONE
                            Toast.makeText(context, "Sign-up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun saveUserWithRetry(user: User, retries: Int, progressBar: ProgressBar, role: String) {
        db.collection("users").document(user.uid).set(user)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Log.d(TAG, "User document created for UID: ${user.uid}")
                findNavController().navigate(
                    if (role == "Teacher") R.id.action_signupFragment_to_teacherProfileFragment
                    else R.id.action_signupFragment_to_dashboardFragment
                )
            }
            .addOnFailureListener { e ->
                if (retries > 0) {
                    Log.w(TAG, "Failed to save user document, retrying... ($retries retries left): ${e.message}")
                    saveUserWithRetry(user, retries - 1, progressBar, role)
                } else {
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Failed to save user document after retries: ${e.message}", e)
                    Toast.makeText(context, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    auth.signOut() // Sign out to prevent partial state
                    findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
                }
            }
    }
}

class TeacherProfileFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private val availabilityList = mutableListOf<Availability>()

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
        val addAvailabilityButton = view.findViewById<Button>(R.id.addAvailabilityButton)
        val saveButton = view.findViewById<Button>(R.id.saveProfileButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        var selectedDate = ""
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth)
        }

        addAvailabilityButton.setOnClickListener {
            val timeSlots = timeSlotsEditText.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (selectedDate.isEmpty()) {
                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (timeSlots.isEmpty()) {
                Toast.makeText(context, "Please enter time slots", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            availabilityList.add(Availability(selectedDate, timeSlots))
            timeSlotsEditText.text.clear()
            Toast.makeText(context, "Availability added for $selectedDate", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener {
            val subjects = subjectsEditText.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val bio = bioEditText.text.toString()
            if (subjects.isEmpty()) {
                Toast.makeText(context, "Please enter at least one subject", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (availabilityList.isEmpty()) {
                Toast.makeText(context, "Please add at least one availability", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            progressBar.visibility = View.VISIBLE
            db.collection("users").document(userId)
                .update(
                    mapOf(
                        "subjects" to subjects,
                        "availability" to availabilityList,
                        "bio" to bio
                    )
                )
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    findNavController().navigate(R.id.action_teacherProfileFragment_to_dashboardFragment)
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to save profile", Toast.LENGTH_SHORT).show()
                }
        }

        return view
    }
}

class EditProfileFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var photoUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoUri = uri
        view?.findViewById<ImageView>(R.id.profilePhotoImageView)?.let {
            Glide.with(this).load(uri).into(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val nameEditText = view.findViewById<EditText>(R.id.nameEditText)
        val bioEditText = view.findViewById<EditText>(R.id.bioEditText)
        val photoImageView = view.findViewById<ImageView>(R.id.profilePhotoImageView)
        val uploadPhotoButton = view.findViewById<Button>(R.id.uploadPhotoButton)
        val saveButton = view.findViewById<Button>(R.id.saveProfileButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java) ?: return@addOnSuccessListener
                nameEditText.setText(user.name)
                bioEditText.setText(user.bio)
                if (user.profilePhotoUrl != null) {
                    Glide.with(this).load(user.profilePhotoUrl).into(photoImageView)
                }
            }

        uploadPhotoButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val bio = bioEditText.text.toString()
            if (name.isEmpty()) {
                Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            val updates = mutableMapOf<String, Any>("name" to name, "bio" to bio)

            if (photoUri != null) {
                val ref = storage.reference.child("profile_photos/$userId")
                ref.putFile(photoUri!!)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { uri ->
                            updates["profilePhotoUrl"] = uri.toString()
                            updateProfile(userId, updates, progressBar)
                        }
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(context, "Failed to upload photo", Toast.LENGTH_SHORT).show()
                    }
            } else {
                updateProfile(userId, updates, progressBar)
            }
        }

        return view
    }

    private fun updateProfile(userId: String, updates: Map<String, Any>, progressBar: ProgressBar) {
        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_editProfileFragment_to_settingsFragment)
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }
}

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
        val ratingBar = view.findViewById<RatingBar>(R.id.profileRatingBar)
        val photoImageView = view.findViewById<ImageView>(R.id.profilePhotoImageView)
        val chatButton = view.findViewById<Button>(R.id.startChatButton)
        val bookButton = view.findViewById<Button>(R.id.bookButton)
        val rateButton = view.findViewById<Button>(R.id.rateButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        progressBar.visibility = View.VISIBLE
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                val user = document.toObject(User::class.java) ?: return@addOnSuccessListener
                nameTextView.text = user.name
                bioTextView.text = user.bio
                subjectsTextView.text = if (user.subjects.isNotEmpty()) {
                    user.subjects.joinToString(", ")
                } else {
                    "N/A"
                }
                availabilityTextView.text = if (user.availability.isNotEmpty()) {
                    user.availability.joinToString("\n") { avail ->
                        "${avail.date}: ${avail.timeSlots.joinToString(", ")}"
                    }
                } else {
                    "No availability"
                }
                ratingBar.rating = user.averageRating
                if (user.profilePhotoUrl != null) {
                    Glide.with(this).load(user.profilePhotoUrl).into(photoImageView)
                }
                chatButton.setOnClickListener {
                    val bundle = Bundle().apply { putString("teacherId", userId) }
                    findNavController().navigate(R.id.action_profileFragment_to_chatFragment, bundle)
                }
                bookButton.setOnClickListener {
                    val bundle = Bundle().apply { putString("teacherId", userId) }
                    findNavController().navigate(R.id.action_profileFragment_to_bookingFragment, bundle)
                }
                rateButton.setOnClickListener {
                    val dialog = Dialog(requireContext())
                    dialog.setContentView(R.layout.dialog_rate)
                    val ratingBar = dialog.findViewById<RatingBar>(R.id.rateRatingBar)
                    val commentEditText = dialog.findViewById<EditText>(R.id.commentEditText)
                    dialog.findViewById<Button>(R.id.submitRatingButton).setOnClickListener {
                        val rating = ratingBar.rating.toInt()
                        val comment = commentEditText.text.toString().trim()
                        if (rating > 0) {
                            val ratingObj = Rating(
                                id = db.collection("ratings").document().id,
                                studentId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                teacherId = userId,
                                rating = rating,
                                comment = comment,
                                timestamp = System.currentTimeMillis()
                            )
                            db.collection("ratings").document(ratingObj.id).set(ratingObj)
                                .addOnSuccessListener {
                                    updateTeacherRating(userId)
                                    Toast.makeText(context, "Rating submitted", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to submit rating", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
                        }
                    }
                    dialog.show()
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }

        return view
    }

    private fun updateTeacherRating(teacherId: String) {
        db.collection("ratings").whereEqualTo("teacherId", teacherId).get()
            .addOnSuccessListener { snapshot ->
                val ratings = snapshot.toObjects(Rating::class.java)
                val average = if (ratings.isNotEmpty()) {
                    ratings.sumOf { it.rating } / ratings.size.toFloat()
                } else 0f
                db.collection("users").document(teacherId)
                    .update("averageRating", average)
            }
    }
}

class DashboardFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TeacherAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        db = FirebaseFirestore.getInstance()
        recyclerView = view.findViewById(R.id.teacherRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = TeacherAdapter { teacher ->
            val bundle = Bundle().apply { putString("userId", teacher.uid) }
            findNavController().navigate(R.id.action_dashboardFragment_to_profileFragment, bundle)
        }
        recyclerView.adapter = adapter

        progressBar.visibility = View.VISIBLE
        db.collection("users")
            .whereEqualTo("role", "Teacher")
            .addSnapshotListener { snapshot, e ->
                progressBar.visibility = View.GONE
                if (e != null) {
                    Toast.makeText(context, "Failed to load teachers", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val teachers = snapshot?.toObjects(User::class.java) ?: emptyList()
                adapter.submitList(teachers)
            }

        view.findViewById<FloatingActionButton>(R.id.filterFab).setOnClickListener {
            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_filter)
            val subjectEditText = dialog.findViewById<EditText>(R.id.filterSubjectEditText)
            dialog.findViewById<Button>(R.id.applyFilterButton).setOnClickListener {
                val subject = subjectEditText.text.toString().trim()
                progressBar.visibility = View.VISIBLE
                if (subject.isNotEmpty()) {
                    db.collection("users")
                        .whereEqualTo("role", "Teacher")
                        .whereArrayContains("subjects", subject)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            progressBar.visibility = View.GONE
                            val filteredTeachers = snapshot.toObjects(User::class.java)
                            adapter.submitList(filteredTeachers)
                        }
                        .addOnFailureListener {
                            progressBar.visibility = View.GONE
                            Toast.makeText(context, "Failed to apply filter", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    db.collection("users")
                        .whereEqualTo("role", "Teacher")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            progressBar.visibility = View.GONE
                            val allTeachers = snapshot.toObjects(User::class.java)
                            adapter.submitList(allTeachers)
                        }
                        .addOnFailureListener {
                            progressBar.visibility = View.GONE
                            Toast.makeText(context, "Failed to load teachers", Toast.LENGTH_SHORT).show()
                        }
                }
                dialog.dismiss()
            }
            dialog.show()
        }

        return view
    }
}

class TeacherAdapter(private val onClick: (User) -> Unit) :
    RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder>() {
    private var teachers: List<User> = emptyList()

    class TeacherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.teacherNameTextView)
        val subjectsTextView: TextView = itemView.findViewById(R.id.subjectsTextView)
        val ratingBar: RatingBar = itemView.findViewById(R.id.teacherRatingBar)
        val photoImageView: ImageView = itemView.findViewById(R.id.teacherPhotoImageView)
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
        holder.ratingBar.rating = teacher.averageRating
        if (teacher.profilePhotoUrl != null) {
            Glide.with(holder.itemView.context).load(teacher.profilePhotoUrl).into(holder.photoImageView)
        }
        holder.cardView.setOnClickListener { onClick(teacher) }
    }

    override fun getItemCount(): Int = teachers.size

    fun submitList(newTeachers: List<User>) {
        val diffCallback = UserDiffCallback(teachers, newTeachers)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        teachers = newTeachers.toList()
        diffResult.dispatchUpdatesTo(this)
    }

    private class UserDiffCallback(
        private val oldList: List<User>,
        private val newList: List<User>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].uid == newList[newItemPosition].uid
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

class BookingFragment : Fragment() {
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_booking, container, false)
        db = FirebaseFirestore.getInstance()

        val teacherId = arguments?.getString("teacherId") ?: return view
        val studentId = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        val dateEditText = view.findViewById<EditText>(R.id.dateEditText)
        val timeSlotEditText = view.findViewById<EditText>(R.id.timeSlotEditText)
        val bookButton = view.findViewById<Button>(R.id.bookButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        db.collection("users").document(teacherId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java) ?: return@addOnSuccessListener
                val availabilityText = user.availability.joinToString("\n") { avail ->
                    "${avail.date}: ${avail.timeSlots.joinToString(", ")}"
                }
                view.findViewById<TextView>(R.id.availabilityTextView).text = availabilityText
            }

        bookButton.setOnClickListener {
            val date = dateEditText.text.toString().trim()
            val timeSlot = timeSlotEditText.text.toString().trim()
            if (date.isEmpty() || timeSlot.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            db.collection("users").document(teacherId).get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(User::class.java) ?: return@addOnSuccessListener
                    val isValid = user.availability.any { avail ->
                        avail.date == date && avail.timeSlots.contains(timeSlot)
                    }
                    if (isValid) {
                        val booking = Booking(
                            id = db.collection("bookings").document().id,
                            studentId = studentId,
                            teacherId = teacherId,
                            date = date,
                            timeSlot = timeSlot,
                            timestamp = System.currentTimeMillis()
                        )
                        db.collection("bookings").document(booking.id).set(booking)
                            .addOnSuccessListener {
                                progressBar.visibility = View.GONE
                                Toast.makeText(context, "Booking requested", Toast.LENGTH_SHORT).show()
                                findNavController().navigate(R.id.action_bookingFragment_to_bookingHistoryFragment)
                            }
                            .addOnFailureListener {
                                progressBar.visibility = View.GONE
                                Toast.makeText(context, "Failed to request booking", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(context, "Invalid date or time slot", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        return view
    }
}

class BookingHistoryFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookingAdapter
    private lateinit var progressBar: ProgressBar
    private val TAG = "BookingHistoryFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_booking_history, container, false)
        db = FirebaseFirestore.getInstance()
        recyclerView = view.findViewById(R.id.bookingRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = BookingAdapter { booking, action ->
            when (action) {
                "Confirm" -> updateBookingStatus(booking, "Confirmed")
                "Cancel" -> updateBookingStatus(booking, "Cancelled")
                "Complete" -> updateBookingStatus(booking, "Completed")
            }
        }
        recyclerView.adapter = adapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        progressBar.visibility = View.VISIBLE
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                val userRole = user?.role ?: "Student"
                val query = if (userRole == "Teacher") {
                    db.collection("bookings").whereEqualTo("teacherId", userId)
                } else {
                    db.collection("users").whereEqualTo("studentId", userId)
                }
                query.addSnapshotListener { snapshot, e ->
                    progressBar.visibility = View.GONE
                    if (e != null) {
                        Log.e(TAG, "Failed to load bookings: ${e.message}", e)
                        Toast.makeText(context, "Failed to load bookings: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    val bookings = snapshot?.toObjects(Booking::class.java) ?: emptyList()
                    adapter.submitList(bookings, userRole)
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e(TAG, "Failed to load user role: ${e.message}", e)
                Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }

        return view
    }

    private fun updateBookingStatus(booking: Booking, status: String) {
        db.collection("bookings").document(booking.id)
            .update("status", status)
            .addOnSuccessListener {
                Toast.makeText(context, "Booking $status", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update booking", Toast.LENGTH_SHORT).show()
            }
    }
}

class BookingAdapter(
    private val onAction: (Booking, String) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {
    private var bookings: List<Booking> = emptyList()
    private var userRole: String = "Student"

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val teacherNameTextView: TextView = itemView.findViewById(R.id.teacherNameTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val timeSlotTextView: TextView = itemView.findViewById(R.id.timeSlotTextView)
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        val confirmButton: Button = itemView.findViewById(R.id.confirmButton)
        val cancelButton: Button = itemView.findViewById(R.id.cancelButton)
        val completeButton: Button = itemView.findViewById(R.id.completeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        FirebaseFirestore.getInstance().collection("users").document(booking.teacherId).get()
            .addOnSuccessListener { document ->
                holder.teacherNameTextView.text = document.toObject(User::class.java)?.name ?: "Unknown"
            }
        holder.dateTextView.text = booking.date
        holder.timeSlotTextView.text = booking.timeSlot
        holder.statusTextView.text = booking.status

        holder.confirmButton.visibility = if (userRole == "Teacher" && booking.status == "Pending") View.VISIBLE else View.GONE
        holder.cancelButton.visibility = if (booking.status == "Pending" || booking.status == "Confirmed") View.VISIBLE else View.GONE
        holder.completeButton.visibility = if (userRole == "Teacher" && booking.status == "Confirmed") View.VISIBLE else View.GONE

        holder.confirmButton.setOnClickListener { onAction(booking, "Confirm") }
        holder.cancelButton.setOnClickListener { onAction(booking, "Cancel") }
        holder.completeButton.setOnClickListener { onAction(booking, "Complete") }
    }

    override fun getItemCount(): Int = bookings.size

    fun submitList(newBookings: List<Booking>, role: String) {
        val diffCallback = BookingDiffCallback(bookings, newBookings)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        bookings = newBookings.toList()
        userRole = role
        diffResult.dispatchUpdatesTo(this)
    }

    private class BookingDiffCallback(
        private val oldList: List<Booking>,
        private val newList: List<Booking>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

class ChatFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private lateinit var progressBar: ProgressBar
    private val TAG = "ChatFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        db = FirebaseFirestore.getInstance()
        recyclerView = view.findViewById(R.id.chatRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = MessageAdapter()
        recyclerView.adapter = adapter

        val teacherId = arguments?.getString("teacherId") ?: run {
            Toast.makeText(context, "Teacher ID missing", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return view
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.e(TAG, "User not authenticated")
            Toast.makeText(context, "Please log in to send messages", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_chatFragment_to_loginFragment)
            return view
        }
        if (teacherId.isEmpty()) {
            Log.e(TAG, "Invalid teacherId: empty")
            Toast.makeText(context, "Invalid teacher ID", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return view
        }
        val chatId = if (userId < teacherId) "${userId}_${teacherId}" else "${teacherId}_${userId}"
        Log.d(TAG, "Chat ID: $chatId, User ID: $userId, Teacher ID: $teacherId")

        // Check if user document exists
        progressBar.visibility = View.VISIBLE
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "User document does not exist for UID: $userId")
                    Toast.makeText(context, "User profile not found. Please sign up again.", Toast.LENGTH_SHORT).show()
                    FirebaseAuth.getInstance().signOut()
                    findNavController().navigate(R.id.action_chatFragment_to_loginFragment)
                    return@addOnSuccessListener
                }

                // Ensure parent chat document exists
                db.collection("chats").document(chatId).set(mapOf("created" to System.currentTimeMillis()))
                    .addOnSuccessListener {
                        Log.d(TAG, "Parent chat document created for chatId: $chatId")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to create parent chat document: ${e.message}", e)
                    }

                // Load messages
                db.collection("chats").document(chatId).collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, e ->
                        progressBar.visibility = View.GONE
                        if (e != null) {
                            Log.e(TAG, "Failed to load messages: ${e.message}", e)
                            Toast.makeText(context, "Failed to load messages: ${e.message}", Toast.LENGTH_SHORT).show()
                            return@addSnapshotListener
                        }
                        val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                        adapter.submitList(messages)
                        recyclerView.scrollToPosition(messages.size - 1)
                    }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e(TAG, "Failed to check user document: ${e.message}", e)
                Toast.makeText(context, "Failed to verify user profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        val messageEditText = view.findViewById<EditText>(R.id.messageEditText)
        view.findViewById<Button>(R.id.sendButton).setOnClickListener {
            val content = messageEditText.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(context, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            val message = Message(
                senderId = userId,
                receiverId = teacherId,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            Log.d(TAG, "Sending message: senderId=$userId, receiverId=$teacherId, chatId=$chatId")
            db.collection("chats").document(chatId).collection("messages")
                .add(message)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    messageEditText.text.clear()
                    recyclerView.scrollToPosition(adapter.itemCount - 1)
                    Log.d(TAG, "Message sent successfully")
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Failed to send message: ${e.message}, chatId=$chatId, userId=$userId", e)
                    Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        return view
    }
}

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
        holder.cardView.cardElevation = if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid) 4f else 2f
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid) 0 else 1
    }

    fun submitList(newMessages: List<Message>) {
        val diffCallback = MessageDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages = newMessages.toList()
        diffResult.dispatchUpdatesTo(this)
    }

    private class MessageDiffCallback(
        private val oldList: List<Message>,
        private val newList: List<Message>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].timestamp == newList[newItemPosition].timestamp &&
                    oldList[oldItemPosition].senderId == newList[newItemPosition].senderId
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

class SettingsFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "SettingsFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val editProfileButton = view.findViewById<Button>(R.id.editProfileButton)
        val notificationsToggle = view.findViewById<android.widget.Switch>(R.id.notificationsToggle)
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)

        val userId = auth.currentUser?.uid ?: run {
            Log.e(TAG, "User not authenticated")
            findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
            return view
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                notificationsToggle.isChecked = user?.fcmToken != null
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load user data: ${e.message}", e)
                Toast.makeText(context, "Failed to load settings", Toast.LENGTH_SHORT).show()
            }

        editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
        }

        notificationsToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    db.collection("users").document(userId)
                        .update("fcmToken", token)
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to enable notifications: ${e.message}", e)
                            Toast.makeText(context, "Failed to enable notifications", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                db.collection("users").document(userId)
                    .update("fcmToken", null)
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to disable notifications: ${e.message}", e)
                        Toast.makeText(context, "Failed to disable notifications", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
        }

        return view
    }
}

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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update("fcmToken", token)
    }
}

