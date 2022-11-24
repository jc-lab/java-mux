package kr.jclab.mux.core.test

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import kr.jclab.mux.core.MuxCodec
import kr.jclab.mux.core.MuxFrame
import java.util.ArrayDeque

class EmbeddedChannel<C, D: MuxFrame>(
    private val codec: MuxCodec<C, D>
) {
    private val outboundBuf = ByteBufAllocator.DEFAULT.buffer()
    private val inboundBuf = ByteBufAllocator.DEFAULT.buffer()
    private val inboundQueue = ArrayDeque<D>()

    fun writeOutbound(vararg data: D): Boolean {
        data.forEach {
            codec.encode(null, it, outboundBuf)
        }
        return true
    }

    fun readOutbound(): ByteBuf {
        return outboundBuf
    }

    fun writeInbound(input: ByteBuf) {
        inboundBuf.writeBytes(input)
        val result = ArrayList<D>()
        codec.decode(null, inboundBuf, result)
        inboundQueue.addAll(result)
    }

    fun readInbound(): D? {
        return inboundQueue.pollFirst()
    }
}
