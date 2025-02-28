package com.PlantDetection

// Define plant conditions with prevention and treatment tips
object PlantConditionData {
    data class PlantCondition(
        val name: String,
        val description: String,
        val preventionTips: List<String>,
        val treatmentTips: List<String>,
        // Added detailed tasks for treatment
        val treatmentTasks: List<TreatmentTask>
    )

    data class TreatmentTask(
        val taskName: String,
        val description: String,
        val scheduleInterval: Int, // Days between checks
        val materials: List<String> = emptyList(),
        val instructions: List<String> = emptyList()
    )

    val conditions = mapOf(
        "Anthracnose" to PlantCondition(
            "Anthracnose",
            "A fungal disease that causes dark, sunken lesions on fruits, stems, and leaves.",
            listOf(
                "Use disease-free seeds and transplants",
                "Rotate crops every 2-3 years",
                "Ensure good air circulation between plants",
                "Avoid overhead watering",
                "Apply preventative fungicide during humid weather"
            ),
            listOf(
                "Remove and destroy infected plant parts",
                "Apply fungicides with active ingredients like chlorothalonil or mancozeb",
                "Apply copper-based fungicides every 7-10 days in wet weather",
                "Improve air circulation by pruning",
                "Avoid working with plants when they're wet"
            ),
            listOf(
                TreatmentTask(
                    "Remove Infected Parts",
                    "Prune and remove all visibly infected leaves, stems and fruits",
                    1,
                    listOf("Clean scissors or pruning shears", "Disposal bag"),
                    listOf(
                        "Identify all plant parts with dark, sunken lesions",
                        "Cut at least 1 inch below visible infection",
                        "Dispose of infected parts in sealed bag, don't compost",
                        "Disinfect tools with 10% bleach solution between cuts"
                    )
                ),
                TreatmentTask(
                    "Apply Fungicide",
                    "Apply appropriate fungicide to treat remaining infection and prevent spread",
                    7,
                    listOf("Copper-based fungicide or chlorothalonil", "Sprayer", "Protective gloves"),
                    listOf(
                        "Mix fungicide according to label instructions",
                        "Apply in early morning or evening, not during hot sun",
                        "Cover all plant surfaces, especially undersides of leaves",
                        "Repeat every 7-10 days for at least 3 applications",
                        "Avoid watering for 24 hours after application"
                    )
                ),
                TreatmentTask(
                    "Improve Air Circulation",
                    "Thin plants to improve air flow and reduce humidity around plants",
                    2,
                    listOf("Pruning shears", "Plant stakes or cages"),
                    listOf(
                        "Remove some inner foliage to improve air circulation",
                        "Stake plants to keep foliage off ground",
                        "Space plants adequately",
                        "Remove weeds around plants"
                    )
                ),
                TreatmentTask(
                    "Follow-up Inspection",
                    "Monitor plants for new infections or spread",
                    3,
                    listOf("Magnifying glass (optional)", "Garden journal"),
                    listOf(
                        "Check new growth for signs of infection",
                        "Inspect stems near previous infection sites",
                        "Record progress in garden journal",
                        "If disease persists, consider stronger fungicide options"
                    )
                )
            )
        ),
        "Blossom End Rot" to PlantCondition(
            "Blossom End Rot",
            "A physiological disorder caused by calcium deficiency, resulting in dark, sunken areas at the blossom end of fruits.",
            listOf(
                "Maintain consistent soil moisture",
                "Test soil pH and maintain at 6.5",
                "Add calcium to the soil before planting",
                "Avoid excessive nitrogen fertilization",
                "Mulch around plants to conserve moisture"
            ),
            listOf(
                "Apply calcium foliar sprays",
                "Maintain even watering schedule",
                "Remove affected fruits to prevent stress on the plant",
                "Add eggshells or calcium supplements to soil",
                "Apply gypsum to soil to add calcium without changing pH"
            ),
            listOf(
                TreatmentTask(
                    "Apply Calcium Solution",
                    "Apply calcium foliar spray or calcium nitrate solution",
                    7,
                    listOf("Calcium nitrate or calcium chloride solution", "Sprayer"),
                    listOf(
                        "Mix 4 tablespoons of calcium nitrate in 1 gallon of water",
                        "Apply as a foliar spray in early morning",
                        "Alternatively, drench soil around plant base",
                        "Repeat weekly until new fruits show no symptoms"
                    )
                ),
                TreatmentTask(
                    "Remove Affected Fruits",
                    "Remove fruits showing blossom end rot to reduce plant stress",
                    3,
                    listOf("Pruning shears", "Disposal bag"),
                    listOf(
                        "Identify fruits with dark, sunken areas at bottom end",
                        "Remove affected fruits completely",
                        "Dispose of fruits (don't compost)"
                    )
                ),
                TreatmentTask(
                    "Improve Watering",
                    "Establish consistent watering schedule to prevent fluctuations",
                    1,
                    listOf("Mulch", "Drip irrigation system (optional)", "Moisture meter"),
                    listOf(
                        "Apply 2-3 inches of mulch around plants to retain moisture",
                        "Water deeply (1-2 inches) rather than frequently",
                        "Water at soil level, not on foliage",
                        "Monitor soil moisture with finger test or moisture meter",
                        "Aim for consistent, even moisture"
                    )
                ),
                TreatmentTask(
                    "Add Calcium to Soil",
                    "Add long-term calcium solutions to soil",
                    14,
                    listOf("Crushed eggshells or agricultural gypsum or lime"),
                    listOf(
                        "For immediate use: sprinkle 2-3 tablespoons of gypsum around each plant",
                        "For long-term: work in crushed eggshells around plant base",
                        "Water thoroughly after application",
                        "Avoid adding lime unless soil pH test indicates acidity"
                    )
                )
            )
        ),
        "Collectotrichum rot" to PlantCondition(
            "Collectotrichum rot",
            "A fungal disease causing circular sunken spots on fruits with pink spore masses.",
            listOf(
                "Plant resistant varieties",
                "Use drip irrigation instead of overhead watering",
                "Provide adequate spacing between plants",
                "Apply fungicide preventatively in humid weather",
                "Rotate crops and avoid planting in areas with previous infections"
            ),
            listOf(
                "Remove and destroy infected fruits",
                "Apply approved fungicides",
                "Improve air circulation around plants",
                "Sanitize garden tools after use",
                "Apply organic fungicides like neem oil for minor infections"
            ),
            listOf(
                TreatmentTask(
                    "Remove Infected Fruits",
                    "Remove all fruits showing symptoms to prevent spread",
                    2,
                    listOf("Gloves", "Disposal bags"),
                    listOf(
                        "Identify fruits with circular sunken spots or pink spore masses",
                        "Carefully remove all infected fruits",
                        "Place in sealed bags for disposal (do not compost)",
                        "Wash hands and sanitize tools after handling"
                    )
                ),
                TreatmentTask(
                    "Apply Fungicide Treatment",
                    "Apply appropriate fungicide to stop disease spread",
                    7,
                    listOf("Approved fungicide (chlorothalonil, mancozeb, or copper-based)", "Sprayer", "Protective gear"),
                    listOf(
                        "Mix fungicide according to label instructions",
                        "Apply thoroughly, covering all plant surfaces",
                        "Focus on remaining fruits and surrounding foliage",
                        "Apply in early morning or evening for best absorption",
                        "Repeat application every 7-10 days as instructed on label"
                    )
                ),
                TreatmentTask(
                    "Improve Growing Conditions",
                    "Modify growing conditions to discourage disease development",
                    3,
                    listOf("Pruning shears", "Mulch", "Drip irrigation supplies (optional)"),
                    listOf(
                        "Prune plants to improve air circulation",
                        "Apply mulch to prevent soil splash onto plants",
                        "Convert to drip irrigation if using overhead watering",
                        "Ensure proper plant spacing"
                    )
                ),
                TreatmentTask(
                    "Sanitize Garden Area",
                    "Clean area to reduce reinfection chances",
                    7,
                    listOf("10% bleach solution", "Spray bottle", "Clean rags"),
                    listOf(
                        "Sanitize all tools used on infected plants",
                        "Clean plant supports and stakes",
                        "Remove any plant debris from around affected plants",
                        "Avoid working with plants when wet"
                    )
                )
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
                "Remove yellow or diseased leaves promptly",
                "Apply compost tea as a natural boost"
            ),
            listOf(
                TreatmentTask(
                    "Regular Watering",
                    "Maintain consistent watering schedule",
                    2,
                    listOf("Watering can or drip irrigation"),
                    listOf(
                        "Water deeply at the base of plant",
                        "Provide 1-2 inches of water per week",
                        "Adjust for rainfall and temperature",
                        "Water in morning to allow foliage to dry"
                    )
                ),
                TreatmentTask(
                    "Fertilize",
                    "Apply balanced fertilizer to maintain growth",
                    14,
                    listOf("Balanced fertilizer (10-10-10 or organic equivalent)", "Compost"),
                    listOf(
                        "Apply fertilizer according to package directions",
                        "Spread in a ring around plants, avoiding direct contact with stems",
                        "Water thoroughly after application",
                        "Reduce fertilizer when plants begin fruiting, switching to lower nitrogen"
                    )
                ),
                TreatmentTask(
                    "Pruning and Support",
                    "Prune and support plants for better air circulation and fruit production",
                    7,
                    listOf("Pruning shears", "Plant ties", "Tomato cages or stakes"),
                    listOf(
                        "Remove suckers for indeterminate varieties",
                        "Prune lower leaves that touch the soil",
                        "Tie main stems to supports as they grow",
                        "Remove any yellow or diseased leaves"
                    )
                ),
                TreatmentTask(
                    "Preventative Disease Check",
                    "Monitor plants for early signs of disease or pests",
                    3,
                    listOf("Magnifying glass (optional)", "Garden journal"),
                    listOf(
                        "Inspect underside of leaves for pests",
                        "Look for spots, curling, or discoloration on leaves",
                        "Check fruit for any abnormalities",
                        "Record observations to track changes over time"
                    )
                )
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
                "Pruning can increase air circulation",
                "Apply neem oil preventatively for common pests"
            ),
            listOf(
                TreatmentTask(
                    "Regular Watering",
                    "Maintain consistent watering schedule",
                    2,
                    listOf("Watering can or drip irrigation"),
                    listOf(
                        "Water deeply at the base of plant",
                        "Keep soil consistently moist but not waterlogged",
                        "Provide approximately 1-1.5 inches of water weekly",
                        "Increase during hot weather, decrease during cool periods"
                    )
                ),
                TreatmentTask(
                    "Fertilize",
                    "Apply balanced fertilizer to support growth and fruit production",
                    14,
                    listOf("Balanced fertilizer (10-10-10 or 5-10-10)"),
                    listOf(
                        "Apply fertilizer according to package directions",
                        "Reduce nitrogen once plants begin flowering",
                        "Water thoroughly after application",
                        "Consider side-dressing with compost monthly"
                    )
                ),
                TreatmentTask(
                    "Support and Prune",
                    "Support plants and remove excess foliage for better production",
                    7,
                    listOf("Plant stakes", "Pruning shears", "Soft plant ties"),
                    listOf(
                        "Secure main stems to stakes as plant grows",
                        "Remove lower leaves that touch the soil",
                        "Prune to 4-5 main branches for better air circulation",
                        "Remove any damaged or diseased foliage"
                    )
                ),
                TreatmentTask(
                    "Pest Prevention",
                    "Apply preventative measures against common eggplant pests",
                    7,
                    listOf("Neem oil", "Organic insecticidal soap", "Sprayer"),
                    listOf(
                        "Mix neem oil according to package directions",
                        "Apply to all plant surfaces, especially leaf undersides",
                        "Treat in early morning or evening, not during hot sun",
                        "Monitor for flea beetles, aphids, and spider mites"
                    )
                )
            )
        ),
        "Leaf Caterpillar" to PlantCondition(
            "Leaf Caterpillar",
            "Caterpillars that eat plant leaves, causing irregular holes and damage.",
            listOf(
                "Inspect plants regularly for eggs and young caterpillars",
                "Encourage beneficial insects like wasps and ladybugs",
                "Use row covers during peak moth activity",
                "Apply neem oil as a preventative",
                "Plant trap crops like dill or fennel nearby"
            ),
            listOf(
                "Handpick caterpillars off plants",
                "Apply Bacillus thuringiensis (Bt) spray",
                "Use insecticidal soap for young caterpillars",
                "Apply spinosad for severe infestations",
                "Introduce beneficial insects like trichogramma wasps"
            ),
            listOf(
                TreatmentTask(
                    "Manual Removal",
                    "Handpick and remove caterpillars from plants",
                    1,
                    listOf("Gloves", "Bucket of soapy water"),
                    listOf(
                        "Inspect plants thoroughly, especially leaf undersides",
                        "Look for caterpillars, eggs, and frass (droppings)",
                        "Remove caterpillars by hand and drop into soapy water",
                        "Crush or remove egg masses (often on leaf undersides)",
                        "Repeat daily until infestation is controlled"
                    )
                ),
                TreatmentTask(
                    "Apply Bt Treatment",
                    "Apply Bacillus thuringiensis spray for biological control",
                    5,
                    listOf("Bt spray (Bacillus thuringiensis)", "Sprayer"),
                    listOf(
                        "Mix Bt according to package directions",
                        "Apply thoroughly to all plant surfaces, especially leaf undersides",
                        "Apply in evening for best results (Bt breaks down in sunlight)",
                        "Reapply after rainfall",
                        "Note: Bt only affects caterpillars, not beneficial insects"
                    )
                ),
                TreatmentTask(
                    "Apply Insecticidal Soap",
                    "Use insecticidal soap for additional control",
                    3,
                    listOf("Insecticidal soap", "Sprayer"),
                    listOf(
                        "Mix insecticidal soap according to label instructions",
                        "Apply directly to caterpillars for best effect",
                        "Cover all plant surfaces, focusing on leaf undersides",
                        "Apply in early morning or evening, not during hot sun",
                        "Repeat as needed according to product instructions"
                    )
                ),
                TreatmentTask(
                    "Follow-up Monitoring",
                    "Check for effectiveness and reapply treatments as needed",
                    2,
                    listOf("Magnifying glass (optional)", "Garden journal"),
                    listOf(
                        "Inspect plants for new caterpillars or eggs",
                        "Look for continued leaf damage",
                        "Record effectiveness of treatments",
                        "If infestation persists, consider spinosad or other organic treatments"
                    )
                )
            )
        ),
        "Leaf Roller" to PlantCondition(
            "Leaf Roller",
            "Small caterpillars that roll leaves and feed inside the shelter.",
            listOf(
                "Remove plant debris in fall to eliminate overwintering sites",
                "Encourage natural predators like birds and beneficial insects",
                "Avoid excessive nitrogen fertilization",
                "Monitor plants regularly",
                "Use pheromone traps to monitor moth populations"
            ),
            listOf(
                "Prune and destroy infested leaves",
                "Apply Bacillus thuringiensis (Bt) spray",
                "Use spinosad-based insecticides",
                "Apply horticultural oil to smother eggs",
                "Introduce predatory insects like lacewings"
            ),
            listOf(
                TreatmentTask(
                    "Remove Rolled Leaves",
                    "Identify and remove leaves with caterpillars inside",
                    2,
                    listOf("Pruning shears", "Disposal bag"),
                    listOf(
                        "Identify rolled or webbed leaves (main indicator)",
                        "Carefully cut off affected leaves",
                        "Open rolls to confirm caterpillar presence",
                        "Destroy removed leaves (don't compost)",
                        "Check all plants in vicinity"
                    )
                ),
                TreatmentTask(
                    "Apply Bt Spray",
                    "Apply Bacillus thuringiensis for biological control",
                    5,
                    listOf("Bt spray", "Sprayer"),
                    listOf(
                        "Mix Bt according to package directions",
                        "Apply thoroughly to all plant surfaces",
                        "Apply in evening for best results",
                        "Focus especially on new growth and unaffected leaves",
                        "Reapply after rainfall"
                    )
                ),
                TreatmentTask(
                    "Apply Horticultural Oil",
                    "Use horticultural oil to smother eggs and small larvae",
                    7,
                    listOf("Horticultural oil", "Sprayer"),
                    listOf(
                        "Mix oil according to product instructions",
                        "Apply to all plant surfaces, ensuring complete coverage",
                        "Apply in early morning before temperatures rise",
                        "Do not apply when temperatures exceed 85°F",
                        "Repeat every 7-10 days for 2-3 applications"
                    )
                ),
                TreatmentTask(
                    "Monitor and Follow-up",
                    "Check effectiveness and continue surveillance",
                    3,
                    listOf("Magnifying glass", "Garden journal"),
                    listOf(
                        "Inspect new growth for signs of rolling",
                        "Look for moth activity around plants in evening",
                        "Record effectiveness of treatments",
                        "If severe infestation continues, consider spinosad application"
                    )
                )
            )
        ),
        "Melon thrips" to PlantCondition(
            "Melon thrips",
            "Tiny insects that suck plant sap, causing silvering, scarring, and distortion of leaves.",
            listOf(
                "Use reflective mulch to deter thrips",
                "Use blue or yellow sticky traps",
                "Avoid planting near onions or garlic",
                "Use floating row covers",
                "Plant trap crops like basil or marigold"
            ),
            listOf(
                "Apply insecticidal soap or neem oil",
                "Use spinosad-based insecticides",
                "Release predatory mites",
                "Remove and destroy heavily infested plants",
                "Apply diatomaceous earth around plant base"
            ),
            listOf(
                TreatmentTask(
                    "Apply Insecticidal Soap",
                    "Treat plants with insecticidal soap to control thrips",
                    3,
                    listOf("Insecticidal soap", "Sprayer"),
                    listOf(
                        "Mix soap solution according to label directions",
                        "Apply thoroughly to all plant surfaces, especially undersides of leaves",
                        "Apply in early morning or evening, avoiding hot sun",
                        "Ensure complete coverage for contact killing",
                        "Repeat every 5-7 days for at least 3 applications"
                    )
                ),
                TreatmentTask(
                    "Apply Neem Oil",
                    "Use neem oil as an organic control method",
                    7,
                    listOf("Neem oil", "Sprayer"),
                    listOf(
                        "Mix neem oil according to package directions",
                        "Apply to all plant surfaces, especially leaf undersides",
                        "Apply in early morning or evening, avoiding hot sun",
                        "Repeat weekly for at least 3 applications",
                        "Avoid applying when beneficial insects are active"
                    )
                ),
                TreatmentTask(
                    "Set Up Sticky Traps",
                    "Use sticky traps to monitor and reduce thrips population",
                    5,
                    listOf("Blue or yellow sticky traps", "Wooden stakes"),
                    listOf(
                        "Place sticky traps at plant height around affected plants",
                        "Use blue traps (most effective for thrips) or yellow traps",
                        "Space traps every 3-5 feet around garden area",
                        "Check traps regularly to monitor population",
                        "Replace traps when surface becomes covered with insects"
                    )
                ),
                TreatmentTask(
                    "Apply Spinosad Treatment",
                    "Use spinosad for more severe infestations",
                    7,
                    listOf("Spinosad-based insecticide", "Sprayer", "Protective gear"),
                    listOf(
                        "Mix spinosad product according to label instructions",
                        "Apply thoroughly to all plant surfaces",
                        "Apply in evening to minimize impact on beneficial insects",
                        "Avoid applying near flowering plants when bees are active",
                        "Repeat according to product instructions, typically every 7-10 days"
                    )
                )
            )
        ),
        "Rice Water Weevil" to PlantCondition(
            "Rice Water Weevil",
            "Beetles that feed on leaves and larvae that feed on roots, causing wilting and reduced vigor.",
            listOf(
                "Rotate crops annually",
                "Maintain good drainage in growing areas",
                "Time planting to avoid peak adult activity",
                "Use floating row covers during adult flight periods",
                "Plant trap crops around garden perimeter"
            ),
            listOf(
                "Apply beneficial nematodes to soil",
                "Use pyrethrin-based insecticides for adults",
                "Flood and drain growing areas to disrupt larval development",
                "Apply systemic insecticides as a last resort",
                "Introduce predatory insects like ground beetles"
            ),
            listOf(
                TreatmentTask(
                    "Apply Beneficial Nematodes",
                    "Introduce beneficial nematodes to soil to control larvae",
                    14,
                    listOf("Beneficial nematodes (Steinernema or Heterorhabditis species)", "Watering can"),
                    listOf(
                        "Purchase fresh nematodes from reliable supplier",
                        "Mix according to package directions in cool water",
                        "Apply to moist soil in evening or on cloudy day",
                        "Water area before and after application",
                        "Keep soil moist for at least two weeks after application"
                    )
                ),
                TreatmentTask(
                    "Apply Pyrethrin Spray",
                    "Use pyrethrin spray to control adult weevils",
                    5,
                    listOf("Pyrethrin-based insecticide", "Sprayer"),
                    listOf(
                        "Mix pyrethrin according to label instructions",
                        "Apply to plant foliage when adults are active (usually evening)",
                        "Focus on leaf margins where feeding occurs",
                        "Repeat application after rainfall",
                        "Apply every 5-7 days during peak activity periods"
                    )
                ),
                TreatmentTask(
                    "Set up Row Covers",
                    "Install floating row covers to prevent adult egg laying",
                    1,
                    listOf("Lightweight floating row cover", "Garden hoops or stakes", "Weights or soil"),
                    listOf(
                        "Drape row cover over plants, using hoops for support",
                        "Secure edges with soil, rocks, or weights",
                        "Ensure complete sealing around edges",
                        "Leave enough slack for plant growth",
                        "Remove temporarily for pollination if needed"
                    )
                ),
                TreatmentTask(
                    "Improve Drainage",
                    "Modify soil and drainage to discourage larval development",
                    7,
                    listOf("Digging tools", "Organic matter or sand"),
                    listOf(
                        "Create raised beds or improve existing drainage",
                        "Add organic matter to heavy clay soils",
                        "Ensure water doesn't pool around plants",
                        "Avoid overwatering which can encourage larvae",
                        "Allow soil to dry slightly between waterings"
                    )
                )
            )
        ),
        "White Fly" to PlantCondition(
            "White Fly",
            "Small, white flying insects that feed on plant sap and excrete honeydew, leading to sooty mold.",
            listOf(
                "Use yellow sticky traps",
                "Inspect new plants before introducing to garden",
                "Use reflective mulches",
                "Encourage natural predators like ladybugs and lacewings",
                "Plant trap crops like nasturtiums nearby"
            ),
            listOf(
                "Spray plants forcefully with water to dislodge insects",
                "Apply insecticidal soap or neem oil",
                "Use horticultural oils to smother eggs and nymphs",
                "In severe cases, use imidacloprid or other systemic insecticides",
                "Introduce parasitic wasps (Encarsia formosa)"
            ),
            listOf(
                TreatmentTask(
                    "Water Spray Treatment",
                    "Use strong water spray to dislodge whiteflies",
                    2,
                    listOf("Garden hose with spray nozzle"),
                    listOf(
                        "Adjust hose to strong spray setting",
                        "Spray undersides of leaves forcefully",
                        "Focus on areas with visible whiteflies",
                        "Perform in morning so leaves dry quickly",
                        "Repeat every 2-3 days for at least a week"
                    )
                ),
                TreatmentTask(
                    "Apply Insecticidal Soap",
                    "Use insecticidal soap for direct control",
                    3,
                    listOf("Insecticidal soap", "Sprayer"),
                    listOf(
                        "Mix soap according to package directions",
                        "Apply thoroughly to all plant surfaces, especially leaf undersides",
                        "Apply in early morning or evening, never in hot sun",
                        "Ensure complete coverage for contact killing",
                        "Repeat every 5-7 days for at least 3 applications"
                    )
                ),
                TreatmentTask(
                    "Apply Neem Oil",
                    "Use neem oil as a natural insecticide and repellent",
                    7,
                    listOf("Neem oil", "Sprayer"),
                    listOf(
                        "Mix neem oil according to package directions",
                        "Apply to all plant surfaces, especially leaf undersides",
                        "Apply in early morning or evening, avoiding hot sun",
                        "Focus on new growth where whiteflies congregate",
                        "Repeat weekly for at least 3 applications"
                    )
                ),
                TreatmentTask(
                    "Set Up Yellow Sticky Traps",
                    "Use traps to monitor and reduce whitefly populations",
                    5,
                    listOf("Yellow sticky traps", "Wooden stakes or hangers"),
                    listOf(
                        "Hang yellow sticky traps at plant height",
                        "Place near but not touching plants",
                        "Use at least one trap per 2-3 plants",
                        "Check traps regularly to monitor population",
                        "Replace when surface becomes covered with insects"
                    )
                ),
                TreatmentTask(
                    "Clean Honeydew and Sooty Mold",
                    "Remove honeydew and sooty mold from leaves",
                    5,
                    listOf("Mild soap solution", "Soft cloth", "Spray bottle"),
                    listOf(
                        "Mix mild soap solution (few drops in water)",
                        "Gently wipe affected leaves to remove mold",
                        "Rinse with clean water spray",
                        "Remove severely affected leaves",
                        "Treat underlying whitefly problem to prevent recurrence"
                    )
                )
            )
        )
    )
}