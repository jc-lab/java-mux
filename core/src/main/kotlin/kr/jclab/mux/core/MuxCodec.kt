package kr.jclab.mux.core

import io.netty.buffer.ByteBuf

interface MuxCodec<C, D: MuxFrame> {
    fun encode(ctx: C?, msg: D, out: ByteBuf)

    @Throws(ProtocolViolationException::class)
    fun decode(ctx: C?, msg: ByteBuf, out: MutableList<D>)
}