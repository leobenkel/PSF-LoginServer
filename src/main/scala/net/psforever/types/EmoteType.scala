// Copyright (c) 2017 PSForever
package net.psforever.types

import net.psforever.packet.PacketHelpers
import scodec.Codec
import scodec.codecs._

object EmoteType extends Enumeration {
  type Type = Value
  val Charge, Halt, Nod, Stinky, Wave, Bow, CabbagePatch, Cheer, ChestThump, Choking, Dunno, Fistup, Followme, Help,
      Laugh, Move, No, // TODO: Double check this one, doesn't seem to have an associated slash command
  Roundup, Salute, Sorry, Spreadout, Thumbsdown = Value

  implicit val codec: Codec[EmoteType.Value] = PacketHelpers.createEnumerationCodec(this, uint8L)
}
