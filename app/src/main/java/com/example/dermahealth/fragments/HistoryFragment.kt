package com.example.dermahealth.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.dermahealth.R
import com.example.dermahealth.adapter.HistoryAdapter
import com.example.dermahealth.data.ScanHistory
import com.example.dermahealth.databinding.FragmentHistoryBinding
import com.example.dermahealth.ui.SwipeActionsCallback
import com.google.android.material.snackbar.Snackbar
import com.example.dermahealth.helper.BackHandler

class HistoryFragment : Fragment(), BackHandler {

    // Handle physical back press (returns false = not consumed)
    override fun onBackPressed(): Boolean = false

    // ViewBinding
    private var _b: FragmentHistoryBinding? = null
    private val b get() = _b!!

    // Adapter for scan history RecyclerView
    private val adapter by lazy {
        HistoryAdapter(
            onEdit = { scan ->
                // Show dialog to edit notes (currently simple display)
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.edit_notes))
                    .setMessage("Edit notes for: ${scan.result}\n\n${scan.notes}")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            },
            onDelete = { scan ->
                confirmDelete(scan) // Ask confirmation before deleting
            },
            onToggleExpand = { pos, expanded ->
                // Scroll to item when expanded
                if (expanded) {
                    b.rvHistory.post { b.rvHistory.smoothScrollToPosition(pos) }
                }
            }
        )
    }

    // Confirm deletion dialog
    private fun confirmDelete(scan: ScanHistory) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_scan_title)
            .setMessage(R.string.delete_scan_msg)
            .setPositiveButton(R.string.yes) { _, _ -> removeWithUndo(scan) }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    // Remove item with undo option
    private fun removeWithUndo(scan: ScanHistory) {
        val current = adapter.currentList.toMutableList()
        val idx = current.indexOfFirst { it.id == scan.id }
        if (idx == -1) return
        current.removeAt(idx)
        adapter.submitList(current)
        updateEmptyState()

        // Show Snackbar with Undo
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
        // Setup RecyclerView
        b.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        b.rvHistory.adapter = adapter
        b.rvHistory.setHasFixedSize(false) // items can change height
        (b.rvHistory.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        // Extra padding at bottom
        val extra = resources.getDimensionPixelSize(R.dimen.rv_extra_scroll)
        b.rvHistory.post {
            b.rvHistory.updatePadding(bottom = b.rvHistory.paddingBottom + extra)
        }

        // --- Swipe actions ---
        val swipe = SwipeActionsCallback(
            context = requireContext(),
            onRequestLeft = { pos, done ->   // DELETE
                val scan = adapter.currentList.getOrNull(pos) ?: return@SwipeActionsCallback done()
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.delete_scan_title)
                    .setMessage(R.string.delete_scan_msg)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        removeWithUndo(scan)
                        done()
                    }
                    .setNegativeButton(R.string.no) { _, _ -> done() }
                    .setOnDismissListener { done() }
                    .show()
            },
            onRequestRight = { pos, done ->  // EDIT
                val scan = adapter.currentList.getOrNull(pos) ?: return@SwipeActionsCallback done()
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.edit_notes))
                    .setMessage("Edit notes for: ${scan.result}\n\n${scan.notes}")
                    .setPositiveButton(android.R.string.ok) { _, _ -> done() }
                    .setOnDismissListener { done() }
                    .show()
            },
            swipeThreshold = 0.35f
        )
        ItemTouchHelper(swipe).attachToRecyclerView(b.rvHistory)

        // --- Seed dummy data ---
        val seed = listOf(
            ScanHistory(1, null, "Benign", "2025-09-24", "Looks fine, monitor", null),
            ScanHistory(2, null, "Suspicious", "2025-09-20", "Visit dermatologist soon", null),
            ScanHistory(3, null, "Malignant", "2025-09-18", "Seek consultation ASAP", null),
            ScanHistory(4, null, "Neutral", "2025-09-15", "No abnormality detected", null)
        )
        adapter.submitList(seed)
        updateEmptyState()
    }

    // Show/hide empty state views
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
