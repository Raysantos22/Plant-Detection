package Plantdetection



// Define plant conditions with prevention and treatment tips
object PlantConditionData {
    data class PlantCondition(
        val name: String,
        val description: String,
        val preventionTips: List<String>,
        val treatmentTips: List<String>
    )
    
    val conditions = mapOf(
        "Anthracnose" to PlantCondition(
            "Anthracnose",
            "A fungal disease that causes dark, sunken lesions on fruits, stems, and leaves.",
            listOf(
                "Use disease-free seeds and transplants",
                "Rotate crops every 2-3 years",
                "Ensure good air circulation between plants",
                "Avoid overhead watering"
            ),
            listOf(
                "Remove and destroy infected plant parts",
                "Apply fungicides with active ingredients like chlorothalonil or mancozeb",
                "Apply copper-based fungicides every 7-10 days in wet weather"
            )
        ),
        "Blossom End Rot" to PlantCondition(
            "Blossom End Rot",
            "A physiological disorder caused by calcium deficiency, resulting in dark, sunken areas at the blossom end of fruits.",
            listOf(
                "Maintain consistent soil moisture",
                "Test soil pH and maintain at 6.5",
                "Add calcium to the soil before planting",
                "Avoid excessive nitrogen fertilization"
            ),
            listOf(
                "Apply calcium foliar sprays",
                "Maintain even watering schedule",
                "Remove affected fruits to prevent stress on the plant",
                "Add eggshells or calcium supplements to soil"
            )
        ),
        "Collectotrichum rot" to PlantCondition(
            "Collectotrichum rot",
            "A fungal disease causing circular sunken spots on fruits with pink spore masses.",
            listOf(
                "Plant resistant varieties",
                "Use drip irrigation instead of overhead watering",
                "Provide adequate spacing between plants",
                "Apply fungicide preventatively in humid weather"
            ),
            listOf(
                "Remove and destroy infected fruits",
                "Apply approved fungicides",
                "Improve air circulation around plants",
                "Sanitize garden tools after use"
            )
        ),
        "Healthy Tomato" to PlantCondition(
            "Healthy Tomato",
            "Your tomato plant is healthy! Here are some tips to maintain its health.",
            listOf(
                "Water consistently, about 1-2 inches per week",
                "Apply balanced fertilizer every 4-6 weeks",
                "Prune suckers for indeterminate varieties",
                "Support plants with cages or stakes",
                "Mulch to retain moisture and prevent soil splash"
            ),
            listOf(
                "Continue regular maintenance",
                "Monitor for early signs of pests or disease",
                "Harvest fruits when fully ripe",
                "Remove yellow or diseased leaves promptly"
            )
        ),
        "Healthy Eggplant" to PlantCondition(
            "Healthy Eggplant",
            "Your eggplant is healthy! Here are some tips to maintain its health.",
            listOf(
                "Water deeply and consistently",
                "Apply balanced fertilizer monthly",
                "Maintain temperatures between 70-85°F",
                "Support plants with stakes",
                "Mulch to retain moisture and suppress weeds"
            ),
            listOf(
                "Continue regular maintenance",
                "Monitor for early signs of pests or disease",
                "Harvest fruits when skin is glossy",
                "Pruning can increase air circulation"
            )
        ),
        "Leaf Caterpillar" to PlantCondition(
            "Leaf Caterpillar",
            "Caterpillars that eat plant leaves, causing irregular holes and damage.",
            listOf(
                "Inspect plants regularly for eggs and young caterpillars",
                "Encourage beneficial insects like wasps and ladybugs",
                "Use row covers during peak moth activity",
                "Apply neem oil as a preventative"
            ),
            listOf(
                "Handpick caterpillars off plants",
                "Apply Bacillus thuringiensis (Bt) spray",
                "Use insecticidal soap for young caterpillars",
                "Apply spinosad for severe infestations"
            )
        ),
        "Leaf Roller" to PlantCondition(
            "Leaf Roller",
            "Small caterpillars that roll leaves and feed inside the shelter.",
            listOf(
                "Remove plant debris in fall to eliminate overwintering sites",
                "Encourage natural predators like birds and beneficial insects",
                "Avoid excessive nitrogen fertilization",
                "Monitor plants regularly"
            ),
            listOf(
                "Prune and destroy infested leaves",
                "Apply Bacillus thuringiensis (Bt) spray",
                "Use spinosad-based insecticides",
                "Apply horticultural oil to smother eggs"
            )
        ),
        "Melon thrips" to PlantCondition(
            "Melon thrips",
            "Tiny insects that suck plant sap, causing silvering, scarring, and distortion of leaves.",
            listOf(
                "Use reflective mulch to deter thrips",
                "Use blue or yellow sticky traps",
                "Avoid planting near onions or garlic",
                "Use floating row covers"
            ),
            listOf(
                "Apply insecticidal soap or neem oil",
                "Use spinosad-based insecticides",
                "Release predatory mites",
                "Remove and destroy heavily infested plants"
            )
        ),
        "Rice Water Weevil" to PlantCondition(
            "Rice Water Weevil",
            "Beetles that feed on leaves and larvae that feed on roots, causing wilting and reduced vigor.",
            listOf(
                "Rotate crops annually",
                "Maintain good drainage in growing areas",
                "Time planting to avoid peak adult activity",
                "Use floating row covers during adult flight periods"
            ),
            listOf(
                "Apply beneficial nematodes to soil",
                "Use pyrethrin-based insecticides for adults",
                "Flood and drain growing areas to disrupt larval development",
                "Apply systemic insecticides as a last resort"
            )
        ),
        "White Fly" to PlantCondition(
            "White Fly",
            "Small, white flying insects that feed on plant sap and excrete honeydew, leading to sooty mold.",
            listOf(
                "Use yellow sticky traps",
                "Inspect new plants before introducing to garden",
                "Use reflective mulches",
                "Encourage natural predators like ladybugs and lacewings"
            ),
            listOf(
                "Spray plants forcefully with water to dislodge insects",
                "Apply insecticidal soap or neem oil",
                "Use horticultural oils to smother eggs and nymphs",
                "In severe cases, use imidacloprid or other systemic insecticides"
            )
        )
    )
}

// Splash Screen Activity with Logo


// Onboarding Activity with ViewPager2

// Vegetable Selection Activity - Tomato and Eggplant only

// Loading Activity before Camera

// Adapter for Onboarding ViewPager
