package com.my.first.appdecontactos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallsAdapter(
    private var calls: List<CallRecord>,
    private val onCallClick: (CallRecord) -> Unit,
    private val onDeleteClick: (CallRecord) -> Unit
) : RecyclerView.Adapter<CallsAdapter.CallViewHolder>() {

    class CallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNumber: TextView = itemView.findViewById(R.id.tvCallNumber)
        val tvDate: TextView = itemView.findViewById(R.id.tvCallDate)
        val btnCall: ImageButton = itemView.findViewById(R.id.btnCallItem)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteCall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call, parent, false)
        return CallViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        val record = calls[position]

        val displayText = if (record.contactName.isNotEmpty()) {
            "${record.contactName} (${record.phoneNumber})"
        } else {
            record.phoneNumber
        }

        holder.tvNumber.text = displayText

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvDate.text = sdf.format(Date(record.timestamp))

        holder.btnCall.setOnClickListener {
            onCallClick(record)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(record)
        }
    }

    override fun getItemCount(): Int = calls.size

    fun updateCalls(newCalls: List<CallRecord>) {
        calls = newCalls.toList()
        notifyDataSetChanged()
    }
}
