package com.PlantDetection

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

object SupabaseConfig {
    // Replace these with your actual Supabase credentials
//    private const val SUPABASE_URL = "https://snsfnvculzdquloevznj.supabase.co"
//    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNuc2ZudmN1bHpkcXVsb2V2em5qIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTQzOTU5OTUsImV4cCI6MjA2OTk3MTk5NX0.MUHoiUcBCue6jhH29bGUj9F9hVCln8h75UOOAhT2WYg"

    private const val SUPABASE_URL = "https://gndbmbpyylnmkggdvesd.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImduZGJtYnB5eWxubWtnZ2R2ZXNkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTQ3NDQ4NTIsImV4cCI6MjA3MDMyMDg1Mn0.Xsfc7WUcx9FAyEAH3_uAmPlYBrwKiE1lbbBDpsPmCsg"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Storage)
    }

    val storage = client.storage
}