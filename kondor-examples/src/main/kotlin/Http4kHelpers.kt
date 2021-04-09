import com.ubertob.kondor.json.JConverter
import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Meta
import org.http4k.lens.httpBodyRoot


private val contentTypeHeaderName = "Content-Type"

fun Request.contentType(contentType: ContentType) =
    header(contentTypeHeaderName, contentType.toHeaderValue())


// for http clients
fun <T : Any> Request.bodyAsJson(converter: JConverter<T>, value: T) =
    body(converter.toJson(value)).contentType(APPLICATION_JSON)

fun <T : Any> Response.parseJsonBody(converter: JConverter<T>): T =
    converter.fromJson(bodyString()).orThrow()

//for http servers

fun <T : Any> Request.parseJsonBody(converter: JConverter<T>): T =
    converter.fromJson(bodyString()).orThrow()

fun <T : Any> Response.bodyAsJson(converter: JConverter<T>, value: T) =
    body(converter.toJson(value)).header(contentTypeHeaderName, APPLICATION_JSON.toHeaderValue())


fun <T : Any> JConverter<T>.toBodyLens(vararg metas: Meta): BiDiBodyLens<T> =
    httpBodyRoot(metas.toList(), APPLICATION_JSON, ContentNegotiation.None)
        .map({ fromJson(it.payload.asString()).orThrow() }, { Body(toJson(it)) })
        .toLens()