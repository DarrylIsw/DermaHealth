package com.example.dermahealth.helper

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class ClassificationResult(val label: String, val score: Float)

class TFLiteClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val labels = listOf("Benign", "Malignant")
    private val inputSize = 224

    init {
        interpreter = try {
            val fd = context.assets.openFd("model.tflite")
            val inputStream = FileInputStream(fd.fileDescriptor)
            val channel = inputStream.channel
            val buffer = channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            Interpreter(buffer)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (interpreter == null) Log.e("TFLiteClassifier", "Failed to load model!")
    }

    fun classify(bitmap: Bitmap): ClassificationResult {
        val tflite = interpreter ?: return ClassificationResult("Unknown", 0f)
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
            for (y in 0 until inputSize) for (x in 0 until inputSize) {
                val px = resized.getPixel(x, y)
                putFloat(((px shr 16) and 0xFF) / 255f)
                putFloat(((px shr 8) and 0xFF) / 255f)
                putFloat((px and 0xFF) / 255f)
            }
            rewind()
        }

        val output = TensorBuffer.createFixedSize(intArrayOf(1, 2), DataType.FLOAT32)
        tflite.run(input, output.buffer)
        val scores = output.floatArray
        val benign = scores[0]
        val malignant = scores[1]
        val label = if (benign > malignant) "Benign" else "Malignant"
        return ClassificationResult(label, maxOf(benign, malignant))
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
