package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(private val paymentProvider: PaymentProvider, private val dal: AntaeusDal)
    : Runnable {

    override fun run() {
        dal.fetchInvoices().forEach {
            val invoiceId = it.id
            try {
                if (bill(invoiceId)) {
                    println("Billing for the invoice id $invoiceId finished successfully.")
                } else {
                    println("Billing for the invoice id $invoiceId did not finish successfully.")
                }
            } catch (e: Exception) {
                println("Billing for the invoice id $invoiceId finished with the exception message: ${e.message}")
            }
        }
    }

    /**
     * Bill the pending Invoice represented by invoiceId.
     *
     * @param invoiceId The invoice ID
     * @return the result of billing, true is success, false otherwise
     * @throws CustomerNotFoundException when no customer has the given id
     * @throws InvoiceNotFoundException when invoice id does not exist
     * @throws CurrencyMismatchException when the currency does not match the customer account
     * @throws NetworkException when a network error happens
     */
    fun bill(invoiceId: Int): Boolean {
        val invoice = dal.fetchInvoice(invoiceId) ?: throw InvoiceNotFoundException(invoiceId)
        val customer = dal.fetchCustomer(invoice.customerId) ?: throw CustomerNotFoundException(invoice.customerId)
        if (customer.currency != invoice.amount.currency) throw CurrencyMismatchException(invoice.id, customer.id)
        return invoice.status == InvoiceStatus.PAID || paymentProvider.charge(invoice)
    }
}
