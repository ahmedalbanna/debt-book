package com.example

import com.example.data.CustomerEntity
import com.example.data.TransactionType
import com.example.parser.VoiceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandParserTest {

    private val parser = VoiceCommandParser()
    private val existingCustomers = listOf(
        CustomerEntity(id = 1, name = "محمد عبده"),
        CustomerEntity(id = 2, name = "عبدالكريم العلي"),
        CustomerEntity(id = 3, name = "فاطمة الزهراء")
    )

    @Test
    fun `test parse debt voice command`() {
        val result = parser.parseArabicCommand("سجل على محمد عبده دين 25000", existingCustomers)

        assertEquals("محمد عبده", result.detectedCustomerName)
        assertEquals(1L, result.matchedCustomerId)
        assertEquals(25000.0, result.amount, 0.01)
        assertEquals(TransactionType.DEBT, result.transactionType)
    }

    @Test
    fun `test parse payment voice command`() {
        val result = parser.parseArabicCommand("واصل من عبدالكريم 5000", existingCustomers)

        assertEquals("عبدالكريم العلي", result.detectedCustomerName)
        assertEquals(2L, result.matchedCustomerId)
        assertEquals(5000.0, result.amount, 0.01)
        assertEquals(TransactionType.PAYMENT, result.transactionType)
    }

    @Test
    fun `test parse spelled arabic number debt command`() {
        val result = parser.parseArabicCommand("سجل على فاطمة الزهراء دين خمسة عشر الف", existingCustomers)

        assertEquals("فاطمة الزهراء", result.detectedCustomerName)
        assertEquals(3L, result.matchedCustomerId)
        assertEquals(15000.0, result.amount, 0.01)
        assertEquals(TransactionType.DEBT, result.transactionType)
    }
}
