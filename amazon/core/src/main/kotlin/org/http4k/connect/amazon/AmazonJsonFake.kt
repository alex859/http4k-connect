package org.http4k.connect.amazon

import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.format.AutoMarshalling
import org.http4k.routing.bind
import org.http4k.routing.header

class AmazonJsonFake(val autoMarshalling: AutoMarshalling, val awsService: AwsService) {
    inline fun <reified Req : Any> route(crossinline fn: (Req) -> Any?) =
        header("X-Amz-Target", "${awsService}.${Req::class.simpleName!!.removeSuffix("Request")}") bind {
            fn(autoMarshalling.asA(it.bodyString(), Req::class))
                ?.let {
                    when (it) {
                        is Unit -> Response(OK).body("{}")
                        else -> Response(OK).body(autoMarshalling.asFormatString(it))
                    }
                }
                ?: Response(BAD_REQUEST)
                    .body(
                        autoMarshalling.asFormatString(
                            JsonError(
                                "ResourceNotFoundException",
                                "$awsService can't find the specified item."
                            )
                        )
                    )
        }
}

data class JsonError(val __type: String, val Message: String)
