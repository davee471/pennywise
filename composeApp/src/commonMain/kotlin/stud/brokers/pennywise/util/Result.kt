package stud.brokers.pennywise.util

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String, val type: ErrorType = ErrorType.UNKNOWN) : Result<Nothing>()

    enum class ErrorType {
        DATABASE, VALIDATION, NOT_FOUND, PERMISSION, UNKNOWN
    }

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
}
