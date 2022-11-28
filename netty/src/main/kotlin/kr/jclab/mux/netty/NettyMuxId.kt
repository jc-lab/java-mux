package kr.jclab.mux.netty

import io.netty.channel.ChannelId
import kr.jclab.mux.core.MuxId

data class NettyMuxId(val parentId: ChannelId, override val id: Long, override val initiator: Boolean) : MuxId, ChannelId {
    override fun asShortText() = "$parentId/$id/$initiator"
    override fun asLongText() = asShortText()
    override fun compareTo(other: ChannelId?) = asShortText().compareTo(other?.asShortText() ?: "")
    override fun toString() = asLongText()
}
