package com.bartek.aplikacjainwentaryzacyjna

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MachineAdapter(
    private var machines: List<Machine>,
    private val presentMachinesIds: MutableSet<String>,
    private val onMachineClick: (Machine) -> Unit
) : RecyclerView.Adapter<MachineAdapter.MachineViewHolder>() {

    class MachineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.machine_name)
        val statusTextView: TextView = itemView.findViewById(R.id.machine_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MachineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_machine, parent, false)
        return MachineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MachineViewHolder, position: Int) {
        val machine = machines[position]
        holder.nameTextView.text = "${machine.id} - ${machine.name}"
        holder.statusTextView.text = "Status: ${machine.status}"

        // --- Tło natychmiast, bez animacji ---
        if (presentMachinesIds.contains(machine.id)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#A8E6A3")) // zielone
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE)
        }

        holder.itemView.setOnClickListener {
            onMachineClick(machine)
        }
    }

    override fun getItemCount(): Int = machines.size

    fun updateData(newMachines: List<Machine>) {
        machines = newMachines
        notifyDataSetChanged()
    }
}
