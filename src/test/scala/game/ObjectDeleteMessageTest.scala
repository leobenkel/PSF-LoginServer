// Copyright (c) 2017 PSForever
package game

import org.specs2.mutable._
import net.psforever.packet._
import net.psforever.packet.game._
import net.psforever.types.PlanetSideGUID
import scodec.bits._

class ObjectDeleteMessageTest extends Specification {
private val string = hex"19 4C00 00"

  "decode" in {
    PacketCoding.decodePacket(string).require match {
      case ObjectDeleteMessage(object_guid, unk1) =>
        object_guid mustEqual PlanetSideGUID(76)
        unk1 mustEqual 0
      case _ =>
        ko
    }
  }

  "encode" in {
    val msg = ObjectDeleteMessage(PlanetSideGUID(76), 0)
    val pkt = PacketCoding.encodePacket(msg).require.toByteVector

    pkt mustEqual string
  }
}
