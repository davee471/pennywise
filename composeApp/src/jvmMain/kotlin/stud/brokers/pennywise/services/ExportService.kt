package stud.brokers.pennywise.services
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.buildCsv
import java.io.File


actual class ExportService {
     actual suspend fun exportToCsv(transactions: List<Transaction>): Result<Unit>{
         return try{
             val csv = buildCsv(transactions)
             val file = File(
                 System.getProperty("user.home"),
                 "pennywise_export.csv"
             )
             file.writeText(csv)
             Result.Success(Unit)
         } catch (e: Exception){
             Result.Error(e.message ?: "Export Failed", Result.ErrorType.FILESYSTEM)
         }
     }
}