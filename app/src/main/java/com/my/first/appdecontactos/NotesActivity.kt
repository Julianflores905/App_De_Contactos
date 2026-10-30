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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotesActivity : AppCompatActivity() {

    private lateinit var tvWelcomeUser: TextView
    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteContent: EditText
    private lateinit var btnAddNote: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var rvNotes: RecyclerView
    private lateinit var tvEmptyNotes: TextView

    private lateinit var adapter: NotesAdapter
    private val notesList = mutableListOf<Note>()

    private var usuario: String = "Invitado"
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notes)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        usuario = intent.getStringExtra("EXTRA_USUARIO") ?: "Invitado"

        tvWelcomeUser = findViewById(R.id.tvWelcomeUser)
        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteContent = findViewById(R.id.etNoteContent)
        btnAddNote = findViewById(R.id.btnAddNote)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        rvNotes = findViewById(R.id.rvNotes)
        tvEmptyNotes = findViewById(R.id.tvEmptyNotes)

        tvWelcomeUser.text = getString(R.string.bienvenido_usuario, usuario)

        adapter = NotesAdapter(notesList) { note ->
            deleteNote(note)
        }

        rvNotes.layoutManager = LinearLayoutManager(this)
        rvNotes.adapter = adapter

        btnAddNote.setOnClickListener {
            val title = etNoteTitle.text.toString().trim()
            val content = etNoteContent.text.toString().trim()

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_campos_nota), Toast.LENGTH_SHORT).show()
            } else {
                addNote(title, content)
            }
        }

        btnCerrarSesion.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        listenToNotes()
    }

    private fun listenToNotes() {
        db.collection("users")
            .document(usuario)
            .collection("notes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    notesList.clear()
                    for (doc in snapshot.documents) {
                        val title = doc.getString("title") ?: ""
                        val content = doc.getString("content") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        notesList.add(Note(id = doc.id, title = title, content = content, timestamp = timestamp))
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

        db.collection("users")
            .document(usuario)
            .collection("notes")
            .add(noteData)
            .addOnSuccessListener {
                etNoteTitle.text.clear()
                etNoteContent.text.clear()
                Toast.makeText(this, getString(R.string.nota_agregada), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // Fallback local en caso de desconexión
                val localNote = Note(
                    id = System.currentTimeMillis().toString(),
                    title = title,
                    content = content
                )
                notesList.add(0, localNote)
                adapter.updateNotes(notesList)
                updateEmptyState()
                etNoteTitle.text.clear()
                etNoteContent.text.clear()
                Toast.makeText(this, getString(R.string.nota_agregada), Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteNote(note: Note) {
        if (note.id.isNotEmpty()) {
            db.collection("users")
                .document(usuario)
                .collection("notes")
                .document(note.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.nota_eliminada), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    notesList.remove(note)
                    adapter.updateNotes(notesList)
                    updateEmptyState()
                    Toast.makeText(this, getString(R.string.nota_eliminada), Toast.LENGTH_SHORT).show()
                }
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
}
