package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

data class ExtractedTopic(
    val name: String,
    val description: String = ""
)

data class ExtractedUnit(
    val title: String,
    val topics: List<ExtractedTopic>
)

data class ExtractedCourse(
    val courseName: String,
    val courseCode: String = "",
    val units: List<ExtractedUnit>
) {
    val totalTopicsCount: Int
        get() = units.sumOf { it.topics.size }
}

data class SyllabusParseResult(
    val isSuccess: Boolean,
    val courses: List<ExtractedCourse>,
    val rawText: String,
    val message: String
)

object SyllabusPdfParser {

    /**
     * Copies the PDF from content URI to private app storage.
     */
    fun savePdfLocally(context: Context, uri: Uri, originalName: String): File? {
        return try {
            val syllabiDir = File(context.filesDir, "syllabi").apply { mkdirs() }
            val cleanName = originalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(syllabiDir, "${System.currentTimeMillis()}_$cleanName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts text from PDF bytes using stream content analysis and structural extraction.
     */
    fun extractTextFromPdf(context: Context, uri: Uri): String {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return ""
            extractTextFromPdfBytes(bytes)
        } catch (e: Exception) {
            ""
        }
    }

    fun extractTextFromPdfBytes(bytes: ByteArray): String {
        val stringContent = String(bytes, Charsets.ISO_8859_1)
        val extractedLines = mutableListOf<String>()

        // 1. Match Tj text operators: (some text) Tj
        val tjPattern = Pattern.compile("\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)\\s*Tj", Pattern.MULTILINE)
        val tjMatcher = tjPattern.matcher(stringContent)
        while (tjMatcher.find()) {
            val raw = tjMatcher.group(1) ?: ""
            val clean = unescapePdfString(raw).trim()
            if (clean.isNotBlank() && clean.length > 1) {
                extractedLines.add(clean)
            }
        }

        // 2. Match TJ array text operators: [(t)(e)(x)(t)] TJ
        val tjArrayPattern = Pattern.compile("\\[((?:\\([^()\\\\]*(?:\\\\.[^()\\\\]*)*\\)|[0-9.-]+|\\s+)+)\\]\\s*TJ", Pattern.MULTILINE)
        val tjArrayMatcher = tjArrayPattern.matcher(stringContent)
        val innerPattern = Pattern.compile("\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)")
        while (tjArrayMatcher.find()) {
            val arrayBody = tjArrayMatcher.group(1) ?: ""
            val sb = StringBuilder()
            val innerMatcher = innerPattern.matcher(arrayBody)
            while (innerMatcher.find()) {
                val piece = unescapePdfString(innerMatcher.group(1) ?: "")
                sb.append(piece)
            }
            val text = sb.toString().trim()
            if (text.isNotBlank() && text.length > 1) {
                extractedLines.add(text)
            }
        }

        // 3. Fallback: if PDF streams were compressed or binary, check for readable ASCII strings
        if (extractedLines.isEmpty()) {
            val asciiPattern = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9 ,.:;()_/'\"-]{5,}[a-zA-Z0-9]")
            val asciiMatcher = asciiPattern.matcher(stringContent)
            var count = 0
            while (asciiMatcher.find() && count < 200) {
                val candidate = asciiMatcher.group()
                // Ignore PDF internal markers
                if (!candidate.contains("/Font") && !candidate.contains("/Type") && !candidate.contains("endobj")) {
                    extractedLines.add(candidate.trim())
                    count++
                }
            }
        }

        return extractedLines.joinToString("\n")
    }

    private fun unescapePdfString(s: String): String {
        return s.replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    /**
     * Parses the raw extracted text into structured Courses, Units/Chapters, and Topics.
     * Accurately supports:
     * Option A: Complete Semester PDF with multiple courses.
     * Option B: Single Course PDF.
     */
    fun parseSyllabusContent(rawText: String, fallbackFileName: String = ""): SyllabusParseResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return SyllabusParseResult(
                isSuccess = false,
                courses = emptyList(),
                rawText = rawText,
                message = "The PDF contains no readable text or is a scanned image. You can enter or paste the syllabus topics manually."
            )
        }

        // Check if multiple courses exist in the text (Option A: Complete Semester PDF)
        val courseHeaderPattern = Regex(
            "^(?:Course|Subject|Paper)\\s*(?:Name|Code)?\\s*[:\\-]\\s*(.+)|^([A-Z]{2,4}\\s*\\d{3}[A-Z]?)\\s*[:\\-]\\s*(.+)|^([A-Z][a-zA-Z &]{3,35}(?:Engineering|Systems|Structures|Mathematics|Physics|Chemistry|Science|Programming|Algorithms|Design|Networks|Architecture|Database|Intelligence))",
            RegexOption.IGNORE_CASE
        )

        // Split text into course blocks if multi-course indicators exist
        val courseBlocks = mutableListOf<Pair<String, List<String>>>()
        var currentCourseName = ""
        var currentCourseLines = mutableListOf<String>()

        for (line in lines) {
            val courseMatch = courseHeaderPattern.find(line)
            val isNewCourse = courseMatch != null && (
                line.contains("Course", ignoreCase = true) ||
                line.contains("Subject", ignoreCase = true) ||
                line.contains("Syllabus", ignoreCase = true) ||
                line.matches(Regex("^[A-Z]{2,4}\\s*\\d{3}.*"))
            )

            if (isNewCourse) {
                if (currentCourseLines.isNotEmpty() && currentCourseName.isNotBlank()) {
                    courseBlocks.add(currentCourseName to currentCourseLines.toList())
                    currentCourseLines = mutableListOf()
                }
                currentCourseName = extractCourseNameFromLine(line)
            } else {
                currentCourseLines.add(line)
            }
        }

        if (currentCourseLines.isNotEmpty()) {
            val name = if (currentCourseName.isNotBlank()) currentCourseName else deriveFallbackCourseName(fallbackFileName, lines)
            courseBlocks.add(name to currentCourseLines)
        }

        // If no distinct course blocks were separated, treat the entire text as one course (Option B)
        val finalCourses = mutableListOf<ExtractedCourse>()

        if (courseBlocks.size > 1) {
            for ((cName, cLines) in courseBlocks) {
                val units = parseUnitsAndTopicsFromLines(cLines)
                if (units.isNotEmpty()) {
                    val code = extractCourseCode(cName)
                    finalCourses.add(ExtractedCourse(courseName = cleanCourseName(cName), courseCode = code, units = units))
                }
            }
        } else {
            val singleCourseName = if (courseBlocks.isNotEmpty()) courseBlocks.first().first else deriveFallbackCourseName(fallbackFileName, lines)
            val units = parseUnitsAndTopicsFromLines(lines)
            val code = extractCourseCode(singleCourseName)
            finalCourses.add(ExtractedCourse(courseName = cleanCourseName(singleCourseName), courseCode = code, units = units))
        }

        return if (finalCourses.any { it.units.isNotEmpty() }) {
            SyllabusParseResult(
                isSuccess = true,
                courses = finalCourses,
                rawText = rawText,
                message = "Successfully analyzed syllabus: extracted ${finalCourses.size} course(s) and ${finalCourses.sumOf { it.totalTopicsCount }} topic(s)."
            )
        } else {
            SyllabusParseResult(
                isSuccess = false,
                courses = emptyList(),
                rawText = rawText,
                message = "Could not identify structured units or topics. Please verify the syllabus text or input topics manually."
            )
        }
    }

    private fun parseUnitsAndTopicsFromLines(lines: List<String>): List<ExtractedUnit> {
        val units = mutableListOf<ExtractedUnit>()
        val unitPattern = Regex("^(?:Unit|Chapter|Module|Part)\\s*(\\d+|[IVXLCDM]+)[:\\-\\.]?\\s*(.*)", RegexOption.IGNORE_CASE)

        var currentUnitTitle = "Unit 1: Introduction & Fundamentals"
        var currentTopics = mutableListOf<ExtractedTopic>()

        for (line in lines) {
            val unitMatch = unitPattern.find(line)
            if (unitMatch != null) {
                if (currentTopics.isNotEmpty()) {
                    units.add(ExtractedUnit(title = currentUnitTitle, topics = currentTopics.toList()))
                    currentTopics = mutableListOf()
                }
                val rawTitle = line.trim()
                currentUnitTitle = if (rawTitle.length > 40) rawTitle.take(40) else rawTitle
            } else {
                // Topic line extraction
                val candidateTopic = cleanTopicLine(line)
                if (candidateTopic != null && candidateTopic.isNotBlank()) {
                    // Check if line contains comma or semicolon separated list
                    if (candidateTopic.contains(",") && candidateTopic.length < 90) {
                        val subTopics = candidateTopic.split(",").map { it.trim() }.filter { it.length in 3..45 }
                        for (st in subTopics) {
                            if (!currentTopics.any { it.name.equals(st, ignoreCase = true) }) {
                                currentTopics.add(ExtractedTopic(name = st))
                            }
                        }
                    } else if (candidateTopic.length in 3..60) {
                        if (!currentTopics.any { it.name.equals(candidateTopic, ignoreCase = true) }) {
                            currentTopics.add(ExtractedTopic(name = candidateTopic))
                        }
                    }
                }
            }
        }

        if (currentTopics.isNotEmpty()) {
            units.add(ExtractedUnit(title = currentUnitTitle, topics = currentTopics))
        }

        return units
    }

    private fun cleanTopicLine(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.startsWith("Page ") || trimmed.matches(Regex("^\\d+$")) || trimmed.startsWith("http")) {
            return null
        }
        // Remove leading bullets or numbers: "-", "*", "•", "1.", "1.1", "a)"
        return trimmed.replace(Regex("^(?:[\\-\\•\\*\\>]|\\d+[\\.\\)]|[a-zA-Z][\\.\\)])\\s*"), "").trim()
    }

    private fun extractCourseNameFromLine(line: String): String {
        return line.replace(Regex("^(?:Course|Subject|Paper)\\s*(?:Name|Code)?\\s*[:\\-]\\s*", RegexOption.IGNORE_CASE), "")
            .replace("Syllabus", "", ignoreCase = true)
            .trim()
            .take(40)
    }

    private fun cleanCourseName(name: String): String {
        return name.replace(Regex("\\.pdf$", RegexOption.IGNORE_CASE), "")
            .replace("_", " ")
            .trim()
            .ifEmpty { "Semester Course" }
    }

    private fun extractCourseCode(name: String): String {
        val match = Regex("([A-Z]{2,4}\\s*\\d{3}[A-Z]?)").find(name)
        return match?.value ?: ""
    }

    private fun deriveFallbackCourseName(fileName: String, lines: List<String>): String {
        if (fileName.isNotBlank()) {
            val base = fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
            if (base.length in 3..40) return base
        }
        // Look at first few lines
        val firstHeader = lines.take(3).firstOrNull { it.length in 4..35 && !it.contains("Unit", ignoreCase = true) }
        return firstHeader ?: "Course Syllabus"
    }
}
