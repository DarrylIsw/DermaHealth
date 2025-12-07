package com.example.dermahealth.fragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.dermahealth.LoginRegisterActivity
import com.example.dermahealth.R
import com.example.dermahealth.helper.BackHandler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment(), BackHandler {

    // Views
    private lateinit var imgAvatar: CircleImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvMobile: TextView
    private lateinit var tvAgeValue: TextView
    private lateinit var tvTotalScans: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var tvOverallSkin: TextView
    private lateinit var tvBenign: TextView
    private lateinit var tvNeutral: TextView
    private lateinit var tvSuspicious: TextView
    private lateinit var tvMalignant: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var btnDeleteAccount: Button
    private lateinit var skinScoreProgress: ProgressBar

    // New Badge
    private lateinit var tvHealthBadge: TextView

    override fun onBackPressed(): Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Bind views
        imgAvatar = view.findViewById(R.id.img_avatar_main)
        tvName = view.findViewById(R.id.tv_name)
        tvEmail = view.findViewById(R.id.tv_email)
        tvMobile = view.findViewById(R.id.tv_mobile)
        tvAgeValue = view.findViewById(R.id.tv_age_value)
        tvTotalScans = view.findViewById(R.id.tv_total_scans)
        tvMemberSince = view.findViewById(R.id.tv_member_since_date)
        tvOverallSkin = view.findViewById(R.id.tv_overall_skin_score)
        tvBenign = view.findViewById(R.id.tv_benign_count)
        tvNeutral = view.findViewById(R.id.tv_neutral_count)
        tvSuspicious = view.findViewById(R.id.tv_suspicious_count)
        tvMalignant = view.findViewById(R.id.tv_malignant_count)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnDeleteAccount = view.findViewById(R.id.btn_delete_account)
        skinScoreProgress = view.findViewById(R.id.skin_score_progress)

        // NEW: Health Level Badge
        tvHealthBadge = view.findViewById(R.id.tv_health_badge)

        loadUserProfile()
        setupListeners()

        return view
    }

    private fun loadUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid
        val db = FirebaseFirestore.getInstance()

        // --- MEMBER SINCE ---
        val createdAt = currentUser.metadata?.creationTimestamp ?: 0L
        if (createdAt > 0) {
            val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(Date(createdAt))
            tvMemberSince.text = date
        }

        // --- Statistics Listener ---
        db.collection("statistics").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) return@addSnapshotListener
                if (doc != null && doc.exists()) {

                    tvTotalScans.text = (doc.getLong("totalScans") ?: 0).toString()
                    tvBenign.text = "Benign: ${doc.getLong("benignCount") ?: 0}"
                    tvNeutral.text = "Neutral: ${doc.getLong("neutralCount") ?: 0}"
                    tvSuspicious.text = "Suspicious: ${doc.getLong("suspiciousCount") ?: 0}"
                    tvMalignant.text = "Malignant: ${doc.getLong("malignantCount") ?: 0}"

                    val overallScore = (doc.getDouble("overallSkinScore") ?: 0.0).toInt()
                    tvOverallSkin.text = "$overallScore%"
                    skinScoreProgress.progress = overallScore.coerceIn(0, 100)

                    // --- NEW: Update Badge ---
                    val level = getHealthLevel(overallScore)
                    tvHealthBadge.text = level
                    tvHealthBadge.setTextColor(getHealthColor(level))
                }
            }
    }

    // LEVEL LOGIC
    private fun getHealthLevel(score: Int): String {
        return when {
            score >= 85 -> "Healthy"
            score >= 60 -> "Average"
            score >= 40 -> "At Risk"
            else -> "High Risk"
        }
    }

    // OPTIONAL COLOR LOGIC
    private fun getHealthColor(level: String): Int {
        return when (level) {
            "Healthy" -> resources.getColor(R.color.green, null)
            "Average" -> resources.getColor(R.color.yellow, null)
            "At Risk" -> resources.getColor(R.color.orange, null)
            else -> resources.getColor(R.color.red, null)
        }
    }

    private fun setupListeners() {
        btnEditProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        btnLogout.setOnClickListener {
            showConfirmationDialog("Logout", "Are you sure you want to log out?") {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(requireActivity(), LoginRegisterActivity::class.java))
                requireActivity().finish()
            }
        }

        btnDeleteAccount.setOnClickListener {
            showConfirmationDialog("Delete Account", "This action cannot be undone. Continue?") {
                FirebaseAuth.getInstance().currentUser?.delete()
                startActivity(Intent(requireActivity(), LoginRegisterActivity::class.java))
                requireActivity().finish()
            }
        }
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val color = resources.getColor(R.color.medium_sky_blue, requireContext().theme)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(color)
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }
}
