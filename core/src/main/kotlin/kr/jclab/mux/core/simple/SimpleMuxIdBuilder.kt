package kr.jclab.mux.core.simple

import kr.jclab.mux.core.MuxIdBuilder

class SimpleMuxIdBuilder : MuxIdBuilder<SimpleMuxId> {
    override fun create(streamId: Long, initiator: Boolean): SimpleMuxId =
        SimpleMuxId(streamId, initiator)
}