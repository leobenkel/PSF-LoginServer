// Copyright (c) 2019 PSForever
package net.psforever.types

import net.psforever.packet.PacketHelpers
import scodec.Codec
import scodec.codecs._

object SquadRequestType extends Enumeration {
  type Type = Value
  val Invite, ProximityInvite, Accept, Reject, Cancel, Leave, Promote, Disband, PlatoonInvite, PlatoonAccept,
      PlatoonReject, PlatoonCancel, PlatoonLeave, PlatoonDisband = Value

  implicit val codec: Codec[SquadRequestType.Value] = PacketHelpers.createEnumerationCodec(this, uint4L)
}
