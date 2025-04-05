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
        "Anthracnose (Diseased)" to PlantCondition(
            "Anthracnose (Diseased)",
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
        "Blossom End Rot (Diseased)" to PlantCondition(
            "Blossom End Rot (Diseased)",
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
        "Collectotrichum rot (Diseased)" to PlantCondition(
            "Collectotrichum rot (Diseased)",
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
        "Healthy eggplant" to PlantCondition(
            "Healthy eggplant",
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
        "Aphids (Infested)" to PlantCondition(
            "Aphids (Infested)",
            "Small, soft-bodied insects that cluster on new growth and undersides of leaves, causing stunting, yellowing, and sticky honeydew residue.",
            listOf(
                "Encourage beneficial insects like ladybugs and lacewings",
                "Plant companion plants like marigolds, nasturtiums, or herbs",
                "Avoid excessive nitrogen fertilizer which promotes tender growth",
                "Use reflective mulch to confuse aphids",
                "Regularly inspect plants for early detection"
            ),
            listOf(
                "Spray plants forcefully with water to dislodge aphids",
                "Apply insecticidal soap to affected areas",
                "Use neem oil solution for persistent infestations",
                "Introduce beneficial insects like ladybugs or lacewings",
                "Apply diluted dish soap solution to affected areas"
            ),
            listOf(
                TreatmentTask(
                    "Water Spray Treatment",
                    "Forcefully spray plants with water to dislodge aphids",
                    1,
                    listOf("Garden hose with adjustable nozzle"),
                    listOf(
                        "Adjust hose to medium-strong spray setting",
                        "Target undersides of leaves where aphids congregate",
                        "Spray in morning so plants can dry completely",
                        "Repeat daily for 3-5 days for persistent infestations"
                    )
                ),
                TreatmentTask(
                    "Apply Insecticidal Soap",
                    "Treat plants with insecticidal soap to kill aphids on contact",
                    3,
                    listOf("Insecticidal soap", "Spray bottle"),
                    listOf(
                        "Mix insecticidal soap according to package directions",
                        "Spray all affected plant parts, especially leaf undersides",
                        "Apply when temperatures are below 85°F to prevent leaf burn",
                        "Repeat every 5-7 days until aphids are controlled"
                    )
                ),
                TreatmentTask(
                    "Neem Oil Application",
                    "Apply neem oil to disrupt aphid feeding and reproduction",
                    7,
                    listOf("Neem oil", "Spray bottle", "Mild soap (optional emulsifier)"),
                    listOf(
                        "Mix 2 tsp neem oil and 1 tsp mild soap in 1 quart water",
                        "Apply thoroughly to all plant surfaces, especially new growth",
                        "Apply in evening or early morning to prevent leaf burn",
                        "Repeat weekly for at least 3 applications"
                    )
                ),
                TreatmentTask(
                    "Monitor and Reapply",
                    "Check plants for aphid population reduction and reapply treatments if necessary",
                    3,
                    listOf("Magnifying glass (optional)"),
                    listOf(
                        "Inspect new growth and leaf undersides for aphids",
                        "Check for honeydew (sticky residue) or sooty mold",
                        "Look for beneficial insects that may be controlling aphids",
                        "Reapply treatments if aphid populations persist"
                    )
                )
            )
        ),
        "Cutworm (Infested)" to PlantCondition(
            "Cutworm (Infested)",
            "Soil-dwelling caterpillars that cut off plants at the soil line, primarily damaging young seedlings.",
            listOf(
                "Place protective collars around seedlings (toilet paper tubes work well)",
                "Clear garden of debris where cutworms hide",
                "Till soil 2-4 weeks before planting to expose cutworms",
                "Plant after soil has warmed, as cutworms are more active in cool soils",
                "Encourage natural predators like birds and beneficial insects"
            ),
            listOf(
                "Hand-pick cutworms at night when they're active",
                "Apply diatomaceous earth around plant stems",
                "Use Bacillus thuringiensis (Bt) for severe infestations",
                "Create barriers with cardboard collars or aluminum foil",
                "Apply beneficial nematodes to soil to target larvae"
            ),
            listOf(
                TreatmentTask(
                    "Night Inspection",
                    "Inspect plants after dark to locate and remove cutworms",
                    1,
                    listOf("Flashlight", "Container with soapy water"),
                    listOf(
                        "Search soil around damaged plants after sunset",
                        "Look for C-shaped grayish-brown caterpillars",
                        "Check 1-2 inches below soil surface near plant stems",
                        "Drop collected cutworms in soapy water to eliminate them"
                    )
                ),
                TreatmentTask(
                    "Install Protective Collars",
                    "Create physical barriers around plant stems",
                    1,
                    listOf("Cardboard tubes, aluminum foil, or plastic cups"),
                    listOf(
                        "Cut cardboard tubes into 3-inch sections",
                        "Push collar 1 inch into soil around each plant stem",
                        "Extend collar 2 inches above soil surface",
                        "Ensure no gaps between collar and soil"
                    )
                ),
                TreatmentTask(
                    "Apply Diatomaceous Earth",
                    "Create a protective barrier of diatomaceous earth",
                    5,
                    listOf("Food-grade diatomaceous earth", "Duster or shaker container"),
                    listOf(
                        "Apply a 2-inch wide ring around each plant",
                        "Keep material dry - reapply after rain or irrigation",
                        "Avoid breathing dust; wear a mask when applying",
                        "Apply in evening when cutworms become active"
                    )
                ),
                TreatmentTask(
                    "Bt Treatment",
                    "Apply Bacillus thuringiensis for severe infestations",
                    7,
                    listOf("Bt concentrate", "Sprayer", "Protective gear"),
                    listOf(
                        "Mix Bt according to label directions",
                        "Apply to soil around plant stems",
                        "Apply in evening hours when cutworms feed",
                        "Water lightly after application to activate Bt",
                        "Reapply after heavy rain"
                    )
                )
            )
        ),
        "Fruit Fly (Infested)" to PlantCondition(
            "Fruit Fly (Infested)",
            "Small flies that lay eggs in ripening fruits, causing decay and premature fruit drop.",
            listOf(
                "Harvest fruits as soon as they ripen",
                "Remove and dispose of fallen or damaged fruits",
                "Cover developing fruits with paper bags or fruit sleeves",
                "Use yellow sticky traps to monitor populations",
                "Maintain good garden sanitation by removing debris"
            ),
            listOf(
                "Create fruit fly traps with apple cider vinegar or wine",
                "Apply organic spinosad sprays to affected plants",
                "Use floating row covers to protect plants",
                "Apply kaolin clay to create protective barrier on fruits",
                "Introduce parasitic wasps as biological control"
            ),
            listOf(
                TreatmentTask(
                    "Remove Infested Fruits",
                    "Remove all damaged or fallen fruits to break reproductive cycle",
                    1,
                    listOf("Garden gloves", "Sealed disposal bags"),
                    listOf(
                        "Inspect plants for fruits with tiny puncture marks",
                        "Check for fruits with soft spots indicating larval feeding",
                        "Remove all damaged, overripe, or fallen fruits",
                        "Dispose in sealed bags or bury deeply (12+ inches)"
                    )
                ),
                TreatmentTask(
                    "Set Up Traps",
                    "Create traps to capture adult fruit flies",
                    2,
                    listOf("Plastic containers", "Apple cider vinegar", "Dish soap", "Plastic wrap"),
                    listOf(
                        "Fill containers with 1-inch apple cider vinegar",
                        "Add 2-3 drops of dish soap to break surface tension",
                        "Cover with plastic wrap and poke small holes",
                        "Place traps near affected plants but not touching foliage",
                        "Empty and refresh every 3-4 days"
                    )
                ),
                TreatmentTask(
                    "Apply Spinosad",
                    "Treat plants with organic spinosad spray",
                    7,
                    listOf("Spinosad concentrate", "Sprayer", "Protective gear"),
                    listOf(
                        "Mix spinosad according to package directions",
                        "Apply to fruit and foliage, focusing on developing fruits",
                        "Apply in evening to minimize impact on beneficial insects",
                        "Avoid spraying open flowers to protect pollinators",
                        "Reapply after rain or every 7-10 days during peak season"
                    )
                ),
                TreatmentTask(
                    "Protective Covers",
                    "Use physical barriers to protect developing fruits",
                    3,
                    listOf("Paper bags", "Garden ties", "Row cover material"),
                    listOf(
                        "Cover individual fruits with paper bags when 2-3 inches developed",
                        "Secure bags with garden ties or staples",
                        "For smaller plants, use fine mesh row covers",
                        "Ensure covers don't damage plants in wind or rain",
                        "Remove bags just before harvest to allow final ripening"
                    )
                )
            )
        ),
        "Hippodamia Variegata/Lady Bug" to PlantCondition(
            "Hippodamia Variegata/Lady Bug",
            "Beneficial ladybug species that feeds on aphids and other soft-bodied pests. These are beneficial insects that help control pest populations.",
            listOf(
                "Avoid broad-spectrum insecticides that harm beneficial insects",
                "Plant diverse flowering plants to provide nectar and pollen",
                "Create overwintering sites with leaf litter or insect houses",
                "Maintain areas with aphids or other prey to sustain populations",
                "Introduce purchased ladybugs in evening hours with water spray"
            ),
            listOf(
                "No treatment needed - these are beneficial insects",
                "Protect existing populations by avoiding chemical insecticides",
                "Provide water sources during dry periods",
                "Create habitat with diverse plantings and shelter areas",
                "Document populations to track garden ecosystem health"
            ),
            listOf(
                TreatmentTask(
                    "Preserve Populations",
                    "Protect and encourage beneficial ladybug populations",
                    7,
                    listOf("Garden journal", "Water mister"),
                    listOf(
                        "Avoid all insecticide use in areas with ladybugs",
                        "Provide shallow water sources with landing platforms",
                        "Document ladybug populations to track effectiveness",
                        "Photograph larvae to distinguish from pest species"
                    )
                ),
                TreatmentTask(
                    "Create Habitat",
                    "Enhance garden to support ladybug lifecycle",
                    14,
                    listOf("Native plant seeds", "Mulch", "Ladybug house (optional)"),
                    listOf(
                        "Plant diverse flowering plants like dill, fennel, and yarrow",
                        "Create overwintering sites with rock piles or ladybug houses",
                        "Leave some areas of garden unmulched for ground-dwelling species",
                        "Maintain small patches of aphids as food source"
                    )
                ),
                TreatmentTask(
                    "Monitor Effectiveness",
                    "Evaluate pest control provided by ladybug population",
                    7,
                    listOf("Magnifying glass", "Garden journal"),
                    listOf(
                        "Check plants for aphid populations",
                        "Observe and document ladybug feeding behavior",
                        "Count ladybug eggs, larvae, and adults to assess population",
                        "Compare pest damage in areas with and without ladybugs"
                    )
                ),
                TreatmentTask(
                    "Supplemental Release",
                    "Add purchased ladybugs if natural population insufficient",
                    30,
                    listOf("Purchased ladybugs", "Spray bottle", "Organic honey solution"),
                    listOf(
                        "Release ladybugs in evening hours to reduce dispersal",
                        "Mist plants lightly before release",
                        "Place ladybugs at base of infested plants",
                        "Provide honey-water solution (1:10) on cotton balls as food source",
                        "Create temporary mesh cover for 24-48 hours if possible"
                    )
                )
            )
        ),
        "Melon Thrips (Diseased)" to PlantCondition(
            "Melon Thrips (Diseased)",
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
        )
    )
}