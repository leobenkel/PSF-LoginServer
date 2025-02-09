// Copyright (c) 2017 PSForever
package game

import org.specs2.mutable._
import net.psforever.packet._
import net.psforever.packet.game._
import scodec.bits._

class CharacterNoRecordMessageTest extends Specification {
private val string = hex"13 00400000" //we have no record of this packet, so here's something fake that works

  "decode" in {
    PacketCoding.decodePacket(string).require match {
      case CharacterNoRecordMessage(unk) =>
        unk mustEqual 16384
      case _ =>
        ko
    }
  }

  "encode" in {
    val msg = CharacterNoRecordMessage(16384)
    val pkt = PacketCoding.encodePacket(msg).require.toByteVector

    pkt mustEqual string
  }
}
