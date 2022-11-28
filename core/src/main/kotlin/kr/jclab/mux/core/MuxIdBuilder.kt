package kr.jclab.mux.core

interface MuxIdBuilder<ID : MuxId> {
    fun create(streamId: Long, initiator: Boolean): ID
}