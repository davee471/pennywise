package stud.brokers.pennywise.services

expect class BackupService {
    fun export(): String
    fun importBackup(json:String)
}

