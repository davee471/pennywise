package stud.brokers.pennywise

/**
 * Represents the JVM platform implementation.
 */
class JVMPlatform: Platform {
    /**
     * The name of the platform, including the Java version.
     */
    override val name: String = "Java ${System.getProperty("java.version")}"
}

/**
 * Returns the current [Platform] instance for the JVM.
 *
 * @return An instance of [JVMPlatform].
 */
actual fun getPlatform(): Platform = JVMPlatform()