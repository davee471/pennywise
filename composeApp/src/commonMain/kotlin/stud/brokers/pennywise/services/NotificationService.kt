package stud.brokers.pennywise.services

expect NotificationService
        {
          suspend fun sendAlert(message: String)
        }

