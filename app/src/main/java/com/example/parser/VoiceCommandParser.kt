package com.example.parser

import com.example.data.CustomerEntity
import com.example.data.ParsedVoiceInput
import com.example.data.TransactionType
import kotlin.math.max

class VoiceCommandParser {

    fun parseArabicCommand(rawText: String, existingCustomers: List<CustomerEntity>): ParsedVoiceInput {
        val cleanText = normalizeArabicText(rawText)

        // 1. Determine Transaction Type (DEBT vs PAYMENT)
        val transactionType = detectTransactionType(cleanText)

        // 2. Extract Amount
        val amount = extractAmount(cleanText)

        // 3. Match or Extract Customer Name
        val customerMatch = findBestCustomerMatch(cleanText, existingCustomers)
        val detectedName = customerMatch.first ?: extractCandidateCustomerName(cleanText)
        val matchedCustomerId = customerMatch.second

        // 4. Extract Description / Notes
        val notes = extractDescription(cleanText, detectedName, amount)

        return ParsedVoiceInput(
            rawText = rawText,
            detectedCustomerName = detectedName,
            matchedCustomerId = matchedCustomerId,
            amount = amount,
            transactionType = transactionType,
            notes = notes,
            confidenceScore = if (matchedCustomerId != null) 0.95f else 0.80f
        )
    }

    private fun detectTransactionType(text: String): TransactionType {
        val paymentKeywords = listOf(
            "واصل", "سدد", "تسديد", "دفعة", "دفع", "قبض", "استلمنا",
            "سلم", "جاب", "له", "سداد", "من حساب", "خصم", "استلام"
        )
        val debtKeywords = listOf(
            "سجل", "تسجيل", "دين", "ديون", "على", "علية", "طلب", "اخذ", "أخذ",
            "باقي", "فاتورة", "شراء", "بضاعة", "لك"
        )

        val paymentScore = paymentKeywords.sumOf { if (text.contains(it)) 1 else 0 as Int }
        val debtScore = debtKeywords.sumOf { if (text.contains(it)) 1 else 0 as Int }

        return if (paymentScore > debtScore) {
            TransactionType.PAYMENT
        } else {
            TransactionType.DEBT
        }
    }

    private fun extractAmount(text: String): Double {
        // First: Check for direct numeric digits (Arabic ٠-٩ or Western 0-9)
        val convertedDigitsText = text.map { char ->
            when (char) {
                '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'
                '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
                else -> char
            }
        }.joinToString("")

        val digitRegex = Regex("\\b\\d+(?:\\.\\d+)?\\b")
        val digitMatches = digitRegex.findAll(convertedDigitsText).toList()
        if (digitMatches.isNotEmpty()) {
            // Find the match that likely represents the transaction amount (usually largest or last number before currency)
            val numbers = digitMatches.mapNotNull { it.value.toDoubleOrNull() }
            if (numbers.isNotEmpty()) {
                return numbers.maxOrNull() ?: numbers.last()
            }
        }

        // Second: Parse spelled Arabic number phrases
        return parseArabicSpelledNumber(convertedDigitsText)
    }

    private fun parseArabicSpelledNumber(text: String): Double {
        var total = 0.0

        // Common Arabic financial words
        val words = text.split(" ", "،", ",")
        var currentChunk = 0.0

        for (word in words) {
            val clean = word.replace("ال", "").trim()
            when {
                clean == "مليون" || clean == "الملايين" -> {
                    total += if (currentChunk == 0.0) 1000000.0 else currentChunk * 1000000.0
                    currentChunk = 0.0
                }
                clean == "الف" || clean == "آلاف" || clean == "الاف" || clean == "الفين" -> {
                    val multiplier = if (clean == "الفين") 2000.0 else 1000.0
                    total += if (currentChunk == 0.0) multiplier else currentChunk * 1000.0
                    currentChunk = 0.0
                }
                clean == "مئة" || clean == "مية" || clean == "مائة" || clean == "مئتان" || clean == "ميتين" -> {
                    currentChunk += if (clean == "مئتان" || clean == "ميتين") 200.0 else 100.0
                }
                clean == "خمسمائة" || clean == "خمسمية" -> currentChunk += 500.0
                clean == "ثلاثمائة" || clean == "ثلاثمية" -> currentChunk += 300.0
                clean == "اربعمائة" || clean == "اربعمية" -> currentChunk += 400.0
                clean == "سبعمائة" || clean == "سبعمية" -> currentChunk += 700.0
                clean == "ثمانمائة" || clean == "ثمانمية" -> currentChunk += 800.0
                clean == "تسعمائة" || clean == "تسعمية" -> currentChunk += 900.0

                clean == "عشرون" || clean == "عشرين" -> currentChunk += 20.0
                clean == "ثلاثون" || clean == "ثلاثين" -> currentChunk += 30.0
                clean == "اربعون" || clean == "اربعين" -> currentChunk += 40.0
                clean == "خمسون" || clean == "خمسين" -> currentChunk += 50.0
                clean == "ستون" || clean == "ستين" -> currentChunk += 60.0
                clean == "سبعون" || clean == "سبعين" -> currentChunk += 70.0
                clean == "ثمانون" || clean == "ثمانين" -> currentChunk += 80.0
                clean == "تسعون" || clean == "تسعين" -> currentChunk += 90.0

                clean == "واحد" || clean == "واحدة" -> currentChunk += 1.0
                clean == "اثنان" || clean == "اثنين" || clean == "ثنتين" -> currentChunk += 2.0
                clean == "ثلاثة" || clean == "ثلاث" -> currentChunk += 3.0
                clean == "اربعة" || clean == "اربع" -> currentChunk += 4.0
                clean == "خمسة" || clean == "خمس" -> currentChunk += 5.0
                clean == "ستة" || clean == "ست" -> currentChunk += 6.0
                clean == "سبعة" || clean == "سبع" -> currentChunk += 7.0
                clean == "ثمانية" || clean == "ثماني" -> currentChunk += 8.0
                clean == "تسعة" || clean == "تسع" -> currentChunk += 9.0
                clean == "عشرة" || clean == "عشر" -> currentChunk += 10.0
            }
        }

        total += currentChunk
        return total
    }

    private fun findBestCustomerMatch(
        text: String,
        existingCustomers: List<CustomerEntity>
    ): Pair<String?, Long?> {
        if (existingCustomers.isEmpty()) return Pair(null, null)

        var bestMatch: CustomerEntity? = null
        var highestScore = 0

        for (customer in existingCustomers) {
            val normalizedCustomerName = normalizeArabicText(customer.name)
            val score = calculateSimilarity(normalizedCustomerName, text)
            if (score > highestScore && score >= 2) {
                highestScore = score
                bestMatch = customer
            }
        }

        return if (bestMatch != null) {
            Pair(bestMatch.name, bestMatch.id)
        } else {
            Pair(null, null)
        }
    }

    private fun calculateSimilarity(customerName: String, fullText: String): Int {
        val nameParts = customerName.split(" ").filter { it.length > 2 }
        var score = 0

        if (fullText.contains(customerName, ignoreCase = true)) {
            return customerName.length * 2
        }

        for (part in nameParts) {
            if (fullText.contains(part, ignoreCase = true)) {
                score += part.length
            }
        }
        return score
    }

    private fun extractCandidateCustomerName(text: String): String {
        // Filter out trigger command words, digits, currencies
        val stopWords = setOf(
            "سجل", "تسجيل", "دين", "على", "علية", "من", "واصل", "سدد", "دفعة", "قيمة",
            "مبلغ", "ريال", "جنيه", "درهم", "دولار", "الف", "آلاف", "الاف", "خمسة",
            "عشرة", "خمسين", "مئة", "مليون", "حساب", "الزبون", "عميل", "دفتر", "لك", "له"
        )

        val words = text.split(" ", "،", ",")
            .map { it.trim() }
            .filter { word ->
                word.length > 1 &&
                        !stopWords.contains(word) &&
                        !word.all { char -> char.isDigit() }
            }

        return if (words.size >= 2) {
            "${words[0]} ${words[1]}"
        } else if (words.isNotEmpty()) {
            words[0]
        } else {
            "زبون جديد"
        }
    }

    private fun extractDescription(text: String, customerName: String, amount: Double): String {
        // Extract remaining context items if available
        val keywordsToRemove = listOf(
            "سجل", "على", "دين", "واصل", "من", "سدد", "دفعة", customerName,
            amount.toInt().toString(), "ريال", "بقيمة", "مبلغ"
        )
        var result = text
        for (kw in keywordsToRemove) {
            if (kw.isNotBlank()) {
                result = result.replace(kw, "", ignoreCase = true)
            }
        }
        val trimmed = result.trim().replace(Regex("\\s+"), " ")
        return if (trimmed.length in 3..50) trimmed else ""
    }

    private fun normalizeArabicText(text: String): String {
        return text
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace(Regex("[\\u064B-\\u0652]"), "") // Remove Arabic diacritics / Harakat
            .trim()
    }
}
