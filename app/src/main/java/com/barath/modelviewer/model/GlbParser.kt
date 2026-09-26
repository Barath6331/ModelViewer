package com.barath.modelviewer.model

import android.content.Context
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ModelInfo(
    val id: String,
    val displayName: String,
    val assetPath: String,
    val iconResName: String? = null
)

data class PartLabel(
    val nodeName: String,
    val labelText: String,
    val initialTranslation: FloatArray? = null
)

object GlbParser {

    fun parsePartLabels(inputStream: InputStream): Map<String, String> {
        val labels = mutableMapOf<String, String>()
        try {
            val headerBytes = ByteArray(12)
            var bytesRead = 0
            while (bytesRead < 12) {
                val r = inputStream.read(headerBytes, bytesRead, 12 - bytesRead)
                if (r == -1) return labels
                bytesRead += r
            }

            val headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = headerBuffer.int
            val version = headerBuffer.int
            val totalLength = headerBuffer.int

            // Chunk header: length (4 bytes), type (4 bytes)
            val chunkHeaderBytes = ByteArray(8)
            bytesRead = 0
            while (bytesRead < 8) {
                val r = inputStream.read(chunkHeaderBytes, bytesRead, 8 - bytesRead)
                if (r == -1) return labels
                bytesRead += r
            }

            val chunkHeaderBuffer = ByteBuffer.wrap(chunkHeaderBytes).order(ByteOrder.LITTLE_ENDIAN)
            val chunkLength = chunkHeaderBuffer.int
            val chunkType = chunkHeaderBuffer.int

            val jsonBytes = ByteArray(chunkLength)
            bytesRead = 0
            while (bytesRead < chunkLength) {
                val r = inputStream.read(jsonBytes, bytesRead, chunkLength - bytesRead)
                if (r == -1) break
                bytesRead += r
            }

            val jsonString = String(jsonBytes, Charsets.UTF_8)
            val jsonObject = JSONObject(jsonString)

            if (jsonObject.has("nodes")) {
                val nodesArray = jsonObject.getJSONArray("nodes")
                for (i in 0 until nodesArray.length()) {
                    val nodeObj = nodesArray.getJSONObject(i)
                    val name = nodeObj.optString("name", "Node_$i")
                    if (nodeObj.has("extras")) {
                        val extras = nodeObj.optJSONObject("extras")
                        if (extras != null && extras.has("prop")) {
                            val prop = extras.getString("prop")
                            if (prop.isNotBlank()) {
                                labels[name] = prop
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                inputStream.close()
            } catch (_: Exception) {}
        }
        return labels
    }
}
