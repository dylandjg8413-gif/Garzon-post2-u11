package com.example.app.core.sync

/**
 * Abstraccion del disparador de sincronizacion. Existe para que el
 * repositorio no tenga que conocer WorkManager y se pueda testear con
 * un fake que solo cuente cuantas veces se llamo a schedule().
 */
interface SyncTrigger {
    fun schedule()
}
