package com.arogya.sahaya.ui.pills

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
import androidx.recyclerview.widget.DividerItemDecoration
import com.arogya.sahaya.R
import com.arogya.sahaya.data.model.Pill
import com.arogya.sahaya.databinding.FragmentPillBinding
import com.arogya.sahaya.utils.AlarmScheduler
import com.arogya.sahaya.viewmodel.PillViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PillFragment : Fragment() {

    private var _binding: FragmentPillBinding? = null
    private val binding get() = _binding!!
    private val vm: PillViewModel by viewModels()
    private lateinit var adapter: PillAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentPillBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PillAdapter(
            onTaken = { pill, dose ->
                vm.markTaken(pill.id, dose)
                Snackbar.make(binding.root, "✅ ${pill.name} marked as taken", Snackbar.LENGTH_SHORT).show()
            },
            onSkip = { pill, dose -> vm.markSkipped(pill.id, dose) },
            onDelete = { pill ->
                AlarmScheduler.cancelPillAlarms(requireContext(), pill)
                vm.deletePill(pill)
                Snackbar.make(binding.root, "${pill.name} removed", Snackbar.LENGTH_SHORT).show()
            },
            onEdit = { pill -> showAddEditDialog(pill) }
        )

        binding.recyclerPills.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PillFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        vm.activePills.observe(viewLifecycleOwner) { pills ->
            adapter.submitList(pills)
            binding.tvEmpty.visibility = if (pills.isEmpty()) View.VISIBLE else View.GONE
            binding.tvPillCount.text = "${pills.size} active medicine(s)"
            val lowStock = pills.filter { it.stock <= it.refillAlert }
            if (lowStock.isNotEmpty()) {
                binding.cardStockAlert.visibility = View.VISIBLE
                binding.tvStockAlert.text = "⚠️ Low stock: ${lowStock.joinToString { it.name }}"
            } else binding.cardStockAlert.visibility = View.GONE
        }

        vm.toast.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        }

        binding.fabAddPill.setOnClickListener { showAddEditDialog(null) }
    }

    private fun showAddEditDialog(existing: Pill?) {
        val v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_pill, null)
        val etName = v.findViewById<EditText>(R.id.etPillName)
        val etDosage = v.findViewById<EditText>(R.id.etDosage)
        val etInstructions = v.findViewById<EditText>(R.id.etInstructions)
        val cbMorning = v.findViewById<CheckBox>(R.id.cbMorning)
        val cbAfternoon = v.findViewById<CheckBox>(R.id.cbAfternoon)
        val cbNight = v.findViewById<CheckBox>(R.id.cbNight)
        val etStock = v.findViewById<EditText>(R.id.etStock)
        val spinnerType = v.findViewById<Spinner>(R.id.spinnerPillType)

        val types = arrayOf("TABLET", "CAPSULE", "SYRUP", "INJECTION", "DROPS", "CREAM")
        spinnerType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)

        existing?.let {
            etName.setText(it.name); etDosage.setText(it.dosage)
            etInstructions.setText(it.instructions); etStock.setText(it.stock.toString())
            cbMorning.isChecked = it.doseTimesMorning; cbAfternoon.isChecked = it.doseTimesAfternoon
            cbNight.isChecked = it.doseTimesNight
            spinnerType.setSelection(types.indexOf(it.pillType).takeIf { i -> i >= 0 } ?: 0)
        }

        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle(if (existing == null) "➕ Add Medicine" else "✏️ Edit Medicine")
            .setView(v)
            .setPositiveButton("Save") { _, _ ->
                val nm = etName.text.toString().trim()
                if (nm.isEmpty()) { Toast.makeText(context, "Enter medicine name", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val pill = (existing ?: Pill(name = "", dosage = "")).copy(
                    name = nm, dosage = etDosage.text.toString().trim(),
                    instructions = etInstructions.text.toString().trim(),
                    pillType = types[spinnerType.selectedItemPosition],
                    doseTimesMorning = cbMorning.isChecked, doseTimesAfternoon = cbAfternoon.isChecked,
                    doseTimesNight = cbNight.isChecked,
                    stock = etStock.text.toString().toIntOrNull() ?: 30
                )
                if (existing == null) vm.addPill(pill) else vm.updatePill(pill)
                AlarmScheduler.schedulePillAlarms(requireContext(), pill)
            }
            .setNegativeButton("Cancel", null).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

class PillAdapter(
    private val onTaken: (Pill, String) -> Unit,
    private val onSkip: (Pill, String) -> Unit,
    private val onDelete: (Pill) -> Unit,
    private val onEdit: (Pill) -> Unit
) : ListAdapter<Pill, PillAdapter.VH>(DiffCB) {

    companion object DiffCB : DiffUtil.ItemCallback<Pill>() {
        override fun areItemsTheSame(a: Pill, b: Pill) = a.id == b.id
        override fun areContentsTheSame(a: Pill, b: Pill) = a == b
    }

    private val pillColors = listOf(0xFF1B5E20, 0xFF1565C0, 0xFF6A1B9A, 0xFFE65100, 0xFF880E4F, 0xFF004D40)

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPillName)
        val tvDosage: TextView = view.findViewById(R.id.tvDosage)
        val tvInstructions: TextView = view.findViewById(R.id.tvInstructions)
        val tvTimes: TextView = view.findViewById(R.id.tvTimes)
        val tvStock: TextView = view.findViewById(R.id.tvStock)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val pillColorBar: View = view.findViewById(R.id.pillColorBar)
        val btnMorning: Button = view.findViewById(R.id.btnMorning)
        val btnAfternoon: Button = view.findViewById(R.id.btnAfternoon)
        val btnNight: Button = view.findViewById(R.id.btnNight)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_pill, p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val pill = getItem(pos)
        val color = (pillColors[pill.colorIndex % pillColors.size]).toInt()
        h.pillColorBar.setBackgroundColor(color)
        h.tvName.text = pill.name
        h.tvDosage.text = pill.dosage
        h.tvInstructions.text = if (pill.instructions.isNotBlank()) "📋 ${pill.instructions}" else ""
        h.tvInstructions.visibility = if (pill.instructions.isNotBlank()) View.VISIBLE else View.GONE
        h.tvType.text = when (pill.pillType) { "TABLET" -> "💊 Tablet"; "CAPSULE" -> "💉 Capsule"; "SYRUP" -> "🧪 Syrup"; else -> pill.pillType }
        val times = buildString {
            if (pill.doseTimesMorning) append("🌅 Morning  ")
            if (pill.doseTimesAfternoon) append("☀️ Afternoon  ")
            if (pill.doseTimesNight) append("🌙 Night")
        }
        h.tvTimes.text = times.trim()
        val stockColor = if (pill.stock <= pill.refillAlert) 0xFFC62828.toInt() else 0xFF2E7D32.toInt()
        h.tvStock.text = "Stock: ${pill.stock} left"
        h.tvStock.setTextColor(stockColor)

        h.btnMorning.visibility = if (pill.doseTimesMorning) View.VISIBLE else View.GONE
        h.btnAfternoon.visibility = if (pill.doseTimesAfternoon) View.VISIBLE else View.GONE
        h.btnNight.visibility = if (pill.doseTimesNight) View.VISIBLE else View.GONE

        h.btnMorning.setOnClickListener { onTaken(pill, "MORNING") }
        h.btnAfternoon.setOnClickListener { onTaken(pill, "AFTERNOON") }
        h.btnNight.setOnClickListener { onTaken(pill, "NIGHT") }
        h.btnEdit.setOnClickListener { onEdit(pill) }
        h.btnDelete.setOnClickListener { onDelete(pill) }
    }
}
