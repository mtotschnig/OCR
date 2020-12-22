package org.totschnig.ocr

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider


class OCR : ComponentActivity() {
    lateinit var viewModel: OcrViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(OcrViewModel::class.java)
        viewModel.getResult().observe(this) { result ->
            result.onSuccess {
                setResult(RESULT_OK, Intent().apply {
                    putExtra("result", it)
                })
                finish()
            }.onFailure {
                abort(it.message ?: "Failure")
            }
        }
        if (intent.action == "org.totschnig.ocr.action.RECOGNIZE") {
            setContentView(R.layout.activity_main)
            intent.data?.let { uri ->
                viewModel.runTextRecognition(uri)
            } ?: kotlin.run {
                abort("No uri to process provided")
            }
        } else {
            abort("OCR must be called with action org.totschnig.ocr.action.RECOGNIZE")
        }
    }

    private fun abort(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
        finish()
    }
}
