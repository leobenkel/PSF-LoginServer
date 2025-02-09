// Copyright (c) 2017 PSForever
package game.objectcreate

import net.psforever.packet.PacketCoding
import net.psforever.packet.game.{ObjectCreateDetailedMessage, ObjectCreateMessage}
import net.psforever.packet.game.objectcreate._
import net.psforever.types.{PlanetSideEmpire, PlanetSideGUID, Vector3}
import org.specs2.mutable._
import scodec.bits._

class RemoteProjectileDataTest extends Specification {
private val string_striker_projectile               = hex"17 C5000000 A4B 009D 4C129 0CB0A 9814 00 F5 E3 040000666686400"
private val string_hunter_seeker_missile_projectile = hex"17 c5000000 ca9 ab9e af127 ec465 3723 00 15 c4 2400009a99c9400"
private val string_oicw_little_buddy                = hex"18 ef000000 aca 3d0e 1ef35 d9417 2511 00 0f 21 d3bf0d1bc38900000000000fc"

  "RemoteProjectileData" should {
    "decode (striker_missile_targeting_projectile)" in {
      PacketCoding.decodePacket(string_striker_projectile).require match {
        case ObjectCreateMessage(len, cls, guid, parent, data) =>
          len mustEqual 197
          cls mustEqual ObjectClass.striker_missile_targeting_projectile
          guid mustEqual PlanetSideGUID(40192)
          parent.isDefined mustEqual false
          data match {
            case RemoteProjectileData(CommonFieldDataWithPlacement(pos, deploy), unk2, lim, unk3, unk4, unk5) =>
              pos.coord mustEqual Vector3(4644.5938f, 5472.0938f, 82.375f)
              pos.orient mustEqual Vector3(0, 30.9375f, 171.5625f)
              deploy.faction mustEqual PlanetSideEmpire.TR
              deploy.bops mustEqual false
              deploy.alternate mustEqual false
              deploy.v1 mustEqual true
              deploy.v2.isEmpty mustEqual true
              deploy.jammered mustEqual false
              deploy.v4.isEmpty mustEqual true
              deploy.v5.isEmpty mustEqual true
              deploy.guid mustEqual PlanetSideGUID(0)
              unk2 mustEqual 26214
              lim mustEqual 134
              unk3 mustEqual FlightPhysics.State4
              unk4 mustEqual 0
              unk5 mustEqual 0
            case _ =>
              ko
          }
        case _ =>
          ko
      }
    }

    "decode (hunter_seeker_missile_projectile)" in {
      PacketCoding.decodePacket(string_hunter_seeker_missile_projectile).require match {
        case ObjectCreateMessage(len, cls, guid, parent, data) =>
          len mustEqual 197
          cls mustEqual ObjectClass.hunter_seeker_missile_projectile
          guid mustEqual PlanetSideGUID(40619)
          parent.isDefined mustEqual false
          data match {
            case RemoteProjectileData(CommonFieldDataWithPlacement(pos, deploy), unk2, lim, unk3, unk4, unk5) =>
              pos.coord mustEqual Vector3(3621.3672f, 2701.8438f, 140.85938f)
              pos.orient mustEqual Vector3(0, 300.9375f, 258.75f)
              deploy.faction mustEqual PlanetSideEmpire.NC
              deploy.bops mustEqual false
              deploy.alternate mustEqual false
              deploy.v1 mustEqual true
              deploy.v2.isEmpty mustEqual true
              deploy.jammered mustEqual false
              deploy.v4.isEmpty mustEqual true
              deploy.v5.isEmpty mustEqual true
              deploy.guid mustEqual PlanetSideGUID(0)
              unk2 mustEqual 39577
              lim mustEqual 201
              unk3 mustEqual FlightPhysics.State4
              unk4 mustEqual 0
              unk5 mustEqual 0
            case _ =>
              ko
          }
        case _ =>
          ko
      }
    }

    "decode (oicw_little_buddy)" in {
      PacketCoding.decodePacket(string_oicw_little_buddy).require match {
        case ObjectCreateDetailedMessage(len, cls, guid, parent, data) =>
          len mustEqual 239
          cls mustEqual ObjectClass.oicw_little_buddy
          guid mustEqual PlanetSideGUID(3645)
          parent.isDefined mustEqual false
          data match {
            case LittleBuddyProjectileData(dat, u2, u4) =>
              dat.pos.coord mustEqual Vector3(3046.2344f, 3715.6953f, 68.578125f)
              dat.pos.orient mustEqual Vector3(0, 317.8125f, 357.1875f)
              dat.pos.vel.contains(Vector3(-10.0125f, 101.475f, -101.7f)) mustEqual true
              dat.data.faction mustEqual PlanetSideEmpire.NC
              dat.data.bops mustEqual false
              dat.data.alternate mustEqual false
              dat.data.v1 mustEqual true
              dat.data.v2.isEmpty mustEqual true
              dat.data.jammered mustEqual false
              dat.data.v4.isEmpty mustEqual true
              dat.data.v5.isEmpty mustEqual true
              dat.data.guid mustEqual PlanetSideGUID(0)
              u2 mustEqual 0
              u4 mustEqual true
            case _ =>
              ko
          }
        case _ =>
          ko
      }
    }

    "encode (striker_missile_targeting_projectile)" in {
      val obj = RemoteProjectileData(
        CommonFieldDataWithPlacement(
          PlacementData(4644.5938f, 5472.0938f, 82.375f, 0f, 30.9375f, 171.5625f),
          CommonFieldData(PlanetSideEmpire.TR, false, false, true, None, false, None, None, PlanetSideGUID(0))
        ),
        26214,
        134,
        FlightPhysics.State4,
        0,
        0
      )
      val msg = ObjectCreateMessage(ObjectClass.striker_missile_targeting_projectile, PlanetSideGUID(40192), obj)
      val pkt = PacketCoding.encodePacket(msg).require.toByteVector
      //pkt mustEqual string_striker_projectile

      pkt.toBitVector.take(132) mustEqual string_striker_projectile.toBitVector.take(132)
      pkt.toBitVector.drop(133).take(7) mustEqual string_striker_projectile.toBitVector.drop(133).take(7)
      pkt.toBitVector.drop(141) mustEqual string_striker_projectile.toBitVector.drop(141)
    }

    "encode (hunter_seeker_missile_projectile)" in {
      val obj = RemoteProjectileData(
        CommonFieldDataWithPlacement(
          PlacementData(3621.3672f, 2701.8438f, 140.85938f, 0, 300.9375f, 258.75f),
          CommonFieldData(PlanetSideEmpire.NC, false, false, true, None, false, None, None, PlanetSideGUID(0))
        ),
        39577,
        201,
        FlightPhysics.State4,
        0,
        0
      )
      val msg = ObjectCreateMessage(ObjectClass.hunter_seeker_missile_projectile, PlanetSideGUID(40619), obj)
      val pkt = PacketCoding.encodePacket(msg).require.toByteVector
      //pkt mustEqual string_hunter_seeker_missile_projectile

      pkt.toBitVector.take(132) mustEqual string_hunter_seeker_missile_projectile.toBitVector.take(132)
      pkt.toBitVector.drop(133).take(7) mustEqual string_hunter_seeker_missile_projectile.toBitVector.drop(133).take(7)
      pkt.toBitVector.drop(141) mustEqual string_hunter_seeker_missile_projectile.toBitVector.drop(141)
    }

    "encode (oicw_little_buddy)" in {
      val obj = LittleBuddyProjectileData(
        CommonFieldDataWithPlacement(
            PlacementData(Vector3(3046.2344f, 3715.6953f, 68.578125f),
            Vector3(0, 317.8125f, 357.1875f),
            Some(Vector3(-10.0125f, 101.475f, -101.7f))
          ),
          CommonFieldData(
            PlanetSideEmpire.NC,
            bops = false,
            alternate = false,
            v1 = true,
            v2 = None,
            jammered = false,
            v4 = None,
            v5 = None,
            guid = PlanetSideGUID(0)
          )
        ),
        u2 = 0,
        u4 = true
      )
      val msg = ObjectCreateDetailedMessage(ObjectClass.oicw_little_buddy, PlanetSideGUID(3645), obj)
      val pkt = PacketCoding.encodePacket(msg).require.toByteVector
      pkt mustEqual string_oicw_little_buddy
    }
  }
}
