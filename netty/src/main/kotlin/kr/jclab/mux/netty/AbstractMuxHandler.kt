package kr.jclab.mux.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import kr.jclab.mux.core.exception.ConnectionClosedException
import kr.jclab.mux.core.exception.InternalErrorException
import java.util.concurrent.CompletableFuture
import java.util.function.Function
import kotlin.jvm.Throws

typealias MuxChannelInitializer<TData> = (NettyMuxChannel<TData>) -> Unit

abstract class AbstractMuxHandler<TData>() :
    ChannelInboundHandlerAdapter() {

    private val streamMap: MutableMap<NettyMuxId, NettyMuxChannel<TData>> = mutableMapOf()
    var ctx: ChannelHandlerContext? = null
    private val activeFuture = CompletableFuture<Void>()
    private var closed = false
    protected abstract val inboundInitializer: MuxChannelInitializer<TData>
    private val pendingReadComplete = mutableSetOf<NettyMuxId>()

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        super.handlerAdded(ctx)
        this.ctx = ctx
    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        activeFuture.complete(null)
        super.channelActive(ctx)
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext?) {
        activeFuture.completeExceptionally(ConnectionClosedException())
        closed = true
        super.channelUnregistered(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.fireExceptionCaught(cause)
    }

    fun getChannelHandlerContext(): ChannelHandlerContext {
        return ctx ?: throw InternalErrorException("Internal error: handler context should be initialized at this stage")
    }

    protected fun childRead(id: NettyMuxId, msg: TData) {
        val child = streamMap[id] ?: throw ConnectionClosedException("Channel with id $id not opened")
        pendingReadComplete += id
        child.pipeline().fireChannelRead(msg)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        pendingReadComplete.forEach { streamMap[it]?.pipeline()?.fireChannelReadComplete() }
        pendingReadComplete.clear()
    }

    abstract fun onChildWrite(child: NettyMuxChannel<TData>, data: TData)

    protected fun onRemoteOpen(id: NettyMuxId): CreateChildResult<TData> {
        val initializer = inboundInitializer
        val result = createChild(
            id,
            initializer,
            false
        )
        onRemoteCreated(result.channel)
        return result
    }

    protected fun onRemoteDisconnect(id: NettyMuxId) {
        // the channel could be RESET locally, so ignore remote CLOSE
        streamMap[id]?.onRemoteDisconnected()
    }

    protected fun onRemoteClose(id: NettyMuxId) {
        // the channel could be RESET locally, so ignore remote RESET
        streamMap[id]?.closeImpl()
    }

    fun localDisconnect(child: NettyMuxChannel<TData>) {
        onLocalDisconnect(child)
    }

    fun localClose(child: NettyMuxChannel<TData>) {
        onLocalClose(child)
    }

    fun onClosed(child: NettyMuxChannel<TData>) {
        streamMap.remove(child.id)
    }

    abstract override fun channelRead(ctx: ChannelHandlerContext, msg: Any)
    protected open fun onRemoteCreated(child: NettyMuxChannel<TData>) {}
    protected abstract fun onLocalOpen(child: NettyMuxChannel<TData>)
    protected abstract fun onLocalClose(child: NettyMuxChannel<TData>)
    protected abstract fun onLocalDisconnect(child: NettyMuxChannel<TData>)

    protected open fun newChildChannel(
        id: NettyMuxId,
        initializer: MuxChannelInitializer<TData>,
        initiator: Boolean
    ): NettyMuxChannel<TData> =
        NettyMuxChannel(this, id, initializer, initiator)

    private fun createChild(
        id: NettyMuxId,
        initializer: MuxChannelInitializer<TData>,
        initiator: Boolean
    ): CreateChildResult<TData> {
        val child = newChildChannel(id, initializer, initiator)
        streamMap[id] = child
        return CreateChildResult(child, ctx!!.channel().eventLoop())
    }

    // protected open fun createChannel(id: MuxId, initializer: ChannelHandler) = MuxChannel(this, id, initializer)

    protected abstract fun generateNextId(): NettyMuxId

    @Throws(ConnectionClosedException::class)
    protected fun getStream(id: NettyMuxId): NettyMuxChannel<TData> {
        return streamMap[id] ?: throw ConnectionClosedException("Channel with id $id not opened")
    }

    /**
     * new stream with register to event loop
     */
    fun newStream(outboundInitializer: MuxChannelInitializer<TData>): CompletableFuture<NettyMuxChannel<TData>> {
        try {
            checkClosed() // if already closed then event loop is already down and async task may never execute
            return activeFuture.thenApplyAsync(
                Function {
                    val result = newStreamSync(outboundInitializer)
                    result.register()
                    result.channel
                },
                getChannelHandlerContext().channel().eventLoop()
            )
        } catch (e: Exception) {
            return completedExceptionally(e)
        }
    }

    /**
     * new stream without register to event loop
     */
    fun newStreamSync(outboundInitializer: MuxChannelInitializer<TData>): CreateChildResult<TData> {
        checkClosed() // close may happen after above check and before this point
        return createChild(
            generateNextId(),
            {
                onLocalOpen(it)
                outboundInitializer(it)
            },
            true
        )
    }

    private fun checkClosed() = if (closed) throw ConnectionClosedException("Can't create a new stream: connection was closed: " + ctx!!.channel()) else Unit

    private fun <C> completedExceptionally(t: Throwable) = CompletableFuture<C>().also { it.completeExceptionally(t) }
}
