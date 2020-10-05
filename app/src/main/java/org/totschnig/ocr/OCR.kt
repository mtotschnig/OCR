package org.totschnig.ocr

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import timber.log.Timber

class OCR : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == "org.totschnig.ocr.action.RECOGNIZE") {
            setContentView(R.layout.activity_main)
            intent.data?.let { uri ->
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }?.let {
                    InputImage.fromBitmap(it, intent.getIntExtra("orientation", 0))
                }
            }?.let {
                TextRecognition.getClient().process(it)
                    .addOnSuccessListener { texts ->
                        setResult(RESULT_OK, Intent().apply {
                            putExtra("result", texts.wrap())
                        })
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e)
                        abort(e.message ?: "Failure")
                    }
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


fun com.google.mlkit.vision.text.Text.wrap() = Text(textBlocks.map { textBlock ->
    TextBlock(textBlock.lines.map { line ->
        Line(line.text, line.boundingBox, line.elements.map { element ->
            Element(element.text, element.boundingBox)
        })
    })
})
