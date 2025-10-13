package com.example.dermahealth.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import com.example.dermahealth.R
import com.example.dermahealth.adapter.HistoryAdapter
import com.example.dermahealth.data.ScanHistory
import com.example.dermahealth.databinding.FragmentHistoryBinding
import com.google.android.material.snackbar.Snackbar

class HistoryFragment : Fragment() {

    private var _b: FragmentHistoryBinding? = null
    private val b get() = _b!!

    private val adapter by lazy {
        HistoryAdapter(
            onEdit = { scan ->
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.edit_notes))
                    .setMessage("Edit notes for: ${scan.result}")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            },
            onDelete = { scan ->
                confirmDelete(scan)
            }
        )
    }

    private fun confirmDelete(scan: ScanHistory) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_scan_title)
            .setMessage(R.string.delete_scan_msg)
            .setPositiveButton(R.string.yes) { _, _ -> removeWithUndo(scan) }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun removeWithUndo(scan: ScanHistory) {
        val current = adapter.currentList.toMutableList()
        val idx = current.indexOfFirst { it.id == scan.id }
        if (idx == -1) return
        current.removeAt(idx)
        adapter.submitList(current)
        updateEmptyState()

        Snackbar.make(b.root, getString(R.string.scan_deleted), Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                val restored = adapter.currentList.toMutableList()
                restored.add(idx.coerceIn(0, restored.size), scan)
                adapter.submitList(restored)
                updateEmptyState()
            }
            .show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentHistoryBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        b.rvHistory.adapter = adapter

        // swipe to delete
        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val scan = adapter.currentList[vh.bindingAdapterPosition]
                confirmDelete(scan)
                adapter.notifyItemChanged(vh.bindingAdapterPosition) // restore until confirmed
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(b.rvHistory)

        // seed dummy
        val seed = listOf(
            ScanHistory(1, null, "Benign", "2025-09-24", "Looks fine, monitor", null),
            ScanHistory(2, null, "Suspicious", "2025-09-20", "Visit dermatologist soon", null)
        )
        adapter.submitList(seed)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val isEmpty = adapter.itemCount == 0
        b.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvHistory.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}
