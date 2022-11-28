package kr.jclab.mux.core.simple

import kr.jclab.mux.core.MuxIdBuilder

class SimpleMuxIdBuilder : MuxIdBuilder<Void, SimpleMuxId> {
    override fun create(ctx: Void?, streamId: Long, initiator: Boolean): SimpleMuxId =
        SimpleMuxId(streamId, initiator)
}