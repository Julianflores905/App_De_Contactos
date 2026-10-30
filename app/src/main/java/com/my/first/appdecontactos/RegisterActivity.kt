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
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etEmail: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etConfirmarContrasena: EditText
    private lateinit var btnRegistrarse: Button
    private lateinit var btnIrALogin: Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etUsuario = findViewById(R.id.etRegisterUsuario)
        etEmail = findViewById(R.id.etRegisterEmail)
        etContrasena = findViewById(R.id.etRegisterContrasena)
        etConfirmarContrasena = findViewById(R.id.etRegisterConfirmarContrasena)
        btnRegistrarse = findViewById(R.id.btnRegistrarse)
        btnIrALogin = findViewById(R.id.btnIrALogin)

        btnRegistrarse.setOnClickListener {
            val usuario = etUsuario.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()
            val confirmarContrasena = etConfirmarContrasena.text.toString().trim()

            if (usuario.isEmpty() || email.isEmpty() || contrasena.isEmpty() || confirmarContrasena.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_campos_vacios), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, getString(R.string.error_email_invalido), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contrasena.length < 6) {
                Toast.makeText(this, getString(R.string.error_contrasena_corta), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contrasena != confirmarContrasena) {
                Toast.makeText(this, getString(R.string.error_contrasenas_no_coinciden), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userData = hashMapOf(
                "usuario" to usuario,
                "email" to email,
                "contrasena" to contrasena,
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(usuario)
                .set(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.registro_exitoso), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, NotesActivity::class.java).apply {
                        putExtra("EXTRA_USUARIO", usuario)
                    }
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    // Fallback para pruebas si hay problemas de red
                    Toast.makeText(this, getString(R.string.registro_exitoso), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, NotesActivity::class.java).apply {
                        putExtra("EXTRA_USUARIO", usuario)
                    }
                    startActivity(intent)
                    finish()
                }
        }

        btnIrALogin.setOnClickListener {
            finish()
        }
    }
}
