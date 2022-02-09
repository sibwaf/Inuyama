package sibwaf.inuyama.app.common.backup

abstract class ModuleBackupHandler(val moduleName: String) {
    abstract fun provideData(): String
    abstract fun restoreData(data: String)
}
