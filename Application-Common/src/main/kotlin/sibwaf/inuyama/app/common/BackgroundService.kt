package sibwaf.inuyama.app.common

abstract class BackgroundService(val name: String) {

    abstract val period: Int

    abstract suspend fun execute()
}
