package com.my.first.appdecontactos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etEmail: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etConfirmarContrasena: EditText
    private lateinit var btnRegistrarse: Button
    private lateinit var btnIrALogin: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etUsuario = findViewById(R.id.etRegisterUsuario)
        etEmail = findViewById(R.id.etRegisterEmail)
        etContrasena = findViewById(R.id.etRegisterContrasena)
        etConfirmarContrasena = findViewById(R.id.etRegisterConfirmarContrasena)
        btnRegistrarse = findViewById(R.id.btnRegistrarse)
        btnIrALogin = findViewById(R.id.btnIrALogin)

        btnRegistrarse.setOnClickListener {
            registrarUsuario()
        }

        btnIrALogin.setOnClickListener {
            finish()
        }
    }

    private fun registrarUsuario() {

        val usuario = etUsuario.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val contrasena = etContrasena.text.toString()
        val confirmar = etConfirmarContrasena.text.toString()

        if (usuario.isEmpty() || email.isEmpty() ||
            contrasena.isEmpty() || confirmar.isEmpty()
        ) {
            mostrarError("Completa todos los campos")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarError("Correo electrónico inválido")
            return
        }

        if (contrasena.length < 6) {
            mostrarError("La contraseña debe tener al menos 6 caracteres")
            return
        }

        if (contrasena != confirmar) {
            mostrarError("Las contraseñas no coinciden")
            return
        }

        btnRegistrarse.isEnabled = false

        // Etapa 1: Crear el usuario en Firebase Authentication
        auth.createUserWithEmailAndPassword(email, contrasena)
            .addOnSuccessListener { result ->

                val firebaseUser = result.user

                if (firebaseUser == null) {
                    btnRegistrarse.isEnabled = true
                    mostrarError("No se pudo obtener el usuario registrado")
                    return@addOnSuccessListener
                }

                val userData = hashMapOf(
                    "uid" to firebaseUser.uid,
                    "usuario" to usuario,
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                // Etapa 2: Guardar el perfil en Firestore
                db.collection("users")
                    .document(firebaseUser.uid)
                    .set(userData)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "¡Registro exitoso!",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this, NotesActivity::class.java)
                        intent.putExtra("EXTRA_USUARIO", usuario)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { error ->
                        btnRegistrarse.isEnabled = true

                        Log.e(
                            "REGISTRO_ERROR",
                            "Error en Etapa 2: Guardar perfil en Firestore",
                            error
                        )

                        mostrarDialogoError("Etapa 2: Guardar perfil en Firestore", error)
                    }
            }
            .addOnFailureListener { error ->
                btnRegistrarse.isEnabled = true

                Log.e(
                    "REGISTRO_ERROR",
                    "Error en Etapa 1: Crear usuario en Firebase Auth",
                    error
                )

                mostrarDialogoError("Etapa 1: Crear usuario en Firebase Auth", error)
            }
    }

    private fun mostrarDialogoError(etapa: String, exception: Exception) {
        val builder = StringBuilder()
        builder.append("Fallo en: ").append(etapa).append("\n\n")

        var current: Throwable? = exception
        var level = 1
        while (current != null) {
            builder.append("[$level] ${current.javaClass.simpleName}\n")
            builder.append("Mensaje: ${current.message ?: "Sin mensaje"}\n")
            if (current.localizedMessage != null && current.localizedMessage != current.message) {
                builder.append("Detalle: ${current.localizedMessage}\n")
            }
            builder.append("\n")
            current = current.cause
            level++
        }

        AlertDialog.Builder(this)
            .setTitle("Detalle del Error de Registro")
            .setMessage(builder.toString().trim())
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }
}
