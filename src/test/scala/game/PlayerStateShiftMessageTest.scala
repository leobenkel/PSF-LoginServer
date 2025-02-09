// Copyright (c) 2017 PSForever
package game

import org.specs2.mutable._
import net.psforever.packet._
import net.psforever.packet.game.{PlayerStateShiftMessage, ShiftState}
import net.psforever.types.Vector3
import scodec.bits._

class PlayerStateShiftMessageTest extends Specification {
private val string_short     = hex"BE 68"
private val string_pos       = hex"BE 95 A0 89 13 91 B8 B0 B7 F0" //orig: ... B0 BF F0
private val string_posAndVel = hex"BE AE 01 29 CD 59 B9 40 C0 EA D4 00 0F 86 40"

  "decode (short)" in {
    PacketCoding.decodePacket(string_short).require match {
      case PlayerStateShiftMessage(state, unk) =>
        state.isDefined mustEqual false
        unk.isDefined mustEqual true
        unk.get mustEqual 5
      case _ =>
        ko
    }
  }

  "decode (pos)" in {
    PacketCoding.decodePacket(string_pos).require match {
      case PlayerStateShiftMessage(state, unk) =>
        state.isDefined mustEqual true
        state.get.unk mustEqual 1
        state.get.pos.x mustEqual 4624.703f
        state.get.pos.y mustEqual 5922.1484f
        state.get.pos.z mustEqual 46.171875f
        state.get.viewYawLim mustEqual 92.8125f
        state.get.vel.isDefined mustEqual false
        unk.isDefined mustEqual false
      case _ =>
        ko
    }
  }

  "decode (pos and vel)" in {
    PacketCoding.decodePacket(string_posAndVel).require match {
      case PlayerStateShiftMessage(state, unk) =>
        state.isDefined mustEqual true
        state.get.unk mustEqual 2
        state.get.pos.x mustEqual 4645.75f
        state.get.pos.y mustEqual 5811.6016f
        state.get.pos.z mustEqual 50.3125f
        state.get.viewYawLim mustEqual 50.625f
        state.get.vel.isDefined mustEqual true
        state.get.vel.get.x mustEqual 10.125f
        state.get.vel.get.y mustEqual -28.8f
        state.get.vel.get.z mustEqual 1.3499999f
        unk.isDefined mustEqual false
      case _ =>
        ko
    }
  }

  "encode (short)" in {
    val msg = PlayerStateShiftMessage(5)
    val pkt = PacketCoding.encodePacket(msg).require.toByteVector

    pkt mustEqual string_short
  }

  "encode (pos)" in {
    val msg = PlayerStateShiftMessage(ShiftState(1, Vector3(4624.703f, 5922.1484f, 46.171875f), 92.8125f))
    val pkt = PacketCoding.encodePacket(msg).require.toByteVector

    pkt mustEqual string_pos
  }

  "encode (pos and vel)" in {
    val msg = PlayerStateShiftMessage(
      ShiftState(2, Vector3(4645.75f, 5811.6016f, 50.3125f), 50.625f, Vector3(10.125f, -28.8f, 1.3499999f))
    )
    val pkt = PacketCoding.encodePacket(msg).require.toByteVector

    pkt mustEqual string_posAndVel
  }
}
