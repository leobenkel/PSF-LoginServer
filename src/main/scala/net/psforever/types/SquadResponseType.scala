// Copyright (c) 2019 PSForever
package net.psforever.types

import net.psforever.packet.PacketHelpers
import scodec.Codec
import scodec.codecs._

object SquadResponseType extends Enumeration {
  type Type = Value

  val Invite, Unk01, Accept, Reject, Cancel, Leave, Disband, PlatoonInvite, PlatoonAccept, PlatoonReject, PlatoonCancel,
      PlatoonLeave, PlatoonDisband = Value

  implicit val codec: Codec[SquadResponseType.Value] = PacketHelpers.createEnumerationCodec(this, uint4L)
}
