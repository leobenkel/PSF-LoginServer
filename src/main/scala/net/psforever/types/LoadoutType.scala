// Copyright (c) 2017 PSForever
package net.psforever.types

import net.psforever.packet.PacketHelpers
import scodec.Codec
import scodec.codecs.uint2L

object LoadoutType extends Enumeration {
  type Type = Value

  val Infantry, Vehicle, Battleframe = Value

  implicit val codec: Codec[LoadoutType.Value] = PacketHelpers.createEnumerationCodec(this, uint2L)
}
