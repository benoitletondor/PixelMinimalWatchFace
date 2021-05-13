package com.benoitletondor.pixelminimalwatchfacecompanion.helper

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A flow that emits only when it has a subscriber
 */
class MutableLiveFlow<T> : Flow<T>, FlowCollector<T> {
    private val wrapped = MutableSharedFlow<T>(extraBufferCapacity = 8) // Store up to 8 events while LifeCycle is not Started before blocking sender
    private val buffer = mutableListOf<T>()
    private val mutex = Mutex()

    @OptIn(InternalCoroutinesApi::class)
    override suspend fun collect(collector: FlowCollector<T>) {
        wrapped
            .onSubscription {
                mutex.withLock {
                    if (buffer.isNotEmpty()) {
                        emitAll(buffer.asFlow())
                        buffer.clear()
                    }
                }
            }
            .collect(collector)
    }

    override suspend fun emit(value: T) {
        mutex.withLock {
            if (wrapped.subscriptionCount.value <= 0) {
                buffer.add(value)
            } else {
                wrapped.emit(value)
            }
        }
    }
}