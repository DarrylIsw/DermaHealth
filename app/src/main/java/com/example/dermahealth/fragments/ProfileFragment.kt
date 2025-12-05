package com.example.dermahealth.fragments

import android.app.AlertDialog
import android.content.Context
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
import androidx.fragment.app.activityViewModels
import com.example.dermahealth.LoginRegisterActivity
import com.example.dermahealth.R
import com.example.dermahealth.data.ScanHistory
import com.example.dermahealth.helper.BackHandler
import com.example.dermahealth.viewmodel.SharedViewModel
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment(), BackHandler {

    // --- Views ---
    private lateinit var imgAvatar: CircleImageView
    private lateinit var btnEditAvatar: View
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

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onBackPressed(): Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // --- Bind views ---
        imgAvatar = view.findViewById(R.id.img_avatar_main)
        btnEditAvatar = view.findViewById(R.id.btn_edit_avatar_main)
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
        tvOverallSkin = view.findViewById(R.id.tv_overall_skin_score)
        skinScoreProgress = view.findViewById(R.id.skin_score_progress)

        // Observe scan history
        sharedViewModel.history.observe(viewLifecycleOwner) { historyList ->
            updateOverallSkinScore(historyList)
        }

        loadUserData()
        setupListeners()
        observeSharedData()

        return view
    }

    private fun observeSharedData() {
        sharedViewModel.history.observe(viewLifecycleOwner) { list ->
            updateTotalScans(list)
            updateDetectedCategories(list)
            updateOverallSkinScore(list)
        }
    }

    private fun updateTotalScans(list: List<com.example.dermahealth.data.ScanHistory>) {
        val totalImages = list.sumOf { it.images.size }
        tvTotalScans.text = totalImages.toString()
    }

    private fun updateDetectedCategories(list: List<com.example.dermahealth.data.ScanHistory>) {
        var benign = 0
        var neutral = 0
        var suspicious = 0
        var malignant = 0

        list.forEach { card ->
            card.images.forEach { img ->
                when (img.label?.lowercase()) {
                    "benign" -> benign++
                    "neutral" -> neutral++
                    "suspicious" -> suspicious++
                    "malignant" -> malignant++
                }
            }
        }

        tvBenign.text = "Benign: $benign"
        tvNeutral.text = "Neutral: $neutral"
        tvSuspicious.text = "Suspicious: $suspicious"
        tvMalignant.text = "Malignant: $malignant"
    }

    private fun updateOverallSkinScore(historyList: List<ScanHistory>) {
        if (historyList.isEmpty()) {
            tvOverallSkin.text = "0%"
            skinScoreProgress.progress = 0
            return
        }

        // Compute average score per card
        val perCardAverages = historyList.map { scan ->
            val scores = scan.images.mapNotNull { it.score }
            if (scores.isNotEmpty()) {
                // Convert to percentage if your score is 0..1, otherwise keep as is
                val sum = scores.sum()
                val avg = sum / scores.size
                if (avg <= 1f) avg * 100f else avg
            } else 0f
        }

        // Overall skin score = sum of per-card averages / number of cards
        val overallScore = perCardAverages.sum() / perCardAverages.size

        // Update TextView and ProgressBar
        tvOverallSkin.text = String.format("%.0f%%", overallScore)
        skinScoreProgress.progress = overallScore.toInt().coerceIn(0, 100)
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
                clearUserData()
                startActivity(Intent(requireActivity(), LoginRegisterActivity::class.java))
                requireActivity().finish()
            }
        }

        btnDeleteAccount.setOnClickListener {
            showConfirmationDialog("Delete Account", "This action cannot be undone. Continue?") {
                clearUserData()
                startActivity(Intent(requireActivity(), LoginRegisterActivity::class.java))
                requireActivity().finish()
            }
        }

        btnEditAvatar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ChangeAvatarFragment())
                .addToBackStack(null)
                .commit()
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

    private fun clearUserData() {
        val sharedPref = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        loadUserData()
        imgAvatar.setImageResource(R.drawable.ic_person_grey)
    }

    private fun loadUserData() {
        val sharedPref = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        tvName.text = sharedPref.getString("name", "John Doe")
        tvEmail.text = sharedPref.getString("email", "john.doe@email.com")
        tvMobile.text = sharedPref.getString("phone", "+62 812-3456-7890")
        tvAgeValue.text = sharedPref.getString("age", "24")

        val avatarUri = sharedPref.getString("avatarUri", null)
        if (avatarUri != null) {
            imgAvatar.setImageURI(Uri.parse(avatarUri))
        } else {
            imgAvatar.setImageResource(R.drawable.ic_person_grey)
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }
}
