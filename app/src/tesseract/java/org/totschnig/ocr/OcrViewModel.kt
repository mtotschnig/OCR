package org.totschnig.ocr

import Catalano.Imaging.FastBitmap
import Catalano.Imaging.Filters.BradleyLocalThreshold
import Catalano.Imaging.IApplyInPlace
import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

const val TESSERACT_DOWNLOAD_FOLDER = "tesseract4/fast/"

class OcrViewModel(application: Application) : AndroidViewModel(application) {
    private val result = MutableLiveData<Result<Text>>()

    val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(getApplication())

    private fun initialize() {
        System.loadLibrary("jpeg")
        System.loadLibrary("png")
        System.loadLibrary("leptonica")
        System.loadLibrary("tesseract")
    }

    fun getResult(): LiveData<Result<Text>> = result

    fun runTextRecognition(uri: Uri, orientation: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    initialize()
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.use { inputStream ->
                            BitmapFactory.decodeStream(
                                inputStream,
                                null,
                                BitmapFactory.Options().apply {
                                    inMutable = true
                                    inPreferredConfig = Bitmap.Config.ARGB_8888
                                })
                        }?.let {
                            with(TessBaseAPI()) {
                                if (!init(
                                        File(
                                            getApplication<Application>().getExternalFilesDir(null),
                                            TESSERACT_DOWNLOAD_FOLDER
                                        ).path, language()
                                    )
                                ) {
                                    throw IllegalStateException("Could not init Tesseract")
                                }
                                setVariable("tessedit_do_invert", TessBaseAPI.VAR_FALSE)
                                setVariable("load_system_dawg", TessBaseAPI.VAR_FALSE)
                                setVariable("load_freq_dawg", TessBaseAPI.VAR_FALSE)
                                setVariable("load_punc_dawg", TessBaseAPI.VAR_FALSE)
                                setVariable("load_number_dawg", TessBaseAPI.VAR_FALSE)
                                setVariable("load_unambig_dawg", TessBaseAPI.VAR_FALSE)
                                setVariable("load_bigram_dawg", TessBaseAPI.VAR_FALSE)
                                setVariable("load_fixed_length_dawgs", TessBaseAPI.VAR_FALSE)
                                pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD
                                setImage(with(FastBitmap(it)) {
                                    toGrayscale()
                                    val g: IApplyInPlace = BradleyLocalThreshold()
                                    g.applyInPlace(this)
                                    toBitmap()
                                })
                                utF8Text
                                val lines = mutableListOf<Line>()
                                with(resultIterator) {
                                    begin()
                                    do {
                                        val lineText =
                                            getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                                        val lineBoundingRect =
                                            getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                                        val elements = mutableListOf<Element>()
                                        do {
                                            val wordText =
                                                getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                                            val wordBoundingRect =
                                                getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                                            elements.add(Element(wordText, wordBoundingRect))
                                        } while (!isAtFinalElement(
                                                TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE,
                                                TessBaseAPI.PageIteratorLevel.RIL_WORD
                                            ) && next(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                                        )
                                        lines.add(Line(lineText, lineBoundingRect, elements))
                                    } while (next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE))
                                    delete()
                                    end()
                                    result.postValue(Result.success(Text(listOf(TextBlock(lines)))))
                                }
                            }
                        } ?: run {
                        result.postValue(Result.failure(Exception("Unable to open $uri")))
                    }
                } catch (e: Exception) {
                    result.postValue(Result.failure(e))
                }
            }
        }
    }

    private fun filePath(language: String) =
        "${TESSERACT_DOWNLOAD_FOLDER}tessdata/${language}.traineddata"

    private fun fileName(language: String) = "${language}.traineddata"

    fun tessDataExists(context: Context): Boolean = language()?.let {
        File(context.getExternalFilesDir(null), filePath(it)).exists()
    } ?: false

    private fun language(): String? {
        return preferences.getString("tesseract_language", null)
    }

    fun downloadTessData(context: Context) = language()?.let {
        val uri =
            Uri.parse("https://github.com/tesseract-ocr/tessdata_fast/raw/4.0.0/${fileName(it)}")
        ContextCompat.getSystemService(context, DownloadManager::class.java)?.enqueue(
            DownloadManager.Request(uri)
                .setTitle(context.getString(R.string.pref_tesseract_language_title))
                .setDescription(it)
                .setDestinationInExternalFilesDir(context, null, filePath(it))
        )
        getTesseractLanguageDisplayName(context, it)
    }

    fun getLanguageArray(context: Context) =
        context.resources.getStringArray(R.array.pref_tesseract_language_values)
            .map { getTesseractLanguageDisplayName(context, it) }
            .toTypedArray()

    private fun getTesseractLanguageDisplayName(context: Context, localeString: String): String {
        val localeParts = localeString.split("_")
        val lang = when (localeParts[0]) {
            "kmr" -> "kur"
            else -> localeParts[0]
        }
        val localeFromContext = context.resources.getConfiguration().locale
        return if (localeParts.size == 2) {
            val script = when (localeParts[1]) {
                "sim" -> "Hans"
                "tra" -> "Hant"
                else -> localeParts[1]
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Locale.Builder().setLanguage(lang).setScript(script).build().getDisplayName(
                    localeFromContext
                )
            } else {
                "${Locale(lang).getDisplayName(localeFromContext)} ($script)"
            }
        } else
            Locale(lang).getDisplayName(localeFromContext)
    }
}