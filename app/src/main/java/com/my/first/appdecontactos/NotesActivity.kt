package com.my.first.appdecontactos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotesActivity : AppCompatActivity() {

    private lateinit var tvWelcomeUser: TextView
    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteContent: EditText
    private lateinit var btnAddNote: Button
    private lateinit var btnIrALlamadas: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var rvNotes: RecyclerView
    private lateinit var tvEmptyNotes: TextView

    private lateinit var adapter: NotesAdapter
    private val notesList = mutableListOf<Note>()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var usuario: String = "Invitado"
    private var uid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notes)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        val currentUser = auth.currentUser

        if (currentUser == null) {
            volverAlLogin()
            return
        }

        uid = currentUser.uid
        usuario = intent.getStringExtra("EXTRA_USUARIO") ?: currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Usuario"

        tvWelcomeUser = findViewById(R.id.tvWelcomeUser)
        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteContent = findViewById(R.id.etNoteContent)
        btnAddNote = findViewById(R.id.btnAddNote)
        btnIrALlamadas = findViewById(R.id.btnIrALlamadas)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        rvNotes = findViewById(R.id.rvNotes)
        tvEmptyNotes = findViewById(R.id.tvEmptyNotes)

        btnIrALlamadas.setOnClickListener {
            val intent = Intent(this, CallsActivity::class.java)
            intent.putExtra("EXTRA_USUARIO", usuario)
            startActivity(intent)
        }

        tvWelcomeUser.text =
            getString(R.string.bienvenido_usuario, usuario)

        adapter = NotesAdapter(notesList) { note ->
            deleteNote(note)
        }

        rvNotes.layoutManager = LinearLayoutManager(this)
        rvNotes.adapter = adapter

        btnAddNote.setOnClickListener {
            val title = etNoteTitle.text.toString().trim()
            val content = etNoteContent.text.toString().trim()

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(
                    this,
                    "Completa el título y el contenido.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                addNote(title, content)
            }
        }

        btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }

        listenToNotes()
    }

    private fun cerrarSesion() {
        // Cerrar sesión en Firebase
        auth.signOut()

        // Cerrar sesión en Google Sign-In si aplica
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(this, gso).signOut()
        } catch (_: Exception) { }

        Toast.makeText(this, "Sesión cerrada correctamente.", Toast.LENGTH_SHORT).show()
        volverAlLogin()
    }

    private fun notesCollection() =
        db.collection("users")
            .document(uid)
            .collection("notes")

    private fun listenToNotes() {
        notesCollection()
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Error al cargar las notas: ${error.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    notesList.clear()

                    for (doc in snapshot.documents) {
                        notesList.add(
                            Note(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                content = doc.getString("content") ?: "",
                                timestamp = doc.getLong("timestamp")
                                    ?: System.currentTimeMillis()
                            )
                        )
                    }

                    adapter.updateNotes(notesList)
                    updateEmptyState()
                }
            }
    }

    private fun addNote(title: String, content: String) {
        val noteData = hashMapOf(
            "title" to title,
            "content" to content,
            "timestamp" to System.currentTimeMillis()
        )

        btnAddNote.isEnabled = false

        notesCollection()
            .add(noteData)
            .addOnSuccessListener {
                btnAddNote.isEnabled = true
                etNoteTitle.text.clear()
                etNoteContent.text.clear()

                Toast.makeText(
                    this,
                    "Nota guardada correctamente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                btnAddNote.isEnabled = true

                Toast.makeText(
                    this,
                    "No se pudo guardar la nota: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun deleteNote(note: Note) {
        if (note.id.isEmpty()) return

        notesCollection()
            .document(note.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Nota eliminada.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "No se pudo eliminar: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateEmptyState() {
        if (notesList.isEmpty()) {
            tvEmptyNotes.visibility = View.VISIBLE
            rvNotes.visibility = View.GONE
        } else {
            tvEmptyNotes.visibility = View.GONE
            rvNotes.visibility = View.VISIBLE
        }
    }

    private fun volverAlLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}
