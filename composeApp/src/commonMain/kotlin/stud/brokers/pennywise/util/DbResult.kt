// handling Database errors proeprly 
sealed class DbResult<out T> {
  data class Succuess<t> (val data: T): DbResult<T>()
  data class Error<T> (val msg: String): DbResult<T>()
}


