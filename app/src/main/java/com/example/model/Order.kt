package com.example.model

enum class OrderStatus(val label: String, val stepIndex: Int) {
    RECEIVED("Order Received", 0),
    PREPARING("Preparing", 1),
    READY("Ready", 2),
    OUT_FOR_DELIVERY("Out for Delivery", 3),
    DELIVERED("Delivered", 4),
    CANCELLED("Cancelled", -1)
}

data class OrderItemDto(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 1,
    val unitPrice: Int = 0,
    val extrasSummary: String = "",
    val specialInstructions: String = ""
)

data class Order(
    val id: String = "",
    val orderNumber: String = "",
    val customerUid: String = "",
    val customerName: String = "",
    val phone: String = "",
    val customerEmail: String = "",
    val deliveryAddress: String = "",
    val orderNotes: String = "",
    val items: List<OrderItemDto> = emptyList(),
    val subtotal: Int = 0,
    val deliveryFee: Int = 150,
    val total: Int = 0,
    val status: OrderStatus = OrderStatus.RECEIVED,
    val paymentMethod: String = "Cash on Delivery (COD)",
    val createdAt: Long = System.currentTimeMillis()
)
