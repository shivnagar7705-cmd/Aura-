package com.example.aura.backend

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import java.io.File
import java.io.FileOutputStream

object ArtworkHelper {

    /**
     * Creates a high-resolution 512x512 cover artwork PNG with original ambient gradients & typography.
     */
    fun createCoverArtwork(
        context: Context,
        fileName: String,
        title: String,
        artist: String,
        gradientStartHex: String,
        gradientEndHex: String
    ): File {
        val imageDir = File(context.filesDir, "aura_cloud_storage/images")
        if (!imageDir.exists()) imageDir.mkdirs()
        val file = File(imageDir, fileName)
        if (file.exists() && file.length() > 500) {
            return file
        }

        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Gradient background
        val startColor = Color.parseColor(gradientStartHex)
        val endColor = Color.parseColor(gradientEndHex)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                startColor, endColor, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // Subtle glowing vinyl rings / aura resonance circles
        val ringPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(40, 255, 255, 255)
            isAntiAlias = true
        }
        for (radius in intArrayOf(80, 140, 200, 260)) {
            canvas.drawCircle(size / 2f, size / 2f, radius.toFloat(), ringPaint)
        }

        // Center stylized glowing node
        val glowPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.argb(120, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, 24f, glowPaint)

        // Title and artist text
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, size / 2f, size - 70f, titlePaint)

        val artistPaint = Paint().apply {
            color = Color.argb(190, 255, 255, 255)
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(artist, size / 2f, size - 36f, artistPaint)

        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
        }
        bitmap.recycle()

        return file
    }

    /**
     * Saves uploaded bytes from admin or user directly to Cloud Storage.
     */
    fun saveImageBytes(context: Context, fileName: String, bytes: ByteArray): File {
        val imageDir = File(context.filesDir, "aura_cloud_storage/images")
        if (!imageDir.exists()) imageDir.mkdirs()
        val file = File(imageDir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }
}
