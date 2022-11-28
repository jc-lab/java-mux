package kr.jclab.mux.netty

import io.netty.channel.ChannelHandlerContext
import kr.jclab.mux.core.MuxIdBuilder

class NettyMuxIdBuilder : MuxIdBuilder<ChannelHandlerContext, NettyMuxId> {
    override fun create(ctx: ChannelHandlerContext?, streamId: Long, initiator: Boolean): NettyMuxId =
        NettyMuxId(ctx!!.channel().id(), streamId, initiator)
}