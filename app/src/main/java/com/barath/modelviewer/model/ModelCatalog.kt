package com.barath.modelviewer.model

data class ModelItem(
    val id: String,
    val title: String,
    val category: String,
    val assetPath: String,
    val iconEmoji: String,
    val description: String,
    val defaultScale: Float = 1.0f
)

object ModelCatalog {

    val AVAILABLE_MODELS = listOf(
        ModelItem(
            id = "microscope",
            title = "Microscope",
            category = "Scientific Equipment",
            assetPath = "models/Microscope.glb",
            iconEmoji = "🔬",
            description = "High-precision optical microscope with 12 labelled components",
            defaultScale = 1.0f
        ),
        ModelItem(
            id = "bulb",
            title = "Incandescent Bulb",
            category = "Electronics",
            assetPath = "models/Bulb.glb",
            iconEmoji = "💡",
            description = "Classic light bulb showcasing filament, mount, and base contacts",
            defaultScale = 1.0f
        ),
        ModelItem(
            id = "solarsystem",
            title = "Solar System",
            category = "Astronomy",
            assetPath = "models/solarsystem.glb",
            iconEmoji = "🪐",
            description = "Planetary model featuring orbiting planets with labels",
            defaultScale = 1.0f
        ),
        ModelItem(
            id = "lungs",
            title = "Human Lungs",
            category = "Anatomy",
            assetPath = "models/Lungs.glb",
            iconEmoji = "🫁",
            description = "Respiratory system with trachea, bronchus, and bronchial tree",
            defaultScale = 1.0f
        ),
        ModelItem(
            id = "fiagena",
            title = "Bacterial Flagella",
            category = "Microbiology",
            assetPath = "models/Fiagena.glb",
            iconEmoji = "🧬",
            description = "Bacterial motility engine with rotor rings and helical hook",
            defaultScale = 1.0f
        )
    )

    fun getModelById(id: String): ModelItem? {
        return AVAILABLE_MODELS.find { it.id == id }
    }
}
