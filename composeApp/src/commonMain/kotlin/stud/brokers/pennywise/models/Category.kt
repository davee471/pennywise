package stud.brokers.pennywise.models

import kotlinx.serialization.Serializable

/**
 * Represents a transaction category used to classify expenses and income.
 *
 * Categories can be either built-in defaults (e.g. "Food", "Transport") or user-defined.
 * Each category is identified by a unique [id] and carries a display [name] and an [iconName]
 * that maps to a drawable resource or icon identifier on the host platform.
 *
 * Instances are stored in the database and embedded directly inside [Transaction] objects so that
 * category metadata is preserved even if the category is later deleted by the user.
 *
 * @property id Unique database identifier. A value of `0` indicates an unsaved/transient instance.
 * @property name Human-readable label shown in the UI (e.g. "Groceries", "Salary").
 * @property iconName Platform-specific icon identifier (e.g. a Material symbol name or drawable
 * resource name) used to render the category's icon in lists and detail screens.
 */
@Serializable
data class Category(
    val id: Long,
    val name: String,
    val iconName: String,
)
