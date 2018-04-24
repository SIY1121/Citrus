package util

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.actor

class SerializedOperationQueue(name: String = "EventLoop", capacity: Int = 0) {
    private val singleThreadContext = newSingleThreadContext(name)
    private val actor = actor<suspend () -> Unit>(singleThreadContext, capacity) {
        for (operation in channel) {
            operation.invoke()
        }
    }

    fun push(operation: suspend () -> Unit) = launch(Unconfined) {
        actor.send(operation)
    }
}