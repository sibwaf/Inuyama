package sibwaf.inuyama.app.common.backup

import java.io.InputStream

abstract class ModuleBackupHandler(val moduleName: String) {
    abstract fun provideData(): InputStream
    abstract fun restoreData(data: InputStream)
}
