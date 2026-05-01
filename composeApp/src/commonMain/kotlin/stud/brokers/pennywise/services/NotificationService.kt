package stud.brokers.pennywise.services

interface NotificationService {
    suspend fun sendAlert(message:String)
}