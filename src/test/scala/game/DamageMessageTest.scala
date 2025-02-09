// Copyright (c) 2019 PSForever
package game

import org.specs2.mutable._
import net.psforever.packet._
import net.psforever.packet.game._
import net.psforever.types.PlanetSideGUID
import scodec.bits._

class DamageMessageTest extends Specification {
private val string = hex"0b610b02610b00"

  "decode" in {
    PacketCoding.decodePacket(string).require match {
      case DamageMessage(guid1, unk1, guid2, unk2) =>
        guid1 mustEqual PlanetSideGUID(2913)
        unk1 mustEqual 2
        guid2 mustEqual PlanetSideGUID(2913)
        unk2 mustEqual false
      case _ =>
        ko
    }
  }

  "encode" in {
    val msg = DamageMessage(PlanetSideGUID(2913), 2, PlanetSideGUID(2913), false)
    val pkt = PacketCoding.encodePacket(msg).require.toByteVector

    pkt mustEqual string
  }
}
