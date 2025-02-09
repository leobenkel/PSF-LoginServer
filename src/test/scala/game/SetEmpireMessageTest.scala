// Copyright (c) 2017 PSForever
package game

import org.specs2.mutable._
import net.psforever.packet._
import net.psforever.packet.game._
import net.psforever.types.{PlanetSideEmpire, PlanetSideGUID}
import scodec.bits._

class SetEmpireMessageTest extends Specification {
private val string = hex"24 02 00 80"

  "decode" in {
    PacketCoding.decodePacket(string).require match {
      case SetEmpireMessage(object_guid, empire) =>
        object_guid mustEqual PlanetSideGUID(2)
        empire mustEqual PlanetSideEmpire.VS
      case _ =>
        ko
    }
  }

  "encode" in {
    val msg = SetEmpireMessage(PlanetSideGUID(2), PlanetSideEmpire.VS)
    val pkt = PacketCoding.encodePacket(msg).require.toByteVector

    pkt mustEqual string
  }
}
