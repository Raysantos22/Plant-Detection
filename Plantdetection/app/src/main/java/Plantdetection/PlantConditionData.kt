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
        // Existing conditions
        "Aphids (Infested)" to PlantCondition(
            "Aphids",
            "Small sap-sucking insects that cluster on stems and the undersides of leaves, causing leaf curl and stunted growth.",
            listOf(
                "Encourage beneficial insects like ladybugs and lacewings",
                "Use reflective mulch to deter aphids",
                "Plant companion plants like marigolds or nasturtiums",
                "Avoid excessive nitrogen fertilization",
                "Use preventative neem oil sprays"
            ),
            listOf(
                "Spray plants with strong water jet to dislodge aphids",
                "Apply insecticidal soap or neem oil to affected areas",
                "Introduce beneficial insects like ladybugs",
                "Apply horticultural oil to smother aphids",
                "For severe infestations, consider systemic insecticides"
            ),
            listOf(
                TreatmentTask(
                    "Water Spray Treatment",
                    "Use strong water spray to dislodge aphids from plants",
                    2,
                    listOf("Garden hose with spray nozzle"),
                    listOf(
                        "Adjust hose to strong spray setting",
                        "Target aphid colonies on undersides of leaves",
                        "Spray in morning so plants can dry quickly",
                        "Be thorough but careful not to damage plants",
                        "Repeat every 2-3 days for at least a week"
                    )
                ),
                TreatmentTask(
                    "Apply Insecticidal Soap",
                    "Apply insecticidal soap for direct control",
                    5,
                    listOf("Insecticidal soap", "Spray bottle"),
                    listOf(
                        "Mix soap according to package directions",
                        "Apply directly to aphid colonies",
                        "Cover all plant surfaces, especially leaf undersides",
                        "Apply in early morning or evening, not during hot sun",
                        "Repeat every 5-7 days for 2-3 applications"
                    )
                ),
                TreatmentTask(
                    "Apply Neem Oil",
                    "Use neem oil as a natural insecticide and repellent",
                    7,
                    listOf("Neem oil", "Sprayer", "Dish soap (as emulsifier)"),
                    listOf(
                        "Mix 2 tsp neem oil with 1 tsp mild dish soap in 1 quart water",
                        "Shake well and apply to all plant surfaces",
                        "Focus on undersides of leaves where aphids hide",
                        "Apply in early morning or evening, avoiding hot sun",
                        "Repeat weekly for 2-3 weeks"
                    )
                ),
                TreatmentTask(
                    "Introduce Beneficial Insects",
                    "Release ladybugs or lacewings to control aphid population",
                    14,
                    listOf("Purchased ladybugs or lacewing eggs", "Spray bottle with water"),
                    listOf(
                        "Release beneficial insects in evening or early morning",
                        "Lightly mist plants with water before release",
                        "Place insects near aphid colonies",
                        "Provide water source (shallow dish with stones)",
                        "Avoid applying insecticides after release"
                    )
                )
            )
        ),
        "Cutworm (Infested)" to PlantCondition(
            "Cutworm",
            "Caterpillars that cut young plants off at soil level, often feeding at night and hiding in soil during the day.",
            listOf(
                "Place protective collars around seedlings",
                "Clear garden bed of debris before planting",
                "Till soil 2-3 weeks before planting",
                "Encourage natural predators like birds and ground beetles",
                "Plant marigolds as a deterrent"
            ),
            listOf(
                "Handpick cutworms at night with flashlight",
                "Apply diatomaceous earth around plant stems",
                "Use Bacillus thuringiensis (Bt) for biological control",
                "Apply nematodes to soil to target larvae",
                "Create barriers with cardboard collars or aluminum foil"
            ),
            listOf(
                TreatmentTask(
                    "Install Protective Collars",
                    "Place protective barriers around plant stems",
                    1,
                    listOf("Cardboard tubes or aluminum foil", "Scissors", "Small stakes (optional)"),
                    listOf(
                        "Cut cardboard tubes to 2-3 inches in height",
                        "Wrap around plant stems, pushing 1 inch into soil",
                        "Ensure collar extends about 2 inches above soil",
                        "For aluminum foil, wrap loosely around stem and extend into soil",
                        "Secure with small stakes if needed"
                    )
                ),
                TreatmentTask(
                    "Apply Diatomaceous Earth",
                    "Create a protective barrier around plants",
                    7,
                    listOf("Food-grade diatomaceous earth", "Applicator or shaker"),
                    listOf(
                        "Apply a ring of DE around each plant, 2-3 inches from stem",
                        "Create a complete circle about 1/4 inch thick",
                        "Apply when soil is dry for best effectiveness",
                        "Reapply after rainfall or heavy watering",
                        "Avoid breathing dust by wearing a mask during application"
                    )
                ),
                TreatmentTask(
                    "Apply Bt Treatment",
                    "Use Bacillus thuringiensis for biological control",
                    7,
                    listOf("Bt spray or powder", "Sprayer or applicator"),
                    listOf(
                        "Mix Bt according to package directions",
                        "Apply to soil around plant bases and lower stems",
                        "Apply in evening for best results",
                        "Water lightly after application",
                        "Repeat every 7-10 days if cutworm activity continues"
                    )
                ),
                TreatmentTask(
                    "Night Inspection",
                    "Manually remove cutworms during their active hours",
                    3,
                    listOf("Flashlight", "Gloves", "Container with soapy water"),
                    listOf(
                        "Search for cutworms after dark with flashlight",
                        "Check soil around damaged plants",
                        "Look for C-shaped grayish or brown caterpillars",
                        "Hand pick and drop into soapy water",
                        "Mark affected areas for follow-up treatment"
                    )
                )
            )
        ),
        "Fruit Fly (Infested)" to PlantCondition(
            "Fruit Fly",
            "Small flies that lay eggs in ripening fruits, leading to maggot infestations and fruit rot.",
            listOf(
                "Harvest fruits as soon as they ripen",
                "Remove fallen or damaged fruits promptly",
                "Use fruit fly traps around garden perimeter",
                "Cover fruits with protective bags or netting",
                "Maintain garden sanitation by removing debris"
            ),
            listOf(
                "Set up vinegar and soap traps to catch adult flies",
                "Use sticky traps near fruiting plants",
                "Apply organic fruit fly bait spray around plants",
                "Use protective netting or bags for individual fruits",
                "Remove and destroy infested fruits immediately"
            ),
            listOf(
                TreatmentTask(
                    "Set Up DIY Traps",
                    "Create and place fruit fly traps around plants",
                    3,
                    listOf("Small containers", "Apple cider vinegar", "Dish soap", "Plastic wrap", "Toothpick"),
                    listOf(
                        "Fill containers 1/3 full with apple cider vinegar",
                        "Add a few drops of dish soap to break surface tension",
                        "Cover with plastic wrap and secure with rubber band",
                        "Poke small holes in plastic wrap with toothpick",
                        "Place traps around fruiting plants and check/replace weekly"
                    )
                ),
                TreatmentTask(
                    "Apply Fruit Fly Bait",
                    "Apply organic fruit fly bait to attract and kill adults",
                    7,
                    listOf("Organic fruit fly bait/spray", "Sprayer", "Protective gloves"),
                    listOf(
                        "Mix bait according to package instructions",
                        "Apply to non-fruiting parts of plants and nearby surfaces",
                        "Focus on areas where fruit flies congregate",
                        "Apply in morning and avoid spraying fruits directly",
                        "Reapply every 7-10 days or after rainfall"
                    )
                ),
                TreatmentTask(
                    "Protective Covering",
                    "Cover developing fruits to prevent egg-laying",
                    1,
                    listOf("Fine mesh bags or netting", "Garden ties or twist ties"),
                    listOf(
                        "Select developing fruits before fruit fly infestation",
                        "Place mesh bag over fruit or fruit cluster",
                        "Secure opening around stem with tie",
                        "Ensure covering is secure but not restricting growth",
                        "Check regularly and adjust as fruits grow"
                    )
                ),
                TreatmentTask(
                    "Sanitation Measures",
                    "Remove potential breeding sites and infested fruits",
                    2,
                    listOf("Garden gloves", "Sealed disposal bags"),
                    listOf(
                        "Inspect all fruits on plants for signs of infestation",
                        "Remove any fruits showing soft spots or small holes",
                        "Pick up all fallen fruits from ground",
                        "Place collected fruits in sealed bags for disposal",
                        "Do not compost infested fruits"
                    )
                )
            )
        ),
        "Hippodamia Variegata/Lady Bug" to PlantCondition(
            "Hippodamia Variegata/Lady Bug",
            "Beneficial insects that prey on aphids, mites, and other garden pests. These spotted beetles help control pest populations naturally.",
            listOf(
                "Plant diverse flowering plants to attract lady bugs",
                "Avoid using broad-spectrum insecticides",
                "Provide water sources in the garden",
                "Plant dill, fennel, or yarrow as attractant plants",
                "Create overwintering sites with piles of leaves or garden debris"
            ),
            listOf(
                "Lady bugs are beneficial - no treatment needed",
                "Encourage them to stay by providing food sources",
                "Release commercially purchased lady bugs at dusk",
                "Spray plants with sugar water solution to attract them",
                "Avoid insecticides that could harm beneficial insects"
            ),
            listOf(
                TreatmentTask(
                    "Attract More Lady Bugs",
                    "Create an environment attractive to lady bugs",
                    14,
                    listOf("Mixed flower seeds", "Shallow water dish", "Small stones"),
                    listOf(
                        "Plant diverse nectar-producing flowers throughout garden",
                        "Create shallow water source with stones for safe landing",
                        "Allow some aphids to remain as food source",
                        "Plant dill, fennel, cilantro, or yarrow nearby",
                        "Avoid using any pesticides in the area"
                    )
                ),
                TreatmentTask(
                    "Release Purchased Lady Bugs",
                    "Properly introduce purchased lady bugs to your garden",
                    1,
                    listOf("Purchased lady bugs", "Spray bottle with water"),
                    listOf(
                        "Keep lady bugs refrigerated until release time",
                        "Release in evening or early morning when cool",
                        "Mist plants with water before release",
                        "Place lady bugs at base of plants with aphids",
                        "Release near flowers for nectar source"
                    )
                ),
                TreatmentTask(
                    "Create Sugar Water Attractant",
                    "Make supplemental food to keep lady bugs in garden",
                    5,
                    listOf("Sugar", "Spray bottle", "Water"),
                    listOf(
                        "Mix 1 tablespoon sugar in 1 quart of water",
                        "Shake until dissolved",
                        "Lightly mist plants in evening",
                        "Focus on areas with pest problems",
                        "Reapply every 5-7 days"
                    )
                ),
                TreatmentTask(
                    "Create Overwintering Sites",
                    "Provide places for lady bugs to hibernate through winter",
                    30,
                    listOf("Fallen leaves", "Small wooden box with slits", "Straw"),
                    listOf(
                        "Create small piles of leaves in protected garden areas",
                        "Place wooden box with slits in sheltered location",
                        "Fill box with straw or dried leaves",
                        "Position near plants that commonly have aphids",
                        "Avoid disturbing these areas during winter"
                    )
                )
            )
        ),
        "Blossom End Rot (Tomato) (Diseased)" to PlantCondition(
            "Blossom End Rot (Tomato)",
            "A physiological disorder in tomatoes caused by calcium deficiency, resulting in dark, sunken areas at the blossom end of fruits.",
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
        "Melon Thrips (Eggplant) (Diseased)" to PlantCondition(
            "Melon Thrips (Eggplant)",
            "Tiny insects that damage eggplants by sucking plant sap, causing silvering, scarring, and distortion of leaves.",
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
                "Remove and destroy heavily infested leaves",
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
        "Anthracnose (Chili Pepper) (Diseased)" to PlantCondition(
            "Anthracnose (Chili Pepper)",
            "A fungal disease that causes dark, sunken lesions on chili pepper fruits, stems, and leaves.",
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
        "Phytophthora Fruit Rot (Bitter Gourd) (Diseased)" to PlantCondition(
            "Phytophthora Fruit Rot (Bitter Gourd)",
            "A water mold disease causing water-soaked lesions on bitter gourd fruits that develop into white, fluffy fungal growth.",
            listOf(
                "Plant on raised beds with good drainage",
                "Rotate crops for at least 3 years",
                "Use fungicide-treated seeds",
                "Avoid overhead irrigation",
                "Use drip irrigation and mulch to prevent soil splash"
            ),
            listOf(
                "Remove and destroy all infected fruits and plant parts",
                "Apply phosphorous acid or metalaxyl-based fungicides",
                "Improve drainage around plants",
                "Avoid working with plants when they're wet",
                "Apply copper-based fungicides preventatively"
            ),
            listOf(
                TreatmentTask(
                    "Remove Infected Fruits",
                    "Remove all infected fruits and plant parts to prevent spread",
                    1,
                    listOf("Pruning shears", "Disposal bags", "Disinfectant"),
                    listOf(
                        "Identify fruits with water-soaked lesions or white fungal growth",
                        "Cut off infected fruits with clean pruning shears",
                        "Remove any infected leaves or stems",
                        "Dispose of infected material in sealed bags (do not compost)",
                        "Disinfect all tools after use with 10% bleach solution"
                    )
                ),
                TreatmentTask(
                    "Apply Fungicide Treatment",
                    "Apply appropriate fungicide to prevent spread",
                    7,
                    listOf("Phosphorous acid or metalaxyl-based fungicide", "Sprayer", "Protective gear"),
                    listOf(
                        "Mix fungicide according to label instructions",
                        "Apply thoroughly to all plant parts, especially fruits",
                        "Apply in morning so plants can dry before evening",
                        "Avoid watering for 24 hours after application",
                        "Repeat application according to label, typically every 7-14 days"
                    )
                ),
                TreatmentTask(
                    "Improve Drainage",
                    "Enhance drainage around plants to reduce moisture",
                    3,
                    listOf("Garden fork", "Organic matter or sand", "Mulch"),
                    listOf(
                        "Create drainage channels away from plants if water pools",
                        "If soil is heavy, add organic matter or sand to improve structure",
                        "Form soil into mounds around plant bases",
                        "Apply 2-3 inches of mulch keeping it away from stems",
                        "Consider adding agricultural gypsum to improve soil structure"
                    )
                ),
                TreatmentTask(
                    "Preventative Spraying",
                    "Apply copper-based fungicide to protect remaining fruits",
                    10,
                    listOf("Copper-based fungicide", "Sprayer", "Protective gear"),
                    listOf(
                        "Mix copper fungicide according to label directions",
                        "Apply to all plant surfaces as a preventative measure",
                        "Focus on fruits and stems",
                        "Apply in morning or evening, not during hot sun",
                        "Repeat every 10-14 days until harvest"
                    )
                )
            )
        ),
        "Blossom Blight (Okra) (Diseased)" to PlantCondition(
            "Blossom Blight (Okra)",
            "A fungal disease affecting okra blossoms, causing flowers to develop brown spots, wilt, and fail to produce fruits.",
            listOf(
                "Provide adequate plant spacing for good air circulation",
                "Avoid overhead watering; use drip irrigation",
                "Rotate crops yearly",
                "Remove plant debris after harvest",
                "Apply preventative fungicides during flowering"
            ),
            listOf(
                "Remove and destroy infected blossoms and fruits",
                "Apply fungicides containing chlorothalonil or mancozeb",
                "Improve air circulation by pruning surrounding vegetation",
                "Avoid working with plants when wet",
                "Apply copper-based fungicide as a preventative"
            ),
            listOf(
                TreatmentTask(
                    "Remove Infected Blossoms",
                    "Prune all infected blossoms and affected parts",
                    2,
                    listOf("Pruning shears", "Disposal bag", "Disinfectant"),
                    listOf(
                        "Identify blossoms with brown spots or wilting",
                        "Cut off all infected flowers and any affected fruits",
                        "Prune any stems showing signs of infection",
                        "Dispose of plant material in sealed bags (do not compost)",
                        "Disinfect tools between cuts with 10% bleach solution"
                    )
                ),
                TreatmentTask(
                    "Apply Fungicide",
                    "Treat with appropriate fungicide to halt disease spread",
                    7,
                    listOf("Chlorothalonil or mancozeb fungicide", "Sprayer", "Protective gear"),
                    listOf(
                        "Mix fungicide according to label instructions",
                        "Apply to all plant parts, focusing on blossoms and buds",
                        "Apply in early morning or evening when pollinators are less active",
                        "Ensure thorough coverage of plant surfaces",
                        "Repeat every 7-10 days during flowering period"
                    )
                ),
                TreatmentTask(
                    "Improve Plant Environment",
                    "Enhance growing conditions to discourage fungal development",
                    3,
                    listOf("Pruning shears", "Mulch", "Drip irrigation supplies (optional)"),
                    listOf(
                        "Thin plants if overcrowded to improve air circulation",
                        "Remove surrounding weeds that may increase humidity",
                        "Apply mulch to prevent soil splash onto plants",
                        "Switch to drip irrigation if using overhead watering",
                        "Support plants with stakes if they're falling over"
                    )
                ),
                TreatmentTask(
                    "Preventative Protection",
                    "Apply copper-based fungicide to protect new flowers",
                    10,
                    listOf("Copper-based fungicide", "Sprayer", "Protective gear"),
                    listOf(
                        "Mix copper fungicide according to label directions",
                        "Apply to all plant surfaces focusing on new growth",
                        "Apply early in day to allow drying before evening",
                        "Avoid application during flowering time if possible",
                        "Repeat every 10-14 days as a preventative measure"
                    )
                )
            )
        ),

        // New healthy plant conditions
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
        "Healthy okra" to PlantCondition(
            "Healthy Okra",
            "Your okra plants are healthy! Here are some tips to maintain their health and productivity.",
            listOf(
                "Water consistently, especially during hot weather",
                "Apply balanced organic fertilizer every 3-4 weeks",
                "Harvest pods frequently to encourage production",
                "Provide full sun exposure",
                "Mulch to retain moisture and suppress weeds"
            ),
            listOf(
                "Continue regular maintenance",
                "Monitor for early signs of pests or disease",
                "Harvest pods when 2-4 inches long for best texture",
                "Keep the area around plants weed-free",
                "Prune any yellow or diseased leaves"
            ),
            listOf(
                TreatmentTask(
                    "Regular Watering",
                    "Maintain consistent watering schedule",
                    2,
                    listOf("Watering can or drip irrigation"),
                    listOf(
                        "Water deeply at the base of plants",
                        "Provide 1-1.5 inches of water weekly",
                        "Water more frequently during hot, dry periods",
                        "Water in morning to reduce disease risk"
                    )
                ),
                TreatmentTask(
                    "Fertilize",
                    "Apply balanced fertilizer to maintain productivity",
                    21,
                    listOf("Balanced organic fertilizer", "Compost"),
                    listOf(
                        "Apply fertilizer every 3-4 weeks during growing season",
                        "Side-dress with compost or aged manure",
                        "Avoid excessive nitrogen which promotes foliage over pods",
                        "Water thoroughly after application"
                    )
                ),
                TreatmentTask(
                    "Regular Harvesting",
                    "Harvest pods frequently to encourage continued production",
                    2,
                    listOf("Garden gloves", "Pruning shears or scissors"),
                    listOf(
                        "Check plants every 1-2 days for harvestable pods",
                        "Harvest pods when 2-4 inches long",
                        "Cut rather than pull pods to avoid damaging plants",
                        "Remove any overripe pods to encourage new growth"
                    )
                ),
                TreatmentTask(
                    "Preventative Maintenance",
                    "Monitor for early signs of pests or disease",
                    5,
                    listOf("Magnifying glass (optional)", "Neem oil", "Garden journal"),
                    listOf(
                        "Inspect plants for insect damage or discoloration",
                        "Check undersides of leaves for pests",
                        "Apply neem oil preventatively if pests are common in your area",
                        "Prune any yellow or diseased leaves"
                    )
                )
            )
        ),
        "Healthy bitter gourd" to PlantCondition(
            "Healthy Bitter Gourd",
            "Your bitter gourd (bitter melon) plants are healthy! Here are some tips to maintain their vigor and productivity.",
            listOf(
                "Provide strong trellising or support for vines",
                "Water consistently at soil level",
                "Apply balanced fertilizer every 3-4 weeks",
                "Ensure full sun exposure for best growth",
                "Mulch to retain moisture and reduce weed competition"
            ),
            listOf(
                "Continue regular maintenance",
                "Monitor for pests like aphids and fruit flies",
                "Harvest fruits when they're light green (before yellowing)",
                "Prune to maintain airflow and manage plant size",
                "Provide adequate spacing between plants"
            ),
            listOf(
                TreatmentTask(
                    "Trellising and Support",
                    "Maintain strong support system for climbing vines",
                    7,
                    listOf("Trellis materials", "Garden twine", "Soft plant ties"),
                    listOf(
                        "Check and reinforce existing trellis structure",
                        "Guide new growth onto supports",
                        "Secure vines gently with soft ties",
                        "Ensure the weight of developing fruits is supported"
                    )
                ),
                TreatmentTask(
                    "Regular Watering",
                    "Maintain consistent moisture for optimal growth",
                    2,
                    listOf("Watering can or drip irrigation"),
                    listOf(
                        "Water deeply at soil level, avoiding foliage",
                        "Maintain even soil moisture without waterlogging",
                        "Provide approximately 1-1.5 inches of water weekly",
                        "Adjust frequency based on weather conditions"
                    )
                ),
                TreatmentTask(
                    "Fertilization",
                    "Apply balanced fertilizer to support fruit production",
                    21,
                    listOf("Balanced fertilizer (10-10-10)", "Compost tea (optional)"),
                    listOf(
                        "Apply fertilizer according to package directions",
                        "Side-dress plants with compost for slow-release nutrition",
                        "Apply liquid fertilizer or compost tea every 3-4 weeks",
                        "Reduce nitrogen when fruits begin developing"
                    )
                ),
                TreatmentTask(
                    "Pruning and Maintenance",
                    "Prune to maintain plant health and productivity",
                    10,
                    listOf("Pruning shears", "Garden gloves", "Disinfectant"),
                    listOf(
                        "Remove any yellowed or diseased leaves",
                        "Prune excessive lateral growth to improve air circulation",
                        "Trim overgrown vines to manageable length",
                        "Disinfect tools between plants to prevent disease spread"
                    )
                )
            )
        ),
        "Healthy Chili Pepper" to PlantCondition(
            "Healthy Chili Pepper",
            "Your chili pepper plants are healthy! Here are some tips to maintain their vigor and productivity.",
            listOf(
                "Water deeply but allow soil to dry between waterings",
                "Apply balanced fertilizer every 4-6 weeks",
                "Provide full sun exposure",
                "Mulch to retain moisture and suppress weeds",
                "Support taller varieties with stakes"
            ),
            listOf(
                "Continue regular maintenance",
                "Monitor for common pests like aphids and spider mites",
                "Harvest peppers regularly to encourage production",
                "Prune to improve air circulation if plants become dense",
                "Apply calcium to prevent blossom end rot"
            ),
            listOf(
                TreatmentTask(
                    "Strategic Watering",
                    "Maintain proper watering schedule for optimal growth",
                    3,
                    listOf("Watering can or drip irrigation"),
                    listOf(
                        "Water deeply at soil level, not on foliage",
                        "Allow top inch of soil to dry between waterings",
                        "Reduce watering frequency during fruit ripening for spicier peppers",
                        "Maintain consistent moisture to prevent blossom drop"
                    )
                ),
                TreatmentTask(
                    "Fertilization",
                    "Apply appropriate fertilizer for pepper production",
                    28,
                    listOf("Balanced fertilizer", "Calcium supplement"),
                    listOf(
                        "Apply balanced fertilizer (10-10-10) during vegetative growth",
                        "Switch to lower nitrogen fertilizer when flowering begins",
                        "Apply calcium supplement to prevent blossom end rot",
                        "Water thoroughly after fertilizer application"
                    )
                ),
                TreatmentTask(
                    "Support and Pruning",
                    "Provide structural support and selective pruning",
                    14,
                    listOf("Plant stakes", "Soft plant ties", "Pruning shears"),
                    listOf(
                        "Stake taller varieties to prevent breaking under fruit weight",
                        "Remove lower leaves that touch the soil",
                        "Prune interior branches selectively to improve air flow",
                        "Thin excessive fruit clusters on smaller varieties"
                    )
                ),
                TreatmentTask(
                    "Pest Monitoring",
                    "Regular inspection for early pest detection",
                    5,
                    listOf("Magnifying glass (optional)", "Organic insecticidal soap"),
                    listOf(
                        "Check undersides of leaves for aphids and spider mites",
                        "Inspect growing tips for damage or distortion",
                        "Look for speckling on leaves (sign of mite damage)",
                        "Apply insecticidal soap at first sign of pests"
                    )
                )
            )
        )
    )
}