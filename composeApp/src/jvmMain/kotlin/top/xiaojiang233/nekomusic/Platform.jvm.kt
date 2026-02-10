package top.xiaojiang233.nekomusic

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    // Simple heuristic for now, or use a compile-time constant via build config if available.
    // Assuming true for development environment in IDE.
    override val isDebug: Boolean = java.lang.management.ManagementFactory.getRuntimeMXBean().inputArguments.toString().indexOf("-agentlib:jdwp") > 0
}

actual fun getPlatform(): Platform = JVMPlatform()