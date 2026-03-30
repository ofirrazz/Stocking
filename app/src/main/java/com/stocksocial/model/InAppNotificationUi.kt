package com.stocksocial.model

data class InAppNotificationUi(
    val id: String,
    val headline: String,
    val body: String,
    val timeLabel: String,
    val unread: Boolean,
    /** null = no View chip; true = green up; false = red down */
    val viewPositive: Boolean?,
    val stockSymbolForView: String?,
    val isSocialStyle: Boolean
)
