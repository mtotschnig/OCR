package org.totschnig.ocr

import android.app.Application
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class OcrViewModel(application: Application) : BaseViewModel(application) {

    val prefKey = application.getString(R.string.pref_mlkit_script_key)

    val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(getApplication())

    val script: String?
        get() = preferences.getString(prefKey, null)

    val options: TextRecognizerOptionsInterface
        get() = when(script) {
            "Han" -> ChineseTextRecognizerOptions.Builder().build()
            "Deva" -> DevanagariTextRecognizerOptions.Builder().build()
            "Jpan" -> JapaneseTextRecognizerOptions.Builder().build()
            "Kore" -> KoreanTextRecognizerOptions.Builder().build()
            else -> TextRecognizerOptions.DEFAULT_OPTIONS
        }

    fun runTextRecognition(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }?.let {
                        InputImage.fromBitmap(it, getOrientation(uri))
                    }?.let {
                        TextRecognition.getClient(options).process(it)
                            .addOnSuccessListener { texts ->
                                result.postValue(Result.success(texts.wrap()))
                            }
                            .addOnFailureListener { e ->
                                Timber.e(e)
                                result.postValue(Result.failure(e))
                            }
                    } ?: run {
                    result.postValue(Result.failure(Exception("Unable to open $uri")))
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