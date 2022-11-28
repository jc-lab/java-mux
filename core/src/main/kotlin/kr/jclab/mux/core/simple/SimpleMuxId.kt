package kr.jclab.mux.core.simple

import kr.jclab.mux.core.MuxId

data class SimpleMuxId (
    override val id: Long,
    override val initiator: Boolean
) : MuxId