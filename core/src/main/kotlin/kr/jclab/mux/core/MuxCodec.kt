package kr.jclab.mux.core

import io.netty.buffer.ByteBuf

interface MuxCodec<D: MuxFrame<ID>, ID: MuxId> {
    fun encode(msg: D, out: ByteBuf)

    @Throws(ProtocolViolationException::class)
    fun decode(msg: ByteBuf, out: MutableList<D>)
}