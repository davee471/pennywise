package stud.brokers.pennywise.db
import app.cash.sqldelight.db.SqlDriver
import stud.brokers.pennywise.PennyWiseDatabase
expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): PennyWiseDatabase{
    val driver = driverFactory.createDriver()
    return PennyWiseDatabase(driver)
}


