package sibwaf.inuyama.app.common.backup

abstract class BackupProvider(val moduleName: String) {

    abstract fun provideData(): String
}
