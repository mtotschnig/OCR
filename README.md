# OCR

This app bundles OCR functionality from Google's [Mlkit](https://developers.google.com/ml-kit/vision/text-recognition/android).
At the moment it does not provide a UI on its own, but can be called by other apps.

It listens for Intents with action "org.totschnig.ocr.action.RECOGNIZE" and expects an Uri pointing to a JPEG file as data. The orientation of the image can be passed in with an Integer extra "orientation". Value must be either 0, 90, 180 or 270.

The recognized text is passed back in the extra "result" as an object of class [Text](https://github.com/mtotschnig/MyExpenses/blob/master/ocr/src/main/java/org/totschnig/ocr/Text.kt), that must be copied into the client app: 

See MyExpenses for an example:

Activity is started [here](https://github.com/mtotschnig/MyExpenses/blob/0d6e8c1aad8dc60444aa940d571233885b698cf2/ocr/src/main/java/org/totschnig/ocr/ScanPreviewViewModel.kt#L82).
The resuls is processed [here](https://github.com/mtotschnig/MyExpenses/blob/0d6e8c1aad8dc60444aa940d571233885b698cf2/ocr/src/main/java/org/totschnig/ocr/AbstractOcrFeatureImpl.kt#L63)
