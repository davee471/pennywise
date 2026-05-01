package stud.brokers.pennywise.models

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Long,
    val name: String,
    val iconName: String,
)


