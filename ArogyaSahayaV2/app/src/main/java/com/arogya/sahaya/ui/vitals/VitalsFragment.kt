package com.arogya.sahaya.ui.vitals

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arogya.sahaya.R
import com.arogya.sahaya.data.model.VitalEntry
import com.arogya.sahaya.databinding.FragmentVitalsBinding
import com.arogya.sahaya.viewmodel.VitalsViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class VitalsFragment : Fragment() {

    private var _binding: FragmentVitalsBinding? = null
    private val binding get() = _binding!!
    private val vm: VitalsViewModel by viewModels()
    private lateinit var adapter: VitalsAdapter
    private var currentChartMode = "BP"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentVitalsBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = VitalsAdapter(onDelete = { entry ->
            vm.deleteVital(entry)
            Snackbar.make(binding.root, "Entry deleted", Snackbar.LENGTH_SHORT).show()
        })
        binding.recyclerVitals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@VitalsFragment.adapter
        }

        vm.allVitals.observe(viewLifecycleOwner) { vitals ->
            adapter.submitList(vitals)
            binding.tvEmpty.visibility = if (vitals.isEmpty()) View.VISIBLE else View.GONE
        }

        vm.latestVital.observe(viewLifecycleOwner) { v ->
            if (v != null) {
                binding.tvBp.text = "${v.systolic}/${v.diastolic} mmHg"
                binding.tvHr.text = "${v.heartRate} bpm"
                binding.tvGlucose.text = "${v.glucoseLevel} mg/dL"
                binding.tvOxygen.text = "${v.oxygenLevel}% SpO2"
                val (bpStatus, bpColor) = vm.getBpStatus(v.systolic, v.diastolic)
                binding.tvBpStatus.text = bpStatus
                binding.tvBpStatus.setTextColor(bpColor)
                val (gStatus, gColor) = vm.getGlucoseStatus(v.glucoseLevel)
                binding.tvGlucoseStatus.text = gStatus
                binding.tvGlucoseStatus.setTextColor(gColor)
            } else {
                binding.tvBp.text = "--/-- mmHg"
                binding.tvHr.text = "-- bpm"
                binding.tvGlucose.text = "-- mg/dL"
                binding.tvOxygen.text = "-- SpO2"
                binding.tvBpStatus.text = "Tap + to log your first reading"
                binding.tvGlucoseStatus.text = ""
            }
        }

        vm.last7Days.observe(viewLifecycleOwner) { setupChart(it) }

        binding.fabAddVital.setOnClickListener { showAddVitalDialog() }
        binding.btnChartBP.setOnClickListener { updateChartMode("BP") }
        binding.btnChartHR.setOnClickListener { updateChartMode("HR") }
        binding.btnChartGlucose.setOnClickListener { updateChartMode("GLUCOSE") }
    }

    private fun updateChartMode(mode: String) {
        currentChartMode = mode
        vm.last7Days.value?.let { setupChart(it) }
        val active = ContextCompat.getColor(requireContext(), R.color.primary)
        val inactive = ContextCompat.getColor(requireContext(), R.color.text_hint)
        binding.btnChartBP.setTextColor(if (mode == "BP") active else inactive)
        binding.btnChartHR.setTextColor(if (mode == "HR") active else inactive)
        binding.btnChartGlucose.setTextColor(if (mode == "GLUCOSE") active else inactive)
    }

    private fun setupChart(vitals: List<VitalEntry>) {
        if (vitals.isEmpty()) return
        val entries = when (currentChartMode) {
            "HR"      -> vitals.mapIndexed { i, v -> Entry(i.toFloat(), v.heartRate.toFloat()) }
            "GLUCOSE" -> vitals.mapIndexed { i, v -> Entry(i.toFloat(), v.glucoseLevel) }
            else      -> vitals.mapIndexed { i, v -> Entry(i.toFloat(), v.systolic.toFloat()) }
        }
        val label = when (currentChartMode) { "HR" -> "Heart Rate (bpm)"; "GLUCOSE" -> "Glucose (mg/dL)"; else -> "Systolic BP (mmHg)" }
        val lineColor = when (currentChartMode) {
            "HR"      -> Color.parseColor("#2E7D32")
            "GLUCOSE" -> Color.parseColor("#FF8F00")
            else      -> Color.parseColor("#C62828")
        }
        val ds = LineDataSet(entries, label).apply {
            color = lineColor; lineWidth = 2.5f; circleRadius = 5f
            setCircleColor(lineColor); valueTextSize = 10f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            fillAlpha = 60; fillColor = lineColor; setDrawFilled(true)
        }
        binding.lineChart.apply {
            data = LineData(ds)
            description.text = ""
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            axisLeft.textColor = Color.DKGRAY
            axisRight.isEnabled = false
            legend.textSize = 12f
            setTouchEnabled(true)
            setPinchZoom(true)
            animateX(800)
            invalidate()
        }
    }

    private fun showAddVitalDialog() {
        val v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_vital, null)
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("📊 Log Today's Vitals")
            .setView(v)
            .setPositiveButton("Save") { _, _ ->
                val entry = VitalEntry(
                    systolic    = v.findViewById<EditText>(R.id.etSystolic).text.toString().toIntOrNull()   ?: 0,
                    diastolic   = v.findViewById<EditText>(R.id.etDiastolic).text.toString().toIntOrNull()  ?: 0,
                    heartRate   = v.findViewById<EditText>(R.id.etHeartRate).text.toString().toIntOrNull()  ?: 0,
                    glucoseLevel= v.findViewById<EditText>(R.id.etGlucose).text.toString().toFloatOrNull()  ?: 0f,
                    oxygenLevel = v.findViewById<EditText>(R.id.etOxygen).text.toString().toFloatOrNull()   ?: 0f,
                    temperature = v.findViewById<EditText>(R.id.etTemp).text.toString().toFloatOrNull()     ?: 0f,
                    notes       = v.findViewById<EditText>(R.id.etNotes).text.toString().trim()
                )
                vm.addVital(entry)
                Snackbar.make(binding.root, "✅ Vitals saved", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

class VitalsAdapter(private val onDelete: (VitalEntry) -> Unit) :
    ListAdapter<VitalEntry, VitalsAdapter.VH>(DiffCB) {
    companion object DiffCB : DiffUtil.ItemCallback<VitalEntry>() {
        override fun areItemsTheSame(a: VitalEntry, b: VitalEntry) = a.id == b.id
        override fun areContentsTheSame(a: VitalEntry, b: VitalEntry) = a == b
    }
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView    = view.findViewById(R.id.tvDate)
        val tvBp: TextView      = view.findViewById(R.id.tvBp)
        val tvHr: TextView      = view.findViewById(R.id.tvHr)
        val tvGlucose: TextView = view.findViewById(R.id.tvGlucose)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }
    override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_vital, p, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = getItem(pos)
        h.tvDate.text    = SimpleDateFormat("EEE dd MMM  hh:mm a", Locale.getDefault()).format(Date(e.recordedAt))
        h.tvBp.text      = "❤️ BP: ${e.systolic}/${e.diastolic} mmHg"
        h.tvHr.text      = "💓 HR: ${e.heartRate} bpm"
        h.tvGlucose.text = "🩸 Glucose: ${e.glucoseLevel} mg/dL"
        h.btnDelete.setOnClickListener { onDelete(e) }
    }
}
