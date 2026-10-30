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

class MainActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnAceptar: Button
    private lateinit var btnIrARegistro: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etUsuario = findViewById(R.id.etUsuario)
        etContrasena = findViewById(R.id.etContrasena)
        btnAceptar = findViewById(R.id.btnAceptar)
        btnIrARegistro = findViewById(R.id.btnIrARegistro)

        btnAceptar.setOnClickListener {
            val usuario = etUsuario.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()

            if (usuario.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_campos_vacios), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.inicio_sesion_correcto), Toast.LENGTH_SHORT).show()

                val intent = Intent(this, NotesActivity::class.java).apply {
                    putExtra("EXTRA_USUARIO", usuario)
                }
                startActivity(intent)
                finish()
            }
        }

        btnIrARegistro.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
