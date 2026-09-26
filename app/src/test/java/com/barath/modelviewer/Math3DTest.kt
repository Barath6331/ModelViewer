package com.barath.modelviewer

import com.barath.modelviewer.util.Math3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Math3DTest {

    @Test
    fun testMatrixVectorMultiplication() {
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
        val vector = floatArrayOf(2f, 3f, 4f, 1f)
        val result = FloatArray(4)

        Math3D.multiplyMatrixAndVector4(identity, vector, result)

        assertEquals(2f, result[0], 0.0001f)
        assertEquals(3f, result[1], 0.0001f)
        assertEquals(4f, result[2], 0.0001f)
        assertEquals(1f, result[3], 0.0001f)
    }

    @Test
    fun testProjectWorldToScreen_InFrontOfCamera() {
        // Camera at (0,0,5) looking down -Z
        val viewMatrix = doubleArrayOf(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, -5.0, 1.0
        )

        val projMatrix = doubleArrayOf(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, -1.02, -1.0,
            0.0, 0.0, -2.02, 0.0
        )

        val outCoords = FloatArray(2)
        val success = Math3D.projectWorldToScreen(
            worldX = 0f,
            worldY = 0f,
            worldZ = 0f,
            viewMatrix = viewMatrix,
            projMatrix = projMatrix,
            viewWidth = 800,
            viewHeight = 600,
            outCoords = outCoords
        )

        assertTrue("Projection should succeed for point in front of camera", success)
        assertEquals(400f, outCoords[0], 1.0f)
        assertEquals(300f, outCoords[1], 1.0f)
    }

    @Test
    fun testProjectWorldToScreen_BehindCamera_ShouldFail() {
        val viewMatrix = doubleArrayOf(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 5.0, 1.0
        )
        val projMatrix = doubleArrayOf(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, -1.02, -1.0,
            0.0, 0.0, -2.02, 0.0
        )

        val outCoords = FloatArray(2)
        val success = Math3D.projectWorldToScreen(
            worldX = 0f,
            worldY = 0f,
            worldZ = 0f,
            viewMatrix = viewMatrix,
            projMatrix = projMatrix,
            viewWidth = 800,
            viewHeight = 600,
            outCoords = outCoords
        )

        assertFalse("Projection should return false for point behind camera", success)
    }
}
