/*
 * Copyright 2019 BLK Technologies Limited (web3labs.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package kr.jclab.mux.mplex

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kr.jclab.mux.core.MuxCodec
import kr.jclab.mux.core.MuxFrame
import kr.jclab.mux.core.MuxId
import kr.jclab.mux.core.ProtocolViolationException
import kr.jclab.mux.core.types.readUvarint
import kr.jclab.mux.core.types.writeUvarint

const val DEFAULT_MAX_MPLEX_FRAME_DATA_LENGTH = 1 shl 20

/**
 * A Netty codec implementation that converts [MplexFrame] instances to [ByteBuf] and vice-versa.
 */
class MplexFrameCodec<C>(
    val maxFrameDataLength: Int = DEFAULT_MAX_MPLEX_FRAME_DATA_LENGTH
) : MuxCodec<C, MplexFrame> {

    /**
     * Encodes the given mplex frame into bytes and writes them into the output list.
     * @see [https://github.com/libp2p/specs/tree/master/mplex]
     * @param ctx the context.
     * @param msg the mplex frame.
     * @param out the list to write the bytes to.
     */
    override fun encode(ctx: C?, msg: MplexFrame, out: ByteBuf) {
        out.writeUvarint(msg.id.id.shl(3).or(MplexFlags.toMplexFlag(msg.flag, msg.id.initiator).toLong()))
        out.writeUvarint(msg.data?.readableBytes() ?: 0)
        out.writeBytes(msg.data ?: Unpooled.EMPTY_BUFFER)
    }

    /**
     * Decodes the bytes in the given byte buffer and constructs a [MplexFrame] that is written into
     * the output list.
     * @param ctx the context.
     * @param msg the byte buffer.
     * @param out the list to write the extracted frame to.
     */
    override fun decode(ctx: C?, msg: ByteBuf, out: MutableList<MplexFrame>) {
        while (msg.isReadable) {
            val readerIndex = msg.readerIndex()
            val header = msg.readUvarint()
            val lenData = msg.readUvarint()
            if (header < 0 || lenData < 0) {
                // not enough data to read the frame length
                // will wait for more ...
                msg.readerIndex(readerIndex)
                return
            }
            if (lenData > maxFrameDataLength) {
                msg.skipBytes(msg.readableBytes())
                throw ProtocolViolationException("Mplex frame is too large: $lenData")
            }
            if (msg.readableBytes() < lenData) {
                // not enough data to read the frame content
                // will wait for more ...
                msg.readerIndex(readerIndex)
                return
            }
            val streamTag = header.and(0x07).toInt()
            val streamId = header.shr(3)
            val data = msg.readSlice(lenData.toInt())
            data.retain() // MessageToMessageCodec releases original buffer, but it needs to be relayed
            val initiator = if (streamTag == MplexFlags.NewStream) false else !MplexFlags.isInitiator(streamTag)
            val mplexFrame = MplexFrame(MuxId(streamId, initiator), streamTag, data)
            out.add(mplexFrame)
        }
    }
}