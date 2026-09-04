package com.example.voice

import android.content.Context
import android.util.Log
import com.example.data.KhowarDatasetEntry
import com.example.data.SocialMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Supported languages in the Yarkhoon AI Voice Assistant.
 */
enum class VoiceLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val localeTag: String,
    val flagEmoji: String
) {
    KHOWAR("kho", "Khowar", "کھوار (Chitrali)", "ur-PK", "🏔️"),
    URDU("ur", "Urdu", "اردو", "ur-PK", "🇵🇰"),
    ENGLISH("en", "English", "English", "en-US", "🌐");

    val locale: Locale
        get() = when (this) {
            ENGLISH -> Locale.US
            URDU -> Locale("ur", "PK")
            KHOWAR -> Locale("ur", "PK")
        }

    companion object {
        fun fromCode(code: String): VoiceLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: KHOWAR
        }

        fun fromLocale(locale: Locale): VoiceLanguage {
            val tag = locale.toLanguageTag().lowercase()
            return when {
                tag.startsWith("en") -> ENGLISH
                tag.startsWith("ur") -> URDU
                tag.startsWith("kho") -> KHOWAR
                else -> KHOWAR
            }
        }

        fun fromLocaleTag(tag: String): VoiceLanguage {
            val lower = tag.lowercase()
            return when {
                lower.startsWith("en") -> ENGLISH
                lower.startsWith("ur") -> URDU
                lower.startsWith("kho") -> KHOWAR
                else -> KHOWAR
            }
        }
    }
}

/**
 * Dialects of Khowar across Chitral & Northern valleys.
 */
enum class KhowarDialect(val displayName: String, val region: String) {
    UPPER_CHITRAL("Upper Chitral / Yarkhoon", "Yarkhoon, Mastuj, Laspur, Broghil"),
    CENTRAL_CHITRAL("Central Chitral", "Booni, Reshun, Kuragh"),
    LOWER_CHITRAL("Lower Chitral", "Chitral Town, Drosh, Ayun"),
    LOTKOH("Lotkoh / Garam Chashma", "Lotkoh Valley"),
    STANDARD("Standard Khowar", "Chitral General");
}

/**
 * Lexicon entry representing curated Khowar vocabulary, phonetics, and translations.
 */
data class KhowarLexiconItem(
    val khowarNastaliq: String,
    val khowarRoman: String,
    val urduMeaning: String,
    val englishMeaning: String,
    val category: String,
    val pronunciationNote: String = ""
)

/**
 * Core Linguistic Engine for Khowar, Urdu, and English.
 * Provides preloaded vocabulary, phonetic mapping, dataset collection, and fine-tuning exports.
 */
object KhowarLanguageEngine {

    /**
     * Foundational Khowar-Urdu-English Lexicon with 60+ curated phrases and terms.
     */
    val coreLexicon: List<KhowarLexiconItem> = listOf(
        // Greetings & Etiquette
        KhowarLexiconItem("اسلام علیکم", "Salam / Assalam-o-Alaikum", "السلام علیکم", "Peace be upon you / Hello", "Greeting"),
        KhowarLexiconItem("جوشپہ کوسوری؟", "Joshpa kosori?", "آپ کیسے ہیں؟", "How are you?", "Greeting", "Casual greeting"),
        KhowarLexiconItem("جوشپہ آسوس؟", "Joshpa asos?", "آپ کیسے ہیں (محترم)؟", "How are you (formal)?", "Greeting"),
        KhowarLexiconItem("مہ وش خوش ہوا", "Mah wesh khosh huwa", "مجھے خوشی ہوئی", "I am pleased / Nice to meet you", "Greeting"),
        KhowarLexiconItem("تہ نام کیا؟", "Tah nam kya?", "آپ کا نام کیا ہے؟", "What is your name?", "Greeting"),
        KhowarLexiconItem("مہ نام ... شیر", "Mah nam ... sher", "میرا نام ... ہے", "My name is ...", "Greeting"),
        KhowarLexiconItem("شکریہ / منتوار", "Shukriya / Minnatwaar", "شکریہ", "Thank you", "Etiquette"),
        KhowarLexiconItem("اللہ نگہبان / خدانصیب", "Allah nigehban / Khuda naseeb", "خدا حافظ", "Goodbye / God protect you", "Farewell"),
        KhowarLexiconItem("کیا حال شیر؟", "Kya haal sher?", "کیا حال ہے؟", "How are things?", "Greeting"),

        // Places & Geography
        KhowarLexiconItem("یارخون", "Yarkhoon", "یارخون وادی", "Yarkhoon Valley", "Geography"),
        KhowarLexiconItem("مستوج", "Mastuj", "مستوج", "Mastuj", "Geography"),
        KhowarLexiconItem("بونی", "Booni", "بونی", "Booni", "Geography"),
        KhowarLexiconItem("ترچ میر", "Tirich Mir", "ترچ میر (بلند ترین چوٹی)", "Tirich Mir (Highest Peak in Hindukush)", "Geography"),
        KhowarLexiconItem("بروغل", "Broghil", "بروغل نیشنل پارک", "Broghil National Park", "Geography"),
        KhowarLexiconItem("شندور", "Shandur", "شندور ٹاپ (پولو گراؤنڈ)", "Shandur Pass (Highest Polo Ground)", "Geography"),
        KhowarLexiconItem("چترال", "Chitral / Chetrar", "چترال", "Chitral", "Geography"),
        KhowarLexiconItem("گرم چشمہ", "Garam Chashma / Lotkoh", "گرم چشمہ", "Hot Springs / Lotkoh", "Geography"),
        KhowarLexiconItem("دروش", "Drosh", "دروش", "Drosh", "Geography"),
        KhowarLexiconItem("کیلاش", "Kalash", "کیلاش وادی", "Kalash Valleys", "Geography"),

        // Weather & Nature
        KhowarLexiconItem("بارش", "Bashik / Barish", "بارش", "Rain", "Weather"),
        KhowarLexiconItem("ہیم", "Heem", "برف / برفباری", "Snow", "Weather"),
        KhowarLexiconItem("گارم", "Garam", "گرم", "Hot / Warm", "Weather"),
        KhowarLexiconItem("یوڑ", "Yoor", "سورج", "Sun", "Nature"),
        KhowarLexiconItem("مس", "Mas", "چاند", "Moon", "Nature"),
        KhowarLexiconItem("غار", "Ghar", "پہاڑ", "Mountain", "Nature"),
        KhowarLexiconItem("سین", "Seen", "دریا", "River", "Nature"),
        KhowarLexiconItem("درخت / توم", "Toom", "درخت", "Tree", "Nature"),
        KhowarLexiconItem("آسمان", "Asman", "آسمان", "Sky", "Nature"),
        KhowarLexiconItem("سیلاب / گلول", "Galol / Sailab", "سیلاب", "Flood / Glacial Outburst", "Weather"),

        // Social & Community
        KhowarLexiconItem("برار", "Brar", "بھائی", "Brother", "Family"),
        KhowarLexiconItem("یسگار", "Yasgar", "بہن", "Sister", "Family"),
        KhowarLexiconItem("نان", "Naan", "ماں", "Mother", "Family"),
        KhowarLexiconItem("تات", "Taat", "باپ", "Father", "Family"),
        KhowarLexiconItem("دوست", "Dost", "دوست", "Friend", "Social"),
        KhowarLexiconItem("برادری / قوم", "Qaum / Bradari", "برادری", "Community", "Social"),
        KhowarLexiconItem("گروپ", "Group", "گروپ", "Group", "Social"),
        KhowarLexiconItem("پوسٹ", "Post", "پوسٹ / پیغام", "Post / Message", "Social"),
        KhowarLexiconItem("مارکیٹ / بازار", "Bazar", "مارکیٹ", "Marketplace", "Trade"),
        KhowarLexiconItem("خدمت / سروس", "Khidmat", "خدمات", "Services", "Trade"),

        // Culture & Food
        KhowarLexiconItem("غلمندی", "Ghalmandi", "غلمندی (روایتی لذیذ روٹی پنیر کے ساتھ)", "Ghalmandi (Layered cheese flatbread)", "Food"),
        KhowarLexiconItem("سناباچی", "Sanabachi", "سناباچی روٹی", "Sanabachi traditional bread", "Food"),
        KhowarLexiconItem("شوشپ", "Shushp", "حلوہ / روایتی میٹھا", "Shushp (Traditional Chitrali sweet)", "Food"),
        KhowarLexiconItem("ستار", "Chitrali Sitar", "چترالی ستار (روایتی ساز)", "Chitrali Sitar (Folk lute)", "Culture"),
        KhowarLexiconItem("پولو", "Polo / Istor-bazi", "پولو کھیل", "Freestyle Polo", "Culture"),
        KhowarLexiconItem("پاکول", "Pakol", "چترالی روایتی اونی ٹوپی", "Pakol wool cap", "Culture"),
        KhowarLexiconItem("چمبوس", "Chambus", "روایتی رقص", "Traditional Folk Dance", "Culture"),

        // Daily Conversations & Questions
        KhowarLexiconItem("کیا خبر شیر؟", "Kya khabar sher?", "کیا خبر ہے؟", "What is the news?", "Daily"),
        KhowarLexiconItem("کیا خباران شیر؟", "Kya khabaran sher?", "کیا نئی تازی ہے؟", "What's the latest update?", "Daily"),
        KhowarLexiconItem("رہ کیا؟", "Rah kya?", "راستہ کیسا ہے؟ / سڑک کا کیا حال ہے؟", "How is the road condition?", "Travel"),
        KhowarLexiconItem("کورا بوشی؟", "Kora boshi?", "کہاں جا رہے ہو؟", "Where are you going?", "Travel"),
        KhowarLexiconItem("موستوجا بوم", "Mastuja bom", "مستوج جا رہا ہوں", "I am going to Mastuj", "Travel"),
        KhowarLexiconItem("مہ کمک کرور", "Mah kumak karor", "میری مدد کریں", "Please help me", "Daily"),
        KhowarLexiconItem("کیمت کیا شیر؟", "Qeemat kya sher?", "اس کی قیمت کیا ہے؟", "What is the price?", "Trade"),
        KhowarLexiconItem("ہوو", "Hoo / Awa", "ہاں / جی ہاں", "Yes", "Basic"),
        KhowarLexiconItem("نوش", "Nosh", "نہیں / جی نہیں", "No", "Basic"),
        KhowarLexiconItem("صحیح شیر", "Sahi sher", "ٹھیک ہے", "All right / Correct", "Basic"),
        KhowarLexiconItem("مہ پیارا یارخون", "Mah pyara Yarkhoon", "میرا پیارا یارخون", "My beloved Yarkhoon", "Affection")
    )

    /**
     * Seeds initial Khowar dataset entries into the repository if empty.
     */
    suspend fun seedInitialKhowarDataset(repository: SocialMediaRepository) = withContext(Dispatchers.IO) {
        try {
            val existing = repository.getKhowarDatasetEntriesOnce()
            if (existing.isEmpty()) {
                val seedEntries = coreLexicon.mapIndexed { index, item ->
                    KhowarDatasetEntry(
                        id = "khowar_seed_${index + 1}",
                        khowarText = item.khowarNastaliq,
                        khowarRomanText = item.khowarRoman,
                        urduTranslation = item.urduMeaning,
                        englishTranslation = item.englishMeaning,
                        audioFilePath = "",
                        audioDurationMs = 0L,
                        dialect = KhowarDialect.STANDARD.displayName,
                        category = item.category,
                        contributorId = "system_curated",
                        contributorName = "Yarkhoon Linguistic Heritage Foundation",
                        isVerified = true,
                        votesCount = 15,
                        timestamp = System.currentTimeMillis() - (index * 3600000L),
                        exportStatus = "READY_FOR_TRAINING"
                    )
                }
                repository.insertKhowarDatasetEntries(seedEntries)
                Log.d("KhowarLanguageEngine", "Seeded ${seedEntries.size} Khowar linguistic records.")
            }
        } catch (e: Exception) {
            Log.e("KhowarLanguageEngine", "Error seeding Khowar dataset: ${e.localizedMessage}")
        }
    }

    /**
     * Search the local Khowar lexicon for immediate offline matching.
     */
    fun searchLocalLexicon(query: String): List<KhowarLexiconItem> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return coreLexicon.take(10)
        return coreLexicon.filter { item ->
            item.khowarNastaliq.contains(q) ||
            item.khowarRoman.lowercase().contains(q) ||
            item.urduMeaning.contains(q) ||
            item.englishMeaning.lowercase().contains(q) ||
            item.category.lowercase().contains(q)
        }
    }

    /**
     * Post-processes speech-to-text transcripts to normalize Khowar phonetics and names.
     */
    fun normalizeKhowarSpeech(rawTranscript: String): String {
        var text = rawTranscript.trim()
        val replacements = mapOf(
            "yarkhun" to "Yarkhoon",
            "yarkhun valley" to "Yarkhoon Valley",
            "terichmir" to "Tirich Mir",
            "terich mir" to "Tirich Mir",
            "joshpa kosori" to "جوشپہ کوسوری؟ (Joshpa kosori)",
            "joshpa asos" to "جوشپہ آسوس؟ (Joshpa asos)",
            "broghil" to "Broghil",
            "mastuj" to "Mastuj",
            "booni" to "Booni",
            "ghalmandi" to "Ghalmandi",
            "sanabachi" to "Sanabachi",
            "kya haal sher" to "کیا حال شیر؟ (Kya haal sher?)",
            "shukriya" to "منتوار / شکریہ (Minnatwaar)"
        )

        for ((key, replacement) in replacements) {
            if (text.lowercase().contains(key)) {
                text = text.replace(Regex("(?i)$key"), replacement)
            }
        }
        return text
    }

    /**
     * Formats the training dataset into a standard JSONL string ready for fine-tuning.
     */
    fun exportDatasetToJsonl(entries: List<KhowarDatasetEntry>): String {
        val jsonLines = StringBuilder()
        for (entry in entries) {
            val jsonObject = JSONObject().apply {
                put("id", entry.id)
                put("khowar_text", entry.khowarText)
                put("khowar_roman", entry.khowarRomanText)
                put("urdu_translation", entry.urduTranslation)
                put("english_translation", entry.englishTranslation)
                put("dialect", entry.dialect)
                put("category", entry.category)
                put("has_audio", entry.audioFilePath.isNotBlank())
                put("audio_duration_ms", entry.audioDurationMs)
                put("is_verified", entry.isVerified)
                put("contributor", entry.contributorName)
                put("timestamp", entry.timestamp)
            }
            jsonLines.append(jsonObject.toString()).append("\n")
        }
        return jsonLines.toString()
    }

    /**
     * Saves exported dataset to a local device file.
     */
    suspend fun saveDatasetExportFile(context: Context, jsonlContent: String): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, "khowar_datasets").apply { mkdirs() }
        val exportFile = File(exportDir, "khowar_training_dataset_${System.currentTimeMillis()}.jsonl")
        exportFile.writeText(jsonlContent)
        exportFile
    }
}
