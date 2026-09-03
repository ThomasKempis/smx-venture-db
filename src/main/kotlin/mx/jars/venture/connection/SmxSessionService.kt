package mx.jars.venture.connection

import mx.jars.venture.venture_core_connect.SmxConstantsCore
import java.util.UUID

data class SmxConnectionProfile(
    val usuario: String = SmxConstantsCore.usuario,
    val clave: String = SmxConstantsCore.clave,
    val instancia: String = SmxConstantsCore.instancia,
    val urlServidorWeb: String = SmxConstantsCore.urlServidorWeb,
    val idSession: String = SmxConstantsCore.idSession,
    val isDesktop: Boolean = SmxConstantsCore.isDesktop,
    val isConnected: Boolean = false,
)

object SmxSessionService {
    private var profile: SmxConnectionProfile = SmxConnectionProfile()
    private var lastError: String? = null

    fun currentProfile(): SmxConnectionProfile = profile

    fun currentUser(): String = profile.usuario

    fun currentInstancia(): String = profile.instancia

    fun currentError(): String? = lastError

    fun isConnected(): Boolean = profile.isConnected

    fun markConnected() {
        profile = profile.copy(isConnected = true)
        lastError = null
    }

    fun markDisconnected() {
        profile = profile.copy(isConnected = false)
        lastError = null
    }

    fun reset() {
        profile = SmxConnectionProfile(isConnected = false)
        lastError = null
    }

    fun configure(
        usuario: String,
        clave: String,
        instancia: String,
        produccion: Boolean = true,
    ): SmxConnectionProfile {
        SmxConstantsCore.initSetupConnection(produccion)
        SmxConstantsCore.usuario = usuario
        SmxConstantsCore.clave = clave
        SmxConstantsCore.instancia = instancia
        SmxConstantsCore.isDesktop = true
        SmxConstantsCore.idSession = UUID.randomUUID().toString()

        profile = SmxConnectionProfile(
            usuario = SmxConstantsCore.usuario,
            clave = SmxConstantsCore.clave,
            instancia = SmxConstantsCore.instancia,
            urlServidorWeb = SmxConstantsCore.urlServidorWeb,
            idSession = SmxConstantsCore.idSession,
            isDesktop = SmxConstantsCore.isDesktop,
            isConnected = false,
        )
        lastError = null
        return profile
    }

    fun applyToCore(profile: SmxConnectionProfile = currentProfile()) {
        SmxConstantsCore.usuario = profile.usuario
        SmxConstantsCore.clave = profile.clave
        SmxConstantsCore.instancia = profile.instancia
        SmxConstantsCore.urlServidorWeb = profile.urlServidorWeb
        SmxConstantsCore.idSession = profile.idSession
        SmxConstantsCore.isDesktop = profile.isDesktop
    }

    fun recordError(message: String?) {
        lastError = message
        if (message != null) {
            profile = profile.copy(isConnected = false)
        }
    }
}
