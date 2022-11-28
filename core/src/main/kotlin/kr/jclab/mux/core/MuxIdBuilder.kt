package kr.jclab.mux.core

interface MuxIdBuilder<C, ID : MuxId> {
    fun create(ctx: C?, streamId: Long, initiator: Boolean): ID
}