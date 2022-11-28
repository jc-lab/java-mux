package kr.jclab.mux.core

import io.netty.buffer.ByteBuf

interface MuxCodec<C, D: MuxFrame<ID>, ID: MuxId> {
    fun encode(ctx: C?, msg: MuxFrame<*>, out: ByteBuf)

    @Throws(ProtocolViolationException::class)
    fun decode(ctx: C?, msg: ByteBuf, out: MutableList<D>)
}