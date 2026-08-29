package com.acelinkhelper.tv

private const val ACESTREAM_PORT = 6878
private val ID_REGEX = Regex("^[A-Za-z0-9]+$")

/**
 * Extrae el content ID de un enlace acestream.
 *
 * Acepta "acestream://ID", "acestream:ID" y el ID pelado, con el esquema en cualquier
 * combinación de mayúsculas, con barras sobrantes y con parámetros extra ("?name=Canal").
 * Devuelve null si no queda un ID válido.
 */
fun parseAcestreamId(raw: String): String? {
    var s = raw.trim()
    if (s.startsWith("acestream:", ignoreCase = true)) {
        s = s.substring("acestream:".length).trimStart('/')
    }
    s = s.substringBefore('?').substringBefore('#').trim('/').trim()
    return s.takeIf { it.matches(ID_REGEX) }
}

/** URL HTTP del stream en el servidor Acestream de [nasIp]. */
fun acestreamUrl(nasIp: String, id: String): String =
    "http://$nasIp:$ACESTREAM_PORT/ace/getstream?id=$id"
