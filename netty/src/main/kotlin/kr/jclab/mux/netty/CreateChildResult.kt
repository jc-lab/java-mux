package kr.jclab.mux.netty

import io.netty.channel.ChannelFuture
import io.netty.channel.EventLoopGroup

class CreateChildResult<TData>(
    val channel: NettyMuxChannel<TData>,
    private val defaultEventLoop: EventLoopGroup,
) {
    fun register(): ChannelFuture {
        return defaultEventLoop.register(channel)
    }
}