package stud.brokers.pennywise

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform