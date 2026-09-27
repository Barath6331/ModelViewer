package com.barath.modelviewer.util

import android.graphics.PointF

object Math3D {

    /**
     * Multiplies a 4x4 matrix (column-major array of 16 floats) with a 4D vector.
     * result = M * v
     */
    fun multiplyMatrixAndVector4(matrix: FloatArray, vector: FloatArray, result: FloatArray) {
        val x = vector[0]
        val y = vector[1]
        val z = vector[2]
        val w = if (vector.size > 3) vector[3] else 1.0f

        result[0] = matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12] * w
        result[1] = matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13] * w
        result[2] = matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14] * w
        result[3] = matrix[3] * x + matrix[7] * y + matrix[11] * z + matrix[15] * w
    }

    /**
     * Multiplies two 4x4 matrices (column-major arrays of 16 doubles).
     * result = A * B
     */
    fun multiplyMatrix4(a: DoubleArray, b: DoubleArray, result: DoubleArray) {
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                var sum = 0.0
                for (k in 0 until 4) {
                    sum += a[k * 4 + i] * b[j * 4 + k]
                }
                result[j * 4 + i] = sum
            }
        }
    }

    /**
     * Projects a 3D world coordinate onto 2D screen coordinates into a FloatArray [x, y].
     */
    fun projectWorldToScreen(
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        viewMatrix: DoubleArray,
        projMatrix: DoubleArray,
        viewWidth: Int,
        viewHeight: Int,
        outCoords: FloatArray
    ): Boolean {
        // Step 1: Transform to Eye/Camera space: v_eye = View * v_world
        val eyeX = viewMatrix[0] * worldX + viewMatrix[4] * worldY + viewMatrix[8] * worldZ + viewMatrix[12]
        val eyeY = viewMatrix[1] * worldX + viewMatrix[5] * worldY + viewMatrix[9] * worldZ + viewMatrix[13]
        val eyeZ = viewMatrix[2] * worldX + viewMatrix[6] * worldY + viewMatrix[10] * worldZ + viewMatrix[14]
        val eyeW = viewMatrix[3] * worldX + viewMatrix[7] * worldY + viewMatrix[11] * worldZ + viewMatrix[15]

        // Step 2: Transform to Clip space: v_clip = Proj * v_eye
        val clipX = projMatrix[0] * eyeX + projMatrix[4] * eyeY + projMatrix[8] * eyeZ + projMatrix[12] * eyeW
        val clipY = projMatrix[1] * eyeX + projMatrix[5] * eyeY + projMatrix[9] * eyeZ + projMatrix[13] * eyeW
        val clipZ = projMatrix[2] * eyeX + projMatrix[6] * eyeY + projMatrix[10] * eyeZ + projMatrix[14] * eyeW
        val clipW = projMatrix[3] * eyeX + projMatrix[7] * eyeY + projMatrix[11] * eyeZ + projMatrix[15] * eyeW

        // Check if behind camera eye plane
        if (clipW <= 0.0001) {
            return false
        }

        // Step 3: Perspective divide to NDC [-1, 1]
        val ndcX = (clipX / clipW).toFloat()
        val ndcY = (clipY / clipW).toFloat()

        // Step 4: Map NDC to View Screen Coordinates (0,0 is top-left)
        outCoords[0] = (ndcX * 0.5f + 0.5f) * viewWidth
        outCoords[1] = (1.0f - (ndcY * 0.5f + 0.5f)) * viewHeight

        return true
    }

    /**
     * Overload accepting Android PointF.
     */
    fun projectWorldToScreen(
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        viewMatrix: DoubleArray,
        projMatrix: DoubleArray,
        viewWidth: Int,
        viewHeight: Int,
        outPoint: PointF
    ): Boolean {
        val coords = FloatArray(2)
        val success = projectWorldToScreen(worldX, worldY, worldZ, viewMatrix, projMatrix, viewWidth, viewHeight, coords)
        if (success) {
            outPoint.x = coords[0]
            outPoint.y = coords[1]
        }
        return success
    }
}
