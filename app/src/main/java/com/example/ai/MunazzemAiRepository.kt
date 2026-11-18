package com.example.ai

import android.util.Log
import com.example.data.local.AiChatMessageEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "MunazzemAI"

data class ExtractedFixedLesson(
    val subjectName: String,
    val dayOfWeek: Int, // 1=Sunday .. 7=Saturday, 0=Daily/Unspecified
    val startMinutes: Int,
    val durationMinutes: Int
)

data class ParsedDaySchedule(
    val wakeTimeMinutes: Int?,
    val sleepTimeMinutes: Int?,
    val schoolStartMinutes: Int?,
    val schoolEndMinutes: Int?,
    val fixedLessons: List<ExtractedFixedLesson>,
    val freeSlotsSummary: List<String>,
    val conflicts: List<String>,
    val suggestedStudyTips: List<String>,
    val summaryArabic: String
)

class MunazzemAiRepository {

    private val dayParserSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "wakeTimeMinutes" to Schema.integer(
                    description = "Wake up time in minutes from midnight (0..1439), or -1 if NOT explicitly mentioned.",
                    nullable = false
                ),
                "sleepTimeMinutes" to Schema.integer(
                    description = "Sleep time in minutes from midnight (0..1439), or -1 if NOT explicitly mentioned.",
                    nullable = false
                ),
                "schoolStartMinutes" to Schema.integer(
                    description = "School start time in minutes from midnight (0..1439), or -1 if NOT explicitly mentioned.",
                    nullable = false
                ),
                "schoolEndMinutes" to Schema.integer(
                    description = "School return/end time in minutes from midnight (0..1439), or -1 if NOT explicitly mentioned.",
                    nullable = false
                ),
                "fixedLessons" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "subjectName" to Schema.string("Name of the subject/lesson as stated by the student"),
                            "dayOfWeek" to Schema.integer("1=Sunday, 2=Monday, 3=Tuesday, 4=Wednesday, 5=Thursday, 6=Friday, 7=Saturday, 0=Unspecified/Daily"),
                            "startMinutes" to Schema.integer("Start time in minutes from midnight (0..1439)"),
                            "durationMinutes" to Schema.integer("Duration in minutes (default 90 if not stated)")
                        )
                    ),
                    description = "List of private lessons or fixed sessions explicitly mentioned by the student"
                ),
                "freeSlotsSummary" to Schema.array(
                    items = Schema.string(),
                    description = "Arabic summary strings of detected free time windows based strictly on mentioned schedule"
                ),
                "conflicts" to Schema.array(
                    items = Schema.string(),
                    description = "Arabic warnings if any two mentioned times overlap or conflict (empty list if none)"
                ),
                "suggestedStudyTips" to Schema.array(
                    items = Schema.string(),
                    description = "Suggested study windows or advice in Arabic fitting the detected free time"
                ),
                "summaryArabic" to Schema.string("A concise encouraging Arabic summary of the analyzed day")
            )
        )
    }

    private val dayParserModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = dayParserSchema
            },
            systemInstruction = content {
                text(
                    """
                    أنت محلل ذكي لروتين الطالب اليومي والأسبوعي داخل تطبيق MUNAZZEM STUDY PRO.
                    تفهم اللهجة المصرية والعربية الفصحى بطلاقة (مثل: بصحى 6 الصبح، برجع 2 الضهر، يوم الحد الساعة 5، بالليل الساعة 11).
                    قواعد صارمة جداً:
                    1. استخرج المواعيد المذكورة صراحةً فقط في كلام الطالب.
                    2. لا تخترع أو تفترض أي مواعيد غير مذكورة؛ إذا لم يذكر الطالب موعد الاستيقاظ أو النوم أو المدرسة ضع القيمة -1.
                    3. في أوقات المساء مثل "درس الساعة 5" أو "الساعة 7" أو "بنام 11"، حولها إلى دقائق من منتصف الليل بنظام 24 ساعة (مثلاً 5 مساءً = 17:00 = 1020 دقيقة، 11 مساءً = 23:00 = 1380 دقيقة، 6 صباحاً = 360 دقيقة).
                    4. أيام الأسبوع: الأحد=1، الإثنين=2، الثلاثاء=3، الأربعاء=4، الخميس=5، الجمعة=6، السبت=7، غير محدد/يومي=0.
                    5. اكتشف أي تعارض زمني بين المواعيد المذكورة واكتبه بالعربية في conflicts.
                    6. حدد أوقات الفراغ المتاحة واكتبها بوضوح بالعربية في freeSlotsSummary.
                    """.trimIndent()
                )
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    أنت MUNAZZEM AI، المساعد الدراسي الشخصي الذكي داخل تطبيق MUNAZZEM STUDY PRO.
                    شعارنا: "وقتك منظم، ومستقبلك أقرب."
                    تتحدث بالعربية الواضحة والمشجعة وتفهم اللهجة المصرية والفصحى.
                    مهامك: مساعدة الطالب على تنظيم يومه، بناء خطة أسبوعية، توزيع المتراكم دون إرهاق، عمل خطط مراجعة للامتحانات، ترشيح أهم ما يذاكره اليوم، وتحليل مستواه.
                    التزم بالبيانات الفعلية المرفقة لك فقط ولا تخترع درجات أو مواد غير موجودة، وإذا لم تتوفر بيانات الطالب قدّم إرشاداً عملياً واطلب منه إضافة مواده أو تفعيل مشاركة البيانات مع المساعد.
                    نظم إجابتك بنقاط واضحة ومختصرة وعملية.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun parseStudentDayDescription(description: String): Result<ParsedDaySchedule> {
        return try {
            val response = withTimeout(60_000L) {
                dayParserModel.generateContent(description)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("لم يُرجع الذكاء الاصطناعي بيانات."))

            val json = JSONObject(rawJson)
            val wake = json.optInt("wakeTimeMinutes", -1).takeIf { it in 0..1439 }
            val sleep = json.optInt("sleepTimeMinutes", -1).takeIf { it in 0..1439 }
            val schoolStart = json.optInt("schoolStartMinutes", -1).takeIf { it in 0..1439 }
            val schoolEnd = json.optInt("schoolEndMinutes", -1).takeIf { it in 0..1439 }

            val lessonsList = mutableListOf<ExtractedFixedLesson>()
            val lessonsArr = json.optJSONArray("fixedLessons")
            if (lessonsArr != null) {
                for (i in 0 until lessonsArr.length()) {
                    val obj = lessonsArr.getJSONObject(i)
                    val subject = obj.optString("subjectName", "").trim()
                    if (subject.isNotBlank()) {
                        lessonsList.add(
                            ExtractedFixedLesson(
                                subjectName = subject,
                                dayOfWeek = obj.optInt("dayOfWeek", 0).coerceIn(0, 7),
                                startMinutes = obj.optInt("startMinutes", 1020).coerceIn(0, 1439),
                                durationMinutes = obj.optInt("durationMinutes", 90).coerceIn(15, 360)
                            )
                        )
                    }
                }
            }

            fun readStringList(key: String): List<String> {
                val arr = json.optJSONArray(key) ?: return emptyList()
                return (0 until arr.length()).mapNotNull { idx ->
                    arr.optString(idx)?.takeIf { it.isNotBlank() }
                }
            }

            Result.success(
                ParsedDaySchedule(
                    wakeTimeMinutes = wake,
                    sleepTimeMinutes = sleep,
                    schoolStartMinutes = schoolStart,
                    schoolEndMinutes = schoolEnd,
                    fixedLessons = lessonsList,
                    freeSlotsSummary = readStringList("freeSlotsSummary"),
                    conflicts = readStringList("conflicts"),
                    suggestedStudyTips = readStringList("suggestedStudyTips"),
                    summaryArabic = json.optString("summaryArabic", "تم تحليل يومك بنجاح.")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Day parsing timed out", e)
            Result.failure(Exception("انتهت مهلة التحليل، يرجى المحاولة مرة أخرى."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Day parsing failed", e)
            Result.failure(Exception(e.localizedMessage ?: "تعذر تحليل وصف اليوم عبر الذكاء الاصطناعي."))
        }
    }

    fun streamAssistantReply(
        userMessage: String,
        recentHistory: List<AiChatMessageEntity>,
        studentContextSummary: String?
    ): Flow<String> = flow {
        val historyContents = recentHistory.takeLast(8).map { msg ->
            content(if (msg.role == "user") "user" else "model") {
                text(msg.text)
            }
        }
        val chat = chatModel.startChat(history = historyContents)
        val promptWithContext = buildString {
            if (!studentContextSummary.isNullOrBlank()) {
                appendLine("=== بيانات الطالب الفعلية (بموافقته) ===")
                appendLine(studentContextSummary)
                appendLine("======================================")
            } else {
                appendLine("ملاحظة: الطالب لم يفعّل مشاركة بياناته التفصيلية حالياً، أجب بإرشادات عامة مفيدة.")
            }
            appendLine()
            append("طلب الطالب: ")
            append(userMessage)
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(promptWithContext).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("لم يصل رد من المساعد الذكي.")
        }
    }.catch { error ->
        if (error is CancellationException) throw error
        Log.e(TAG, "Assistant stream failed", error)
        throw Exception(
            when (error) {
                is TimeoutCancellationException -> "استغرق المساعد وقتاً أطول من المعتاد، حاول مرة أخرى."
                else -> error.localizedMessage ?: "تعذر الاتصال بالمساعد الذكي."
            }
        )
    }

    suspend fun generateBacklogPlan(
        backlogDetails: String,
        dailyHealthyStudyMinutes: Int
    ): Result<String> {
        return try {
            val prompt = """
                الطالب لديه المهام المتراكمة التالية:
                $backlogDetails
                
                الحد الصحي للمذاكرة الإضافية يومياً هو $dailyHealthyStudyMinutes دقيقة حتى لا يضغط على نومه أو راحته.
                اقترح خطة عملية مقسمة على عدة أيام للتخلص من المتراكم، مع ترتيب الأولويات ونصيحة تحفيزية قصيرة بالعربية.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                chatModel.generateContent(prompt)
            }
            val text = response.text
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("لم يُرجع المساعد خطة."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("انتهت مهلة اقتراح الخطة."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Backlog plan failed", e)
            Result.failure(Exception(e.localizedMessage ?: "تعذر إنشاء خطة المتراكم."))
        }
    }
}
