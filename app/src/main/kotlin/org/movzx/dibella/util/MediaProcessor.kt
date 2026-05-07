package org.movzx.dibella.util

import android.media.MediaMetadataRetriever
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaProcessor @Inject constructor() {
    fun extractVideoFrame(videoFile: File, outputFile: File, quality: Int = 50): Boolean {
        val retriever = MediaMetadataRetriever()
        val startTime = System.currentTimeMillis()

        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val timeInUs = 2 * 1000000L

            val bitmap =
                retriever.getFrameAtTime(timeInUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            if (bitmap != null) {
                val success = FileUtils.saveBitmapAsWebP(bitmap, outputFile, quality)

                bitmap.recycle()

                if (success) {
                    Logger.d(
                        "Dibella_Codec",
                        "[${videoFile.name}] Frame extracted as WebP ($quality%) in ${System.currentTimeMillis() - startTime}ms",
                    )
                }

                success
            } else {
                Logger.e("Dibella_Codec", "[${videoFile.name}] Extraction failed: null bitmap")

                false
            }
        } catch (e: Exception) {
            Logger.e("Dibella_Codec", "[${videoFile.name}] Extraction Exception: ${e.message}")

            false
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Logger.w(
                    "Dibella_Codec",
                    "[${videoFile.name}] Failed to release MediaMetadataRetriever: ${e.message}",
                )
            }
        }
    }

    fun convertToWebP(inputFile: File, outputFile: File, quality: Int = 75): Boolean {
        return FileUtils.convertFileToWebP(inputFile, outputFile, quality)
    }
}
