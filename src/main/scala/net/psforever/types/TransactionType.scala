// Copyright (c) 2017 PSForever
package net.psforever.types

import net.psforever.packet.PacketHelpers
import scodec.Codec
import scodec.codecs._

// TODO: Use sealed trait instead of Enumeration
object TransactionType extends Enumeration {
  type Type = Value
  val Unk0, Learn, // certif term or Buy (v-term)
  Buy, Sell,       // or forget on certif term
  Unk4, Unk5, Loadout, Unk7 = Value

  implicit val codec: Codec[TransactionType.Value] = PacketHelpers.createEnumerationCodec(this, uintL(3))
}
