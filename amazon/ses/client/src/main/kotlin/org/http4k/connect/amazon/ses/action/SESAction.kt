package org.http4k.connect.amazon.ses.action

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Method
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE

abstract class SESAction<R>(
    private val action: String,
    private vararg val mappings: Pair<String, String>?
) : Action<Result<R, RemoteFailure>> {
    override fun toRequest() = (listOf("Action" to action) + mappings)
        .filterNotNull()
        .fold(
            Request(
                POST,
                Uri.of("")
            ).with(CONTENT_TYPE of APPLICATION_FORM_URLENCODED)
        ) { req, it -> req.form(it.first, it.second) }
}
