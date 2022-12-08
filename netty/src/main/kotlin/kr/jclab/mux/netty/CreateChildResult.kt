package kr.jclab.mux.netty

import io.netty.channel.ChannelFuture

class CreateChildResult<TData>(
    val channel: NettyMuxChannel<TData>,
    val registerFuture: ChannelFuture
)