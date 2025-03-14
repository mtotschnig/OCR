package org.totschnig.ocr

import android.content.Context
import android.os.Bundle
import androidx.preference.ListPreference
import java.util.Locale

class SettingsFragment: BaseSettingsFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        if (rootKey == null) {
            findPreference<ListPreference>(viewModel.prefKey)?.let { preference ->
                preference.entries =  getScriptArray()
            }
        }
    }
    fun getScriptArray() =
        resources.getStringArray(R.array.pref_mlkit_script_values)
            .map { getDisplayNameForScript(requireContext(), it) }
            .toTypedArray()

    fun getDisplayNameForScript(context: Context, script: String) =
        getDisplayNameForScript(resources.configuration.locale, script)

    fun getDisplayNameForScript(locale: Locale, script: String): String =
        when (script) {
            "Han" -> Locale.CHINESE.getDisplayLanguage(locale)
            else -> Locale.Builder().setScript(script).build().getDisplayScript(locale)
        }
}