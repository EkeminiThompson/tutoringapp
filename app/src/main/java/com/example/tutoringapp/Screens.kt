package com.example.tutoringapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(
    onNavigateToTeacherProfile: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Student") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = role == "Teacher",
                    onClick = { role = "Teacher" }
                )
                Text("Teacher")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = role == "Student",
                    onClick = { role = "Student" }
                )
                Text("Student/Parent")
            }
        }
        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = {
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
                                        if (role == "Teacher") {
                                            onNavigateToTeacherProfile()
                                        } else {
                                            onNavigateToDashboard()
                                        }
                                    }
                            } else {
                                error = "Sign-up failed: ${task.exception?.message}"
                            }
                        }
                } else {
                    error = "Please fill all fields"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Sign Up")
        }
    }
}

@Composable
fun TeacherProfileScreen(
    onNavigateToDashboard: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var subjects by remember { mutableStateOf("") }
    var timeSlots by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = subjects,
            onValueChange = { subjects = it },
            label = { Text("Subjects (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Select Availability Date",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        OutlinedTextField(
            value = selectedDate,
            onValueChange = { selectedDate = it },
            label = { Text("Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = timeSlots,
            onValueChange = { timeSlots = it },
            label = { Text("Time Slots (comma-separated, e.g., 10:00-11:00)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth()
        )
        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Button(
            onClick = {
                if (selectedDate.isEmpty()) {
                    error = "Please select a date"
                    return@Button
                }
                val subjectList = subjects.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val timeSlotList = timeSlots.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val availability = listOf(Availability(selectedDate, timeSlotList))
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@Button
                coroutineScope.launch {
                    try {
                        db.collection("users").document(userId)
                            .update(
                                mapOf(
                                    "subjects" to subjectList,
                                    "availability" to availability,
                                    "bio" to bio
                                )
                            )
                            .addOnSuccessListener {
                                onNavigateToDashboard()
                            }
                            .addOnFailureListener {
                                error = "Failed to save profile: ${it.message}"
                            }
                    } catch (e: Exception) {
                        error = "Error: ${e.message}"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Save Profile")
        }
    }
}

@Composable
fun ProfileScreen(
    userId: String,
    onNavigateToChat: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var user by remember { mutableStateOf<User?>(null) }
    var error by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        coroutineScope.launch {
            try {
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        user = document.toObject(User::class.java)
                    }
                    .addOnFailureListener {
                        error = "Failed to load profile: ${it.message}"
                    }
            } catch (e: Exception) {
                error = "Error: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        when {
            user != null -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = user!!.name,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = user!!.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = if (user!!.subjects.isNotEmpty()) {
                                "Subjects: ${user!!.subjects.joinToString(", ")}"
                            } else {
                                "Subjects: N/A"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = if (user!!.availability.isNotEmpty()) {
                                "Availability:\n${user!!.availability.joinToString("\n") { avail -> "${avail.date}: ${avail.timeSlots.joinToString(", ")}" }}"
                            } else {
                                "Availability: None"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                Button(
                    onClick = { onNavigateToChat(userId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Chat")
                }
            }
            error.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
fun DashboardScreen(
    onNavigateToProfile: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var teachers by remember { mutableStateOf<List<User>>(emptyList()) }
    var filterSubject by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(filterSubject) {
        val query = if (filterSubject.isNotEmpty()) {
            db.collection("users")
                .whereEqualTo("role", "Teacher")
                .whereArrayContains("subjects", filterSubject)
        } else {
            db.collection("users")
                .whereEqualTo("role", "Teacher")
        }
        query.addSnapshotListener { snapshot: QuerySnapshot?, _ ->
            teachers = snapshot?.toObjects(User::class.java) ?: emptyList()
        }
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Teachers") },
            text = {
                OutlinedTextField(
                    value = filterSubject,
                    onValueChange = { filterSubject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { showFilterDialog = false }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                Button(onClick = { showFilterDialog = false; filterSubject = "" }) {
                    Text("Clear")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        FloatingActionButton(
            onClick = { showFilterDialog = true },
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 16.dp)
        ) {
            Text("Filter")
        }
        LazyColumn {
            items(teachers) { teacher ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onNavigateToProfile(teacher.uid) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = teacher.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = teacher.subjects.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(teacherId: String) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val chatId = if (userId < teacherId) "$userId$teacherId" else "$teacherId$userId"
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(chatId) {
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot: QuerySnapshot?, _ ->
                messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages) { message ->
                val isSent = message.senderId == userId
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .padding(vertical = 4.dp)
                        .align(if (isSent) Alignment.End else Alignment.Start)
                ) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Message") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (messageText.isNotEmpty()) {
                        val message = Message(
                            senderId = userId,
                            receiverId = teacherId,
                            content = messageText,
                            timestamp = System.currentTimeMillis()
                        )
                        coroutineScope.launch {
                            db.collection("chats").document(chatId).collection("messages")
                                .add(message)
                            messageText = ""
                        }
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Send")
            }
        }
    }
}