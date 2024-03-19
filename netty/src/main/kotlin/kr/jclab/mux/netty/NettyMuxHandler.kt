package kr.jclab.mux.netty

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import kr.jclab.mux.core.MuxFrame
import kr.jclab.mux.core.types.sliceMaxSize
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

abstract class NettyMuxHandler(
    private val ready: CompletableFuture<NettyMuxHandler>?
) : AbstractMuxHandler<ByteBuf>() {
    private val idGenerator = AtomicLong(0xF)

    protected abstract val maxFrameDataLength: Int

//    override val inboundInitializer: MuxChannelInitializer<ByteBuf> = {
//        // implement it
//    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        super.handlerAdded(ctx)
        ready?.complete(this)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        msg as MuxFrame<*>
        when (msg.flag) {
            MuxFrame.Flag.OPEN -> onRemoteOpen(msg.id as NettyMuxId)
            MuxFrame.Flag.CLOSE -> onRemoteDisconnect(msg.id as NettyMuxId)
            MuxFrame.Flag.RESET -> onRemoteClose(msg.id as NettyMuxId)
            MuxFrame.Flag.DATA -> childRead(msg.id as NettyMuxId, msg.data!!)
        }
    }

    override fun onChildWrite(child: NettyMuxChannel<ByteBuf>, data: ByteBuf) {
        val ctx = getChannelHandlerContext()
        data.sliceMaxSize(maxFrameDataLength)
            .map { frameSliceBuf ->
                MuxFrame(child.id, MuxFrame.Flag.DATA, frameSliceBuf)
            }.forEach { muxFrame ->
                ctx.write(muxFrame)
            }
        ctx.flush()
    }

    override fun onLocalOpen(child: NettyMuxChannel<ByteBuf>) {
        getChannelHandlerContext().writeAndFlush(MuxFrame<NettyMuxId>(child.id, MuxFrame.Flag.OPEN))
    }

    override fun onLocalDisconnect(child: NettyMuxChannel<ByteBuf>) {
        getChannelHandlerContext().writeAndFlush(MuxFrame<NettyMuxId>(child.id, MuxFrame.Flag.CLOSE))
    }

    override fun onLocalClose(child: NettyMuxChannel<ByteBuf>) {
        getChannelHandlerContext().writeAndFlush(MuxFrame<NettyMuxId>(child.id, MuxFrame.Flag.RESET))
    }

    override fun generateNextId() =
        NettyMuxId(getChannelHandlerContext().channel().id(), idGenerator.incrementAndGet(), true)

//    private fun createStream(channel: MuxChannel<ByteBuf>): Stream {
//        val connection = ctx!!.channel().attr(CONNECTION).get()
//        val stream = StreamOverNetty(channel, connection, channel.initiator)
//        channel.attr(STREAM).set(stream)
//        return stream
//    }
//
//    override fun <T> createStream(protocols: List<ProtocolBinding<T>>): StreamPromise<T> {
//        return createStream(multistreamProtocol.createMultistream(protocols).toStreamHandler())
//    }
//
//    fun <T> createStream(streamHandler: StreamHandler<T>): StreamPromise<T> {
//        val controller = CompletableFuture<T>()
//        val stream = newStream {
//            streamHandler.handleStream(createStream(it)).forward(controller)
//        }.thenApply { it.attr(STREAM).get() }
//        return StreamPromise(stream, controller)
//    }
}
