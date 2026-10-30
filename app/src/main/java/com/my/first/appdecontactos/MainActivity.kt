package com.my.first.appdecontactos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnAceptar: Button
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var btnIrARegistro: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val idToken = account.idToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            } else {
                mostrarDialogoError(
                    "Google Sign-In",
                    "No se obtuvo el token ID de Google. Verifica que el Web Client ID esté bien configurado en Firebase."
                )
            }
        } catch (e: ApiException) {
            Log.e("GOOGLE_LOGIN", "Error en Google Sign-In: código ${e.statusCode}", e)
            val explicacion = when (e.statusCode) {
                10 -> "Código 10 (DEVELOPER_ERROR): Falta registrar la huella SHA-1 de tu PC/emulador en la consola de Firebase o el nombre de paquete no coincide."
                12500 -> "Código 12500: El proveedor de inicio de sesión con Google no está habilitado en Firebase Authentication."
                7 -> "Código 7 (NETWORK_ERROR): Sin conexión a Internet en el dispositivo."
                12501 -> "Se canceló la selección de cuenta de Google."
                else -> "Código de error de Google: ${e.statusCode}"
            }
            if (e.statusCode != 12501) {
                mostrarDialogoError("Google Sign-In (ApiException)", explicacion, e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay un usuario logueado en Firebase, mantener la sesión e ir a las notas
        val currentUser = auth.currentUser
        if (currentUser != null) {
            verificarYRedirigirUsuario(currentUser)
            return
        }

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

        // Configuración de Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        etUsuario = findViewById(R.id.etUsuario)
        etContrasena = findViewById(R.id.etContrasena)
        btnAceptar = findViewById(R.id.btnAceptar)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnIrARegistro = findViewById(R.id.btnIrARegistro)

        btnAceptar.setOnClickListener {
            iniciarSesion()
        }

        btnGoogleSignIn.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleLauncher.launch(signInIntent)
            }
        }

        btnIrARegistro.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    private fun verificarYRedirigirUsuario(user: FirebaseUser) {
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val nombreUsuario = if (document.exists()) {
                    document.getString("usuario") ?: user.displayName ?: user.email?.substringBefore("@") ?: "Usuario"
                } else {
                    user.displayName ?: user.email?.substringBefore("@") ?: "Usuario"
                }
                navegarANotas(nombreUsuario)
            }
            .addOnFailureListener {
                val fallback = user.displayName ?: user.email?.substringBefore("@") ?: "Usuario"
                navegarANotas(fallback)
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
                    mostrarError("No se pudo identificar la cuenta.")
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->

                        btnAceptar.isEnabled = true

                        if (!document.exists()) {
                            auth.signOut()
                            mostrarDialogoError(
                                "Inicio de Sesión",
                                "No se encontró el perfil del usuario en Firestore."
                            )
                            return@addOnSuccessListener
                        }

                        val nombreUsuario =
                            document.getString("usuario") ?: email

                        Toast.makeText(
                            this,
                            "Inicio de sesión correcto.",
                            Toast.LENGTH_SHORT
                        ).show()

                        navegarANotas(nombreUsuario)
                    }
                    .addOnFailureListener { error ->
                        btnAceptar.isEnabled = true

                        mostrarDialogoError(
                            "Cargar perfil desde Firestore",
                            error.message ?: error.toString(),
                            error
                        )
                    }
            }
            .addOnFailureListener { error ->
                btnAceptar.isEnabled = true

                mostrarDialogoError(
                    "Autenticación con correo y contraseña",
                    error.message ?: "Credenciales incorrectas o error de conexión",
                    error
                )
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    guardarONavegarUsuarioGoogle(user)
                } else {
                    mostrarDialogoError(
                        "Firebase Auth con Google",
                        "No se pudo obtener la información del usuario desde Firebase."
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("GOOGLE_LOGIN", "Error en Firebase Credential", e)
                mostrarDialogoError(
                    "Autenticar credencial de Google en Firebase Auth",
                    e.message ?: "Error desconocido",
                    e
                )
            }
    }

    private fun guardarONavegarUsuarioGoogle(user: FirebaseUser) {
        val userRef = db.collection("users").document(user.uid)

        userRef.get()
            .addOnSuccessListener { document ->
                val nombreUsuario = user.displayName ?: user.email?.substringBefore("@") ?: "Usuario"

                if (!document.exists()) {
                    val userData = hashMapOf(
                        "uid" to user.uid,
                        "usuario" to nombreUsuario,
                        "email" to (user.email ?: ""),
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    userRef.set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "¡Registro con Google exitoso!",
                                Toast.LENGTH_SHORT
                            ).show()
                            navegarANotas(nombreUsuario)
                        }
                        .addOnFailureListener { e ->
                            Log.e("GOOGLE_LOGIN", "Error guardando en Firestore", e)
                            mostrarDialogoError(
                                "Guardar nuevo perfil con Google en Firestore",
                                e.message ?: "Error al guardar perfil",
                                e
                            )
                        }
                } else {
                    val usernameFromDb = document.getString("usuario") ?: nombreUsuario
                    Toast.makeText(
                        this,
                        "¡Inicio de sesión correcto!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navegarANotas(usernameFromDb)
                }
            }
            .addOnFailureListener { e ->
                Log.e("GOOGLE_LOGIN", "Error consultando Firestore", e)
                mostrarDialogoError(
                    "Consultar perfil de Google en Firestore",
                    e.message ?: "Error consultando Firestore",
                    e
                )
            }
    }

    private fun navegarANotas(nombreUsuario: String) {
        val intent = Intent(this, NotesActivity::class.java)
        intent.putExtra("EXTRA_USUARIO", nombreUsuario)
        startActivity(intent)
        finish()
    }

    private fun mostrarDialogoError(etapa: String, mensaje: String, exception: Exception? = null) {
        val builder = StringBuilder()
        builder.append("ETAPA / ORIGEN: ").append(etapa).append("\n\n")
        builder.append("DESCRIPCIÓN:\n").append(mensaje).append("\n\n")

        if (exception != null) {
            builder.append("DETALLE TÉCNICO:\n")
            var current: Throwable? = exception
            var level = 1
            while (current != null) {
                builder.append("[$level] ${current.javaClass.simpleName}\n")
                builder.append("Mensaje: ${current.message ?: "Sin mensaje"}\n")
                current = current.cause
                level++
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Error en inicio de sesión")
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
