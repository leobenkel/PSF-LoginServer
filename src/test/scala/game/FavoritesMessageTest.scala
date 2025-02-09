// Copyright (c) 2017 PSForever
package game

import org.specs2.mutable._
import net.psforever.packet._
import net.psforever.packet.game._
import net.psforever.types.{LoadoutType, PlanetSideGUID}
import scodec.bits._

class FavoritesMessageTest extends Specification {
private val stringVehicles = hex"60	5C 84	02 20 5300 6B00 7900 6700 7500 6100 7200 6400"
private val stringInfantry = hex"60 2C 03 82 34 4100 6700 6900 6C00 6500 2000 2800 6200 6100 7300 6900 6300 2900 20"

  "decode (for infantry)" in {
    PacketCoding.decodePacket(stringInfantry).require match {
      case FavoritesMessage(list, player_guid, line, label, armor) =>
        list mustEqual LoadoutType.Infantry
        player_guid mustEqual PlanetSideGUID(3760)
        line mustEqual 0
        label mustEqual "Agile (basic)"
        armor.isDefined mustEqual true
        armor.get mustEqual 1
      case _ =>
        ko
    }
  }

  "encode (for infantry)" in {
    val msg = FavoritesMessage.Infantry(PlanetSideGUID(3760), line = 0, label = "Agile (basic)", armor = 1)
    val pkt = PacketCoding.encodePacket(msg).require.toByteVector

    pkt mustEqual stringInfantry
  }

  "decode (for vehicles)" in {
    PacketCoding.decodePacket(stringVehicles).require match {
      case FavoritesMessage(list, player_guid, line, label, armor) =>
        list mustEqual LoadoutType.Vehicle
        player_guid mustEqual PlanetSideGUID(4210)
        line mustEqual 0
        label mustEqual "Skyguard"
        armor.isDefined mustEqual false
      case _ =>
        ko
    }
  }

  "encode (for vehicles)" in {
    val msg = FavoritesMessage.Vehicle(PlanetSideGUID(4210), line = 0, label = "Skyguard")
    val pkt = PacketCoding.encodePacket(msg).require.toByteVector

    pkt mustEqual stringVehicles
  }
}
