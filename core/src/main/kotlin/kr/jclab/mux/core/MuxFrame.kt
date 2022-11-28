package kr.jclab.mux.core

import io.netty.buffer.ByteBuf
import io.netty.buffer.DefaultByteBufHolder
import io.netty.buffer.Unpooled
import kr.jclab.mux.core.types.toByteArray
import kr.jclab.mux.core.types.toHex

open class MuxFrame<ID: MuxId>(val id: ID, val flag: Flag, val data: ByteBuf? = null) :
    DefaultByteBufHolder(data ?: Unpooled.EMPTY_BUFFER) {

    enum class Flag {
        OPEN,
        DATA,
        CLOSE,
        RESET
    }

    override fun toString(): String {
        return "MuxFrame(id=$id, flag=$flag, data=${data?.toByteArray()?.toHex()})"
    }
}
