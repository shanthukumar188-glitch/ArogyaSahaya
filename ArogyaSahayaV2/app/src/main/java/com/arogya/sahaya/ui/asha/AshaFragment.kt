package com.arogya.sahaya.ui.asha

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arogya.sahaya.R
import com.arogya.sahaya.data.model.HealthEvent
import com.arogya.sahaya.databinding.FragmentAshaBinding
import com.arogya.sahaya.viewmodel.HealthEventViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AshaFragment : Fragment() {
    private var _b: FragmentAshaBinding? = null
    private val binding get() = _b!!
    private val vm: HealthEventViewModel by viewModels()
    private lateinit var adapter: HealthEventAdapter
    private var selectedDate = System.currentTimeMillis()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentAshaBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prePopulate()
        adapter = HealthEventAdapter(
            onDelete = { vm.deleteEvent(it) },
            onComplete = { e -> vm.updateEvent(e.copy(isCompleted = !e.isCompleted)) }
        )
        binding.recyclerEvents.apply {
            layoutManager = LinearLayoutManager(requireContext()); adapter = this@AshaFragment.adapter
        }
        vm.upcomingEvents.observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
            binding.tvEmpty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
            binding.tvCount.text = "${events.size} upcoming events"
        }
        binding.fabAddEvent.setOnClickListener { showAddDialog() }
    }

    private fun prePopulate() {
        fun future(days: Int) = System.currentTimeMillis() + days * 86400000L
        listOf(
            HealthEvent(title = "Free BP & Sugar Screening", location = "Gram Panchayat Hall", eventDate = future(2), eventTime = "9:00 AM", organizer = "Dr. Ramesh PHC", eventType = "HEALTH_CAMP"),
            HealthEvent(title = "ASHA Worker Home Visit", location = "Your Home", eventDate = future(1), eventTime = "3:00 PM", organizer = "ASHA Worker Kamla Devi", eventType = "ASHA_VISIT"),
            HealthEvent(title = "Free Eye Checkup Camp", location = "Community Centre", eventDate = future(5), eventTime = "10:00 AM", organizer = "Lions Club", eventType = "CHECKUP"),
            HealthEvent(title = "Vaccination Drive - COVID Booster", location = "Anganwadi Centre", eventDate = future(8), eventTime = "8:00 AM", eventType = "VACCINATION"),
            HealthEvent(title = "Mobile Health Van Visit", location = "Village Main Road", eventDate = future(3), eventTime = "11:00 AM", organizer = "District Health Dept", eventType = "HEALTH_CAMP")
        ).forEach { vm.addEvent(it) }
    }

    private fun showAddDialog() {
        val v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event, null)
        val etTitle = v.findViewById<EditText>(R.id.etTitle)
        val etLocation = v.findViewById<EditText>(R.id.etLocation)
        val etOrganizer = v.findViewById<EditText>(R.id.etOrganizer)
        val etTime = v.findViewById<EditText>(R.id.etTime)
        val tvDate = v.findViewById<TextView>(R.id.tvSelectedDate)
        val btnDate = v.findViewById<Button>(R.id.btnPickDate)
        val spinner = v.findViewById<Spinner>(R.id.spinnerType)
        val types = arrayOf("HEALTH_CAMP", "ASHA_VISIT", "VACCINATION", "CHECKUP")
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        btnDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d); selectedDate = cal.timeInMillis
                tvDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Add Health Event").setView(v)
            .setPositiveButton("Add") { _, _ ->
                if (etTitle.text.toString().isBlank()) return@setPositiveButton
                vm.addEvent(HealthEvent(title = etTitle.text.toString().trim(),
                    location = etLocation.text.toString().trim(), organizer = etOrganizer.text.toString().trim(),
                    eventDate = selectedDate, eventTime = etTime.text.toString().trim(),
                    eventType = types[spinner.selectedItemPosition]))
            }.setNegativeButton("Cancel", null).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

class HealthEventAdapter(
    private val onDelete: (HealthEvent) -> Unit,
    private val onComplete: (HealthEvent) -> Unit
) : ListAdapter<HealthEvent, HealthEventAdapter.VH>(DiffCB) {
    companion object DiffCB : DiffUtil.ItemCallback<HealthEvent>() {
        override fun areItemsTheSame(a: HealthEvent, b: HealthEvent) = a.id == b.id
        override fun areContentsTheSame(a: HealthEvent, b: HealthEvent) = a == b
    }
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvOrganizer: TextView = view.findViewById(R.id.tvOrganizer)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val btnComplete: ImageButton = view.findViewById(R.id.btnComplete)
    }
    override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_health_event, p, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = getItem(pos)
        h.tvType.text = when (e.eventType) { "HEALTH_CAMP" -> "🏥 Health Camp"; "ASHA_VISIT" -> "🤝 ASHA Visit"; "VACCINATION" -> "💉 Vaccination"; else -> "🔍 Checkup" }
        h.tvTitle.text = e.title; h.tvDate.text = "${SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(Date(e.eventDate))}  •  ${e.eventTime}"
        h.tvLocation.text = if (e.location.isNotBlank()) "📍 ${e.location}" else ""
        h.tvOrganizer.text = if (e.organizer.isNotBlank()) "👤 ${e.organizer}" else ""
        h.itemView.alpha = if (e.isCompleted) 0.5f else 1f
        h.btnDelete.setOnClickListener { onDelete(e) }
        h.btnComplete.setOnClickListener { onComplete(e) }
        h.btnComplete.setImageResource(if (e.isCompleted) R.drawable.ic_check_done else R.drawable.ic_check)
    }
}
