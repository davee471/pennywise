package stud.brokers.pennywise.util

// handling Database errors proeprly
sealed class DbResult<out T> {
  data class Succuess<T> (val data: T): DbResult<T>()
  data class Error<T> (val msg: String): DbResult<Nothing>()
}




