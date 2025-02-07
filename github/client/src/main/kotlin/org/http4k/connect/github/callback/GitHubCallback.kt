package org.http4k.connect.github.callback

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectAdapter
import org.http4k.connect.RemoteFailure
import org.http4k.connect.github.api.action.GitHubCallbackAction

@Http4kConnectAdapter
fun interface GitHubCallback {
    operator fun invoke(action: GitHubCallbackAction): Result<Unit, RemoteFailure>

    companion object
}

