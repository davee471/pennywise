package stud.brokers.pennywise.util

/**
 * A discriminated union representing the outcome of a database or service operation.
 *
 * All [DatabaseManager], [BackupService], [ExportService], and controller functions return [Result]
 * instead of throwing exceptions. Callers pattern-match on [Success] or [Error] to handle outcomes
 * explicitly.
 *
 * Use [getOrNull] to extract the value safely, returning null on error.
 */
sealed class Result<out T> {
  /**
   * Indicates the operation completed successfully.
   * @property data The result value.
   */
  data class Success<out T>(val data: T) : Result<T>()

  /**
   * Indicates the operation failed.
   * @property message Human-readable description of what went wrong.
   * @property type The category of error — used by the UI to decide how to respond.
   */
  data class Error(val message: String, val type: ErrorType = ErrorType.UNKNOWN) :
          Result<Nothing>()

  /**
   * Categories of errors returned by [Result.Error].
   * - [DATABASE] — SQLite query or transaction failed.
   * - [VALIDATION] — Input failed a business rule (e.g. amount <= 0).
   * - [NOT_FOUND] — A requested record does not exist.
   * - [PERMISSION] — Missing system permission (e.g. storage access).
   * - [FILESYSTEM] — File read/write failed (backup, export).
   * - [UNKNOWN] — Unclassified error.
   */
  enum class ErrorType {
    DATABASE,
    VALIDATION,
    NOT_FOUND,
    PERMISSION,
    UNKNOWN,
    FILESYSTEM
  }
}

/**
 * Returns the success value, or `null` if this is a [Result.Error]. Safe alternative to casting —
 * use when a missing value is an acceptable outcome.
 */
fun <T> Result<T>.getOrNull(): T? =
        when (this) {
          is Result.Success -> data
          is Result.Error -> null
        }
