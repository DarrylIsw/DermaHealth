package com.example.dermahealth.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.dermahealth.R
import com.example.dermahealth.adapter.HistoryAdapter
import com.example.dermahealth.data.ScanHistory
import com.example.dermahealth.databinding.FragmentHistoryBinding
import com.example.dermahealth.helper.BackHandler
import com.example.dermahealth.ui.SwipeActionsCallback
import com.example.dermahealth.viewmodel.SharedViewModel
import com.google.android.material.snackbar.Snackbar

class HistoryFragment : Fragment(), BackHandler {

    override fun onBackPressed(): Boolean = false

    private var _b: FragmentHistoryBinding? = null
    private val b get() = _b!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    // Prevent double-dialog spam on swipe
    private var swipeDialogShown = false

    private val adapter by lazy {
        HistoryAdapter(
            onEdit = { scan -> showEditDialog(scan) },
            onDelete = { scan -> confirmDelete(scan) },
            onToggleExpand = { pos, expanded ->
                if (expanded) {
                    b.rvHistory.post { b.rvHistory.smoothScrollToPosition(pos) }
                }
            }
        )
    }

    private fun showEditDialog(scan: ScanHistory) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit_notes))
            .setMessage("Edit notes for: ${scan.mainImage?.label ?: "Unknown"}\n\n${scan.notes}")
            .setPositiveButton(android.R.string.ok, null)
            .create()

        dialog.setOnShowListener {
            val color = resources.getColor(R.color.medium_sky_blue, requireContext().theme)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
        }

        dialog.show()
    }

    private fun confirmDelete(scan: ScanHistory) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_scan_title)
            .setMessage(R.string.delete_scan_msg)
            .setPositiveButton(R.string.yes) { _, _ -> removeWithUndo(scan) }
            .setNegativeButton(R.string.no, null)
            .create()

        dialog.setOnShowListener {
            val color = resources.getColor(R.color.medium_sky_blue, requireContext().theme)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(color)
        }

        dialog.show()
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

        // RecyclerView setup
        b.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        b.rvHistory.adapter = adapter
        b.rvHistory.setHasFixedSize(false)
        (b.rvHistory.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        // Extra bottom padding
        val extra = resources.getDimensionPixelSize(R.dimen.rv_extra_scroll)
        b.rvHistory.post {
            b.rvHistory.updatePadding(bottom = b.rvHistory.paddingBottom + extra)
        }

        // Swipe actions
        val swipe = SwipeActionsCallback(
            context = requireContext(),
            onRequestLeft = { pos, done ->
                val scan = adapter.currentList.getOrNull(pos) ?: return@SwipeActionsCallback done()

                if (!swipeDialogShown) {
                    swipeDialogShown = true
                    val dialog = AlertDialog.Builder(requireContext())
                        .setTitle(R.string.delete_scan_title)
                        .setMessage(R.string.delete_scan_msg)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            removeWithUndo(scan)
                            swipeDialogShown = false
                            done()
                        }
                        .setNegativeButton(R.string.no) { _, _ ->
                            swipeDialogShown = false
                            done()
                        }
                        .setOnCancelListener {
                            swipeDialogShown = false
                            done()
                        }
                        .create()

                    dialog.setOnShowListener {
                        val color = resources.getColor(R.color.medium_sky_blue, requireContext().theme)
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(color)
                    }

                    dialog.show()
                } else {
                    done()
                }
            },

            onRequestRight = { pos, done ->
                val scan = adapter.currentList.getOrNull(pos) ?: return@SwipeActionsCallback done()

                if (!swipeDialogShown) {
                    swipeDialogShown = true

                    val dialog = AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.edit_notes))
                        .setMessage("Edit notes for: ${scan.mainImage?.label ?: "Unknown"}\n\n${scan.notes}")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            swipeDialogShown = false
                            done()
                        }
                        .setOnCancelListener {
                            swipeDialogShown = false
                            done()
                        }
                        .create()

                    dialog.setOnShowListener {
                        val color = resources.getColor(R.color.medium_sky_blue, requireContext().theme)
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
                    }

                    dialog.show()
                } else {
                    done()
                }
            },

            swipeThreshold = 0.35f
        )

        ItemTouchHelper(swipe).attachToRecyclerView(b.rvHistory)

        // Observe ViewModel — replaces seed list
        sharedViewModel.history.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            updateEmptyState()
        }
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
