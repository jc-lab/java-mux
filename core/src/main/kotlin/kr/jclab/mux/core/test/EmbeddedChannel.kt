package kr.jclab.mux.core.test

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import kr.jclab.mux.core.MuxCodec
import kr.jclab.mux.core.MuxFrame
import kr.jclab.mux.core.MuxId
import java.util.ArrayDeque

class EmbeddedChannel<D: MuxFrame<ID>, ID: MuxId>(
    private val codec: MuxCodec<D, ID>
) {
    private val outboundBuf = ByteBufAllocator.DEFAULT.buffer()
    private val inboundBuf = ByteBufAllocator.DEFAULT.buffer()
    private val inboundQueue = ArrayDeque<D>()

    fun writeOutbound(vararg data: D): Boolean {
        data.forEach {
            codec.encode(it, outboundBuf)
        }
        return true
    }

    fun readOutbound(): ByteBuf {
        return outboundBuf
    }

    fun writeInbound(input: ByteBuf) {
        inboundBuf.writeBytes(input)
        val result = ArrayList<D>()
        codec.decode(inboundBuf, result)
        inboundQueue.addAll(result)
    }

    fun readInbound(): D? {
        return inboundQueue.pollFirst()
    }
}
