package kr.jclab.mux.mplex

import io.netty.buffer.Unpooled
import kr.jclab.mux.core.MuxIdBuilder
import kr.jclab.mux.core.simple.SimpleMuxId
import kr.jclab.mux.core.ProtocolViolationException
import kr.jclab.mux.core.simple.SimpleMuxIdBuilder
import kr.jclab.mux.core.test.EmbeddedChannel
import kr.jclab.mux.core.types.toByteArray
import kr.jclab.mux.core.types.toByteBuf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.charset.StandardCharsets.UTF_8

class MplexFrameCodecTest {

    companion object {
        @JvmStatic
        fun splitIndexes() = listOf(
            listOf(),
            listOf(20),
            listOf(10),
            listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
            listOf(2, 4, 8)
        )
    }

    @Test
    fun `check max frame size limit`() {
        val channelLarge = EmbeddedChannel(MplexFrameCodec(SimpleMuxIdBuilder(), maxFrameDataLength = 1024))

        val mplexFrame = MplexFrame(
            SimpleMuxId(777, true), MplexFlags.MessageInitiator,
            ByteArray(1024).toByteBuf()
        )

        assertTrue(
            channelLarge.writeOutbound(mplexFrame)
        )
        val largeFrameBytes = channelLarge.readOutbound()
        val largeFrameBytesTrunc = largeFrameBytes.slice(0, largeFrameBytes.readableBytes() - 1)

        val channelSmall = EmbeddedChannel(MplexFrameCodec(SimpleMuxIdBuilder(), maxFrameDataLength = 128))

        assertThrows<ProtocolViolationException> {
            channelSmall.writeInbound(largeFrameBytesTrunc)
        }

//        assertFalse(channelSmall.isOpen)
    }

    @ParameterizedTest
    @MethodSource("splitIndexes")
    fun testDecoder(sliceIdx: List<Int>) {
        val channel = EmbeddedChannel(MplexFrameCodec(SimpleMuxIdBuilder()))

        val mplexFrames = arrayOf(
            MplexFrame(SimpleMuxId(777, true), MplexFlags.MessageInitiator, "Hello-1".toByteArray().toByteBuf()),
            MplexFrame(SimpleMuxId(888, true), MplexFlags.MessageInitiator, "Hello-2".toByteArray().toByteBuf()),
            MplexFrame(SimpleMuxId(999, true), MplexFlags.MessageInitiator, "Hello-3".toByteArray().toByteBuf())
        )
        assertTrue(
            channel.writeOutbound(*mplexFrames)
        )

        val rawData = Unpooled.wrappedBuffer(
            channel.readOutbound(),
            channel.readOutbound(),
            channel.readOutbound()
        )

        for (i in 0..sliceIdx.size) {
            val startIdx = if (i == 0) 0 else sliceIdx[i - 1]
            val endIdx = if (i == sliceIdx.size) rawData.writerIndex() else sliceIdx[i]
            channel.writeInbound(rawData.retainedSlice(startIdx, endIdx - startIdx))
        }

        val resultFrames = List(3) { channel.readInbound() }
        assertEquals(777, resultFrames[0]?.id?.id)
        assertEquals(888, resultFrames[1]?.id?.id)
        assertEquals(999, resultFrames[2]?.id?.id)
        assertEquals("Hello-1", resultFrames[0]?.data!!.toByteArray().toString(UTF_8))
        assertEquals("Hello-2", resultFrames[1]?.data!!.toByteArray().toString(UTF_8))
        assertEquals("Hello-3", resultFrames[2]?.data!!.toByteArray().toString(UTF_8))
    }
}
