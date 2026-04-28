package stud.brokers.pennywise.util

// handling Database errors proeprly
sealed class Result<out T> {
  data class Success<out T>(val data: T) : Result<T>()
  data class Error(val message: String, val type: ErrorType = ErrorType.UNKNOWN) : Result<Nothing>()

  enum class ErrorType {
    DATABASE, VALIDATION, NOT_FOUND, PERMISSION, UNKNOWN
  }
}




