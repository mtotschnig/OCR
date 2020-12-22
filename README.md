# OCR

This app bundles OCR functionality that can be called from other apps via Intents. Depending on the product flavor, it either uses [Mlkit](https://developers.google.com/ml-kit/vision/text-recognition/android) or [Tesseract](https://tesseract-ocr.github.io/).

It listens for Intents with action "org.totschnig.ocr.action.RECOGNIZE" and expects an Uri pointing to a JPEG file as data.

The recognized text is passed back in the extra "result" as an object of class [Text](https://github.com/mtotschnig/MyExpenses/blob/master/ocr/src/main/java/org/totschnig/ocr/Text.kt), that must be copied into the client app. 

See MyExpenses for an example:

Activity is started [here](https://github.com/mtotschnig/MyExpenses/blob/0d6e8c1aad8dc60444aa940d571233885b698cf2/ocr/src/main/java/org/totschnig/ocr/ScanPreviewViewModel.kt#L82).
The result is processed [here](https://github.com/mtotschnig/MyExpenses/blob/0d6e8c1aad8dc60444aa940d571233885b698cf2/ocr/src/main/java/org/totschnig/ocr/AbstractOcrFeatureImpl.kt#L63).
