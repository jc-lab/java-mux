package kr.jclab.mux.core.simple

import io.netty.buffer.ByteBuf
import kr.jclab.mux.core.*
import kr.jclab.mux.core.exception.ConnectionClosedException
import java.util.concurrent.atomic.AtomicLong

abstract class AbstractSimpleMuxHandler<ID: MuxId> {
    protected abstract val muxIdBuilder: MuxIdBuilder<Void, ID>

    protected var closed: Boolean = false
    protected val streamMap: MutableMap<ID, MuxChannel<ID>> = mutableMapOf()

    fun handleMuxFrame(msg: MuxFrame<ID>) {
        when (msg.flag) {
            MuxFrame.Flag.OPEN -> onRemoteOpen(msg.id)
            MuxFrame.Flag.CLOSE -> onRemoteDisconnect(msg.id)
            MuxFrame.Flag.RESET -> onRemoteClose(msg.id)
            MuxFrame.Flag.DATA -> childRead(msg.id, msg.data!!)
        }
    }

    fun newStreamImpl(channelBuilder: MuxChannelBuilder<ID>): MuxChannel<ID> {
        checkClosed()
        val id = generateNextId()
        val child = channelBuilder(id)
        streamMap[id] = child
        sendFrameToRemote(MuxFrame<ID>(child.id, MuxFrame.Flag.OPEN))
        return child
    }

    protected abstract fun sendFrameToRemote(msg: MuxFrame<ID>);

    protected abstract fun onRemoteOpen(id: MuxId);

    protected abstract fun onRemoteDisconnect(id: MuxId);

    protected abstract fun onRemoteClose(id: MuxId);

    protected abstract fun onRemoteRead(child: MuxChannel<ID>, data: ByteBuf);

    protected fun childRead(id: ID, data: ByteBuf) {
        val child = streamMap[id] ?: throw ConnectionClosedException("Channel with id $id not opened")
        onRemoteRead(child, data)
    }

    protected fun onClosed(child: MuxChannel<ID>): Boolean =
        streamMap.remove(child.id) != null

    private fun checkClosed() = if (closed) throw ConnectionClosedException("Can't create a new stream: connection was closed") else Unit

    private val idGenerator = AtomicLong(0xF)
    protected fun generateNextId(): ID =
        muxIdBuilder.create(null, idGenerator.incrementAndGet(), true)
}