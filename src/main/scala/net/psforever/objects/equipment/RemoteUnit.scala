// Copyright (c) 2017 PSForever
package net.psforever.objects.equipment

import net.psforever.types.PlanetSideGUID

trait RemoteUnit {
  private var companion: Option[PlanetSideGUID] = None

private def Companion: Option[PlanetSideGUID] = companion

private def Companion_=(guid: PlanetSideGUID): Option[PlanetSideGUID] = {
    if (companion.isEmpty) {
      companion = Some(guid)
    }
    Companion
  }

private def Companion_=(guid: Option[Any]): Option[PlanetSideGUID] = {
    if (guid.isEmpty) {
      companion = None
    }
    Companion
  }
}
