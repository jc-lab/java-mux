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

import kr.jclab.mux.core.MuxFrame
import kr.jclab.mux.core.MuxFrame.Flag.CLOSE
import kr.jclab.mux.core.MuxFrame.Flag.DATA
import kr.jclab.mux.core.MuxFrame.Flag.OPEN
import kr.jclab.mux.core.MuxFrame.Flag.RESET
import kr.jclab.mux.core.ProtocolViolationException
import kotlin.jvm.Throws

/**
 * Contains all the permissible values for flags in the <code>mplex</code> protocol.
 */
object MplexFlags {
    const val NewStream = 0
    const val MessageReceiver = 1
    const val MessageInitiator = 2
    const val CloseReceiver = 3
    const val CloseInitiator = 4
    const val ResetReceiver = 5
    const val ResetInitiator = 6

    fun isInitiator(mplexFlag: Int) = mplexFlag % 2 == 0

    @Throws(ProtocolViolationException::class)
    fun toAbstractFlag(mplexFlag: Int): MuxFrame.Flag =
        when (mplexFlag) {
            NewStream -> OPEN
            MessageReceiver, MessageInitiator -> DATA
            CloseReceiver, CloseInitiator -> CLOSE
            ResetReceiver, ResetInitiator -> RESET
            else -> throw ProtocolViolationException("Unknown mplex flag: $mplexFlag")
        }

    fun toMplexFlag(abstractFlag: MuxFrame.Flag, initiator: Boolean): Int =
        when (abstractFlag) {
            OPEN -> NewStream
            DATA -> if (initiator) MessageInitiator else MessageReceiver
            CLOSE -> if (initiator) CloseInitiator else CloseReceiver
            RESET -> if (initiator) ResetInitiator else ResetReceiver
        }
}
