package kr.jclab.mux.netty

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.DefaultChannelId
import io.netty.channel.DefaultChannelPromise
import io.netty.channel.embedded.EmbeddedChannel
import kr.jclab.mux.mplex.MplexFrameCodec
import kr.jclab.mux.core.MuxIdBuilder
import kr.jclab.mux.mplex.MplexFlags
import kr.jclab.mux.mplex.MplexFrame
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import kotlin.test.assertEquals

class NettyMuxCodecTest {
    @Test
    fun mplexNettyMuxCodec() {
        val parentChannelId = DefaultChannelId.newInstance()
        val muxIdBuilder: MuxIdBuilder<ChannelHandlerContext, NettyMuxId> = object: MuxIdBuilder<ChannelHandlerContext, NettyMuxId> {
            override fun create(ctx: ChannelHandlerContext?, streamId: Long, initiator: Boolean): NettyMuxId {
                return NettyMuxId(parentChannelId, streamId, initiator)
            }
        }
        val codec = NettyMuxCodec(MplexFrameCodec(muxIdBuilder))

        val channel = EmbeddedChannel()
        val closePromise = DefaultChannelPromise(channel)
        val ctx = mock(ChannelHandlerContext::class.java)
        `when`(ctx.alloc()).thenReturn(ByteBufAllocator.DEFAULT)

        val frame = MplexFrame(
            NettyMuxId(DefaultChannelId.newInstance(), 1, true),
            MplexFlags.NewStream
        )

        val bufCapture = ArgumentCaptor.forClass(ByteBuf::class.java)
        codec.write(ctx, frame, closePromise)
        verify(ctx).write(bufCapture.capture(), any())

        val frameCapture = ArgumentCaptor.forClass(MplexFrame::class.java)
        codec.channelRead(ctx, bufCapture.value)
        verify(ctx).fireChannelRead(frameCapture.capture())

        assertEquals(frameCapture.value, frame)
    }
}