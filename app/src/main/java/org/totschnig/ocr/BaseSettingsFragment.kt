package org.totschnig.ocr

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

const val TEST_RC = 1
abstract class BaseSettingsFragment : PreferenceFragmentCompat() {
    lateinit var viewModel: OcrViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity()).get(OcrViewModel::class.java)
        viewModel.getResult().observe(this) { result ->
            result.onSuccess {
                AlertDialog.Builder(requireContext())
                    .setMessage(it.textBlocks.map { textBlock -> textBlock.lines.map { line -> line.text }.joinToString(separator = "\n") }.joinToString(separator = "\n"))
                    .create().show()
            }.onFailure {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
            }
        }
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = preferenceScreen.title ?: getString(R.string.app_name)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == TEST_RC && resultCode == Activity.RESULT_OK) {
            data?.data?.let { viewModel.runTextRecognition(it) }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.base_preferences, rootKey)
        if (rootKey == "credits") {
            addPreferencesFromResource(R.xml.flavor_credits)
        } else {
            findPreference<Preference>("test")?.setOnPreferenceClickListener {
                val gallIntent = Intent(Intent.ACTION_GET_CONTENT)
                gallIntent.type = "image/*"
                startActivityForResult(Intent.createChooser(gallIntent, null), TEST_RC)
                true
            }
        }
    }
}