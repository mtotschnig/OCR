package org.totschnig.ocr

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class OcrViewModel(application: Application) : BaseViewModel(application) {

    fun runTextRecognition(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }?.let {
                        InputImage.fromBitmap(it, getOrientation(uri))
                    }?.let {
                        TextRecognition.getClient().process(it)
                            .addOnSuccessListener { texts ->
                                result.postValue(Result.success(texts.wrap()))
                            }
                            .addOnFailureListener { e ->
                                Timber.e(e)
                                result.postValue(Result.failure(e))
                            }
                    } ?: run {
                    result.postValue(Result.failure(Exception("Unable to open " + uri)))
                }
            }
        }
    }
}

fun com.google.mlkit.vision.text.Text.wrap() = Text(textBlocks.map { textBlock ->
    TextBlock(textBlock.lines.map { line ->
        Line(line.text, line.boundingBox, line.elements.map { element ->
            Element(element.text, element.boundingBox)
        })
    })
})