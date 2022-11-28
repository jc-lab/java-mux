package kr.jclab.mux.core

import io.netty.buffer.ByteBuf
import kr.jclab.mux.core.exception.ConnectionClosedException
import java.util.concurrent.atomic.AtomicLong

typealias MuxChannelBuilder = (id: MuxId) -> MuxChannel

abstract class AbstractSimpleMuxHandler {
    protected var closed: Boolean = false
    protected val streamMap: MutableMap<MuxId, MuxChannel> = mutableMapOf()

    fun handleMuxFrame(msg: MuxFrame) {
        when (msg.flag) {
            MuxFrame.Flag.OPEN -> onRemoteOpen(msg.id)
            MuxFrame.Flag.CLOSE -> onRemoteDisconnect(msg.id)
            MuxFrame.Flag.RESET -> onRemoteClose(msg.id)
            MuxFrame.Flag.DATA -> childRead(msg.id, msg.data!!)
        }
    }

    fun newStreamImpl(channelBuilder: MuxChannelBuilder): MuxChannel {
        checkClosed()
        val id = generateNextId()
        val child = channelBuilder(id)
        streamMap[id] = child
        sendFrameToRemote(MuxFrame(child.getId(), MuxFrame.Flag.OPEN))
        return child
    }

    protected abstract fun sendFrameToRemote(msg: MuxFrame);

    protected abstract fun onRemoteOpen(id: MuxId);

    protected abstract fun onRemoteDisconnect(id: MuxId);

    protected abstract fun onRemoteClose(id: MuxId);

    protected abstract fun onRemoteRead(child: MuxChannel, data: ByteBuf);

    protected fun childRead(id: MuxId, data: ByteBuf) {
        val child = streamMap[id] ?: throw ConnectionClosedException("Channel with id $id not opened")
        onRemoteRead(child, data)
    }

    protected fun onClosed(child: MuxChannel): Boolean =
        streamMap.remove(child.getId()) != null

    private fun checkClosed() = if (closed) throw ConnectionClosedException("Can't create a new stream: connection was closed") else Unit

    private val idGenerator = AtomicLong(0xF)
    protected fun generateNextId(): MuxId =
        MuxId(idGenerator.incrementAndGet(), true)
}
