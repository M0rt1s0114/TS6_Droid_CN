package dev.tsdroid.bridge

import android.content.Context
import dev.tsdroid.han.R

/**
 * Translates raw native connection errors (thrown as `TsLibException` by the
 * JNI bridge, possibly wrapped by [TsClient]) into concrete, localized
 * messages instead of dumping Rust internals on screen.
 */
object ConnectionErrorMapper {

    fun localizedMessage(context: Context, failure: Throwable?): String {
        val raw = collectMessages(failure)
        val lower = raw.lowercase()
        // Native Rust error variants are camel-cased by thiserror/Debug
        // (e.g. ServerInvalidPassword); keep a compact form so the snake_case
        // CSV names match too.
        val compact = lower.replace("_", "")

        val resId = when {
            "toomanyclones" in compact || "too many clones" in lower ->
                R.string.error_too_many_clones

            "servermaxclientsreached" in compact || "server maxclient reached" in lower ->
                R.string.error_server_full

            "serverinvalidpassword" in compact || "clientinvalidpassword" in compact ->
                R.string.error_wrong_server_password

            "connectfailedbanned" in compact ->
                R.string.error_banned

            "banflooding" in compact || "clientisflooding" in compact || "flood ban" in lower ->
                R.string.error_flood_ban

            "clientversionoutdated" in compact || "serverversionoutdated" in compact ->
                R.string.error_version_outdated

            "clientnicknameinuse" in compact ->
                R.string.error_nickname_in_use

            "clientloginnotpermitted" in compact ->
                R.string.error_login_not_permitted

            "identity level" in lower || "security level too low" in lower ->
                R.string.error_identity_security_level

            "dns resolution failed" in lower || "failed to resolve address" in lower ->
                R.string.error_dns_resolution

            "invalid server address" in lower ->
                R.string.error_invalid_address

            "connection timed out" in lower || "operation timed out" in lower || "timeout" in lower ->
                R.string.error_connection_timeout

            "connection refused" in lower ->
                R.string.error_connection_refused

            else -> null
        }

        if (resId != null) return context.getString(resId)

        // Unknown failure: keep the most useful native detail available so
        // the user still gets a concrete hint instead of a generic message.
        return raw.takeIf { it.isNotBlank() } ?: context.getString(R.string.connection_failed)
    }

    private fun collectMessages(failure: Throwable?): String {
        val messages = mutableListOf<String>()
        var current: Throwable? = failure
        while (current != null) {
            val message = current.message?.trim()
            if (!message.isNullOrEmpty() && message !in messages) {
                messages.add(message)
            }
            current = current.cause
        }
        return messages.joinToString(" — ")
    }
}
