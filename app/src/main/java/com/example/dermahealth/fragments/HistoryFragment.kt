package com.example.dermahealth.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dermahealth.R
import com.example.dermahealth.adapter.HistoryAdapter
import com.example.dermahealth.data.ScanHistory
import com.google.android.material.snackbar.Snackbar

class HistoryFragment : Fragment() {

    private lateinit var rvHistory: RecyclerView
    private val scanList = mutableListOf<ScanHistory>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rvHistory = view.findViewById(R.id.rv_history)

        // Dummy data
        scanList.add(ScanHistory(1, R.drawable.ic_launcher_foreground, "Benign", "Sep 24, 2025", "Looks fine, monitor"))
        scanList.add(ScanHistory(2, R.drawable.ic_launcher_foreground, "Suspicious", "Sep 20, 2025", "Visit dermatologist soon"))

        val adapter = HistoryAdapter(
            scans = scanList,
            onEdit = { scan ->
                Toast.makeText(requireContext(), "Edit notes: ${scan.result}", Toast.LENGTH_SHORT)
                    .show()
            },
            onDelete = { scan ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Scan")
                    .setMessage("Are you sure you want to delete this scan?")
                    .setPositiveButton("Yes") { _, _ ->
                        scanList.remove(scan)
                        rvHistory.adapter?.notifyDataSetChanged()
                        Snackbar.make(view, "Scan deleted", Snackbar.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )

        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = adapter
    }
}
