package com.my.first.appdecontactos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CallsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var etPhoneNumber: EditText
    private lateinit var btnBackspace: ImageButton
    private lateinit var btnMakeCall: Button
    private lateinit var rvCallHistory: RecyclerView
    private lateinit var tvEmptyCalls: TextView

    private lateinit var adapter: CallsAdapter
    private val callsList = mutableListOf<CallRecord>()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var uid: String = ""
    private var pendingPhoneNumber: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            if (pendingPhoneNumber.isNotEmpty()) {
                makeDirectCall(pendingPhoneNumber)
            }
        } else {
            Toast.makeText(
                this,
                "Permiso denegado. Abriendo marcador predeterminado...",
                Toast.LENGTH_SHORT
            ).show()
            if (pendingPhoneNumber.isNotEmpty()) {
                openDialer(pendingPhoneNumber)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calls)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.mainCalls)
        ) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
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
            finish()
            return
        }
        uid = currentUser.uid

        btnBack = findViewById(R.id.btnBack)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnBackspace = findViewById(R.id.btnBackspace)
        btnMakeCall = findViewById(R.id.btnMakeCall)
        rvCallHistory = findViewById(R.id.rvCallHistory)
        tvEmptyCalls = findViewById(R.id.tvEmptyCalls)

        btnBack.setOnClickListener {
            finish()
        }

        setupKeypad()

        btnBackspace.setOnClickListener {
            val text = etPhoneNumber.text.toString()
            if (text.isNotEmpty()) {
                etPhoneNumber.setText(text.substring(0, text.length - 1))
                etPhoneNumber.setSelection(etPhoneNumber.text.length)
            }
        }

        btnBackspace.setOnLongClickListener {
            etPhoneNumber.setText("")
            true
        }

        btnMakeCall.setOnClickListener {
            val number = etPhoneNumber.text.toString().trim()
            if (number.isEmpty()) {
                Toast.makeText(this, "Ingresa un número de teléfono", Toast.LENGTH_SHORT).show()
            } else {
                initiateCallProcess(number)
            }
        }

        adapter = CallsAdapter(
            callsList,
            onCallClick = { record ->
                etPhoneNumber.setText(record.phoneNumber)
                initiateCallProcess(record.phoneNumber)
            },
            onDeleteClick = { record ->
                deleteCallRecord(record)
            }
        )

        rvCallHistory.layoutManager = LinearLayoutManager(this)
        rvCallHistory.adapter = adapter

        listenToCallHistory()
    }

    private fun setupKeypad() {
        val keysMap = mapOf(
            R.id.btnKey0 to "0",
            R.id.btnKey1 to "1",
            R.id.btnKey2 to "2",
            R.id.btnKey3 to "3",
            R.id.btnKey4 to "4",
            R.id.btnKey5 to "5",
            R.id.btnKey6 to "6",
            R.id.btnKey7 to "7",
            R.id.btnKey8 to "8",
            R.id.btnKey9 to "9",
            R.id.btnKeyStar to "*",
            R.id.btnKeyHash to "#"
        )

        for ((btnId, digit) in keysMap) {
            findViewById<Button>(btnId)?.setOnClickListener {
                appendDigit(digit)
            }
        }

        findViewById<Button>(R.id.btnKey0)?.setOnLongClickListener {
            appendDigit("+")
            true
        }
    }

    private fun appendDigit(digit: String) {
        val currentText = etPhoneNumber.text.toString()
        etPhoneNumber.setText(currentText + digit)
        etPhoneNumber.setSelection(etPhoneNumber.text.length)
    }

    private fun initiateCallProcess(phoneNumber: String) {
        pendingPhoneNumber = phoneNumber
        saveCallRecord(phoneNumber)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            makeDirectCall(phoneNumber)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun makeDirectCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        } catch (_: Exception) {
            openDialer(phoneNumber)
        }
    }

    private fun openDialer(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "No se pudo abrir el marcador", Toast.LENGTH_SHORT).show()
        }
    }

    private fun callsCollection() =
        db.collection("users")
            .document(uid)
            .collection("calls")

    private fun saveCallRecord(phoneNumber: String) {
        val recordData = hashMapOf(
            "phoneNumber" to phoneNumber,
            "contactName" to "",
            "timestamp" to System.currentTimeMillis()
        )

        callsCollection().add(recordData)
    }

    private fun listenToCallHistory() {
        callsCollection()
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    callsList.clear()
                    for (doc in snapshot.documents) {
                        callsList.add(
                            CallRecord(
                                id = doc.id,
                                phoneNumber = doc.getString("phoneNumber") ?: "",
                                contactName = doc.getString("contactName") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            )
                        )
                    }

                    adapter.updateCalls(callsList)
                    updateEmptyState()
                }
            }
    }

    private fun deleteCallRecord(record: CallRecord) {
        if (record.id.isEmpty()) return

        callsCollection()
            .document(record.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Registro de llamada eliminado.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyState() {
        if (callsList.isEmpty()) {
            tvEmptyCalls.visibility = View.VISIBLE
            rvCallHistory.visibility = View.GONE
        } else {
            tvEmptyCalls.visibility = View.GONE
            rvCallHistory.visibility = View.VISIBLE
        }
    }
}
