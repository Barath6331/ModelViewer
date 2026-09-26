package com.barath.modelviewer

import com.barath.modelviewer.model.GlbParser
import com.barath.modelviewer.model.ModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class GlbParserTest {

    private fun getAssetFile(relativePath: String): File {
        val candidates = listOf(
            File("src/main/assets", relativePath),
            File("app/src/main/assets", relativePath),
            File("../app/src/main/assets", relativePath),
            File("c:/Users/barat/AndroidStudioProjects/modelviewer/app/src/main/assets", relativePath)
        )
        return candidates.firstOrNull { it.exists() } ?: File("src/main/assets", relativePath)
    }

    @Test
    fun testParseBulbGlb() {
        val file = getAssetFile("models/Bulb.glb")
        assertTrue("Bulb.glb must exist in assets: ${file.absolutePath}", file.exists())
        val labels = GlbParser.parsePartLabels(FileInputStream(file))
        assertTrue("Bulb should have labelled parts", labels.isNotEmpty())
        assertTrue("Bulb should contain Filament label", labels.values.contains("Filament"))
        assertTrue("Bulb should contain Glass Bulb label", labels.values.contains("Glass Bulb"))
    }

    @Test
    fun testParseAllCatalogModels() {
        for (item in ModelCatalog.AVAILABLE_MODELS) {
            val file = getAssetFile(item.assetPath)
            assertTrue("${item.assetPath} must exist in assets: ${file.absolutePath}", file.exists())
            val labels = GlbParser.parsePartLabels(FileInputStream(file))
            assertNotNull("Parsed labels should not be null for ${item.title}", labels)
            assertTrue("${item.title} should have at least 1 label", labels.isNotEmpty())
        }
    }

    @Test
    fun testCatalogCompleteness() {
        assertEquals(5, ModelCatalog.AVAILABLE_MODELS.size)
        val ids = ModelCatalog.AVAILABLE_MODELS.map { it.id }.toSet()
        assertEquals(5, ids.size)
    }
}
