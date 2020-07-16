package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class BillingServiceTest {
    private val dal = mockk<AntaeusDal> {

        every { fetchInvoice(1) } returns Invoice(1, 11, Money(BigDecimal("1000"), Currency.EUR), InvoiceStatus.PAID)
        every { fetchCustomer(11)} returns Customer(11, Currency.EUR)

        every { fetchInvoice(2) } returns Invoice(2, 22, Money(BigDecimal("1000"), Currency.EUR), InvoiceStatus.PENDING)
        every { fetchCustomer(22)} returns Customer(22, Currency.EUR)

        every { fetchInvoice(3) } returns Invoice(3, 33, Money(BigDecimal("1000"), Currency.EUR), InvoiceStatus.PENDING)
        every { fetchCustomer(33)} returns Customer(33, Currency.EUR)

        every { fetchInvoice(4) } returns Invoice(4, 44, Money(BigDecimal("1000"), Currency.EUR), InvoiceStatus.PENDING)
        every { fetchCustomer(44)} returns Customer(44, Currency.DKK)

        every { fetchInvoice(5) } returns Invoice(5, 55, Money(BigDecimal("1000"), Currency.EUR), InvoiceStatus.PENDING)
        every { fetchCustomer(55)} returns null

        every { fetchInvoice(404) } returns null

        every { fetchInvoices() } returns listOf(fetchInvoice(1), fetchInvoice(2), fetchInvoice(3), fetchInvoice(4)) as List<Invoice>
    }

    private val billingService = BillingService(getPaymentProvider(), dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            billingService.bill(404)
        }
    }

    @Test
    fun `will return true for payment when customer already paid`() {
        assertTrue(billingService.bill(1))
    }

    @Test
    fun `will return true for payment when customer is required to pay and payment provider returns true`() {
        assertTrue(billingService.bill(2))
    }

    @Test
    fun `will return false for payment when customer is required to pay and payment provider returns false`() {
        assertFalse(billingService.bill(3))
    }

    @Test
    fun `will throw if invoice's currency is different that customer's`() {
        assertThrows<CurrencyMismatchException> {
            billingService.bill(4)
        }
    }

    @Test
    fun `will throw if invoice's customer is missing`() {
        assertThrows<CustomerNotFoundException> {
            billingService.bill(5)
        }
    }

    @Test
    fun `will never throw on scheduled list of invoices`() {
        assertDoesNotThrow{ billingService.run() }
    }

    // This is the mocked instance of the payment provider
    private fun getPaymentProvider(): PaymentProvider {
        return object : PaymentProvider {
            override fun charge(invoice: Invoice): Boolean {
                return invoice.id != 3
            }
        }
    }
}