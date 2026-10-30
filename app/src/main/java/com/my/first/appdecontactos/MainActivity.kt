
package com.my.first.appdecontactos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnAceptar: Button
    private lateinit var btnIrARegistro: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

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

        etUsuario = findViewById(R.id.etUsuario)
        etContrasena = findViewById(R.id.etContrasena)
        btnAceptar = findViewById(R.id.btnAceptar)
        btnIrARegistro = findViewById(R.id.btnIrARegistro)

        btnAceptar.setOnClickListener {
            iniciarSesion()
        }

        btnIrARegistro.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    private fun iniciarSesion() {
        val email = etUsuario.text.toString().trim()
        val contrasena = etContrasena.text.toString()

        if (email.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(
                this,
                "Completa el correo y la contraseña.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnAceptar.isEnabled = false

        auth.signInWithEmailAndPassword(email, contrasena)
            .addOnSuccessListener { result ->

                val uid = result.user?.uid

                if (uid == null) {
                    btnAceptar.isEnabled = true
                    Toast.makeText(
                        this,
                        "No se pudo identificar la cuenta.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->

                        btnAceptar.isEnabled = true

                        if (!document.exists()) {
                            auth.signOut()
                            Toast.makeText(
                                this,
                                "No se encontró el perfil del usuario.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@addOnSuccessListener
                        }

                        val nombreUsuario =
                            document.getString("usuario") ?: email

                        Toast.makeText(
                            this,
                            "Inicio de sesión correcto.",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(
                            this,
                            NotesActivity::class.java
                        )

                        intent.putExtra(
                            "EXTRA_USUARIO",
                            nombreUsuario
                        )

                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        btnAceptar.isEnabled = true
                        auth.signOut()

                        Toast.makeText(
                            this,
                            "Error al cargar el perfil: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener {
                btnAceptar.isEnabled = true

                Toast.makeText(
                    this,
                    "Correo o contraseña incorrectos, o no se pudo conectar.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
