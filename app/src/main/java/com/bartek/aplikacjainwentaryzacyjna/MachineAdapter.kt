package com.bartek.aplikacjainwentaryzacyjna

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MachineAdapter(
    private var machines: List<Machine>,
    private val presentMachinesIds: Set<String>,
    private val onMachineClick: (Machine) -> Unit
) : RecyclerView.Adapter<MachineAdapter.MachineViewHolder>() {

    inner class MachineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val idTextView: TextView = itemView.findViewById(R.id.machine_id)
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
        holder.idTextView.text = machine.id
        holder.nameTextView.text = machine.name
        holder.statusTextView.text = machine.status

        // Kolorowanie tła: zielone jeśli maszyna jest obecna
        holder.itemView.setBackgroundColor(
            if (presentMachinesIds.contains(machine.id)) Color.parseColor("#A8E6A3") // jasna zieleń
            else Color.WHITE
        )

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
