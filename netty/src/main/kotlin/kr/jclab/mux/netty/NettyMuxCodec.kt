package kr.jclab.mux.netty

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec
import kr.jclab.mux.core.MuxCodec
import kr.jclab.mux.core.MuxFrame

class NettyMuxCodec<F : MuxFrame<NettyMuxId>>(
    val codec: MuxCodec<ChannelHandlerContext, F, NettyMuxId>
) : ByteToMessageCodec<MuxFrame<NettyMuxId>>() {
    override fun encode(ctx: ChannelHandlerContext, msg: MuxFrame<NettyMuxId>, out: ByteBuf) =
        codec.encode(ctx, msg as F, out)

    override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf, out: MutableList<Any>) =
        codec.decode(ctx, `in`, out as MutableList<F>)

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        // notify higher level handlers on the error
        ctx.fireExceptionCaught(cause)
        // exceptions in [decode] are very likely unrecoverable so just close the connection
        ctx.close()
    }
}