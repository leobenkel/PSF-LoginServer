// Copyright (c) 2021 PSForever
package net.psforever.objects.serverobject.mount

import net.psforever.types.PlanetSideGUID

trait MountableEntity {
  private var bailProtection: Boolean = false

private def BailProtection: Boolean = bailProtection

private def BailProtection_=(protect: Boolean) = {
    bailProtection = protect
    BailProtection
  }

  private var mountedIn: Option[PlanetSideGUID] = None

private def MountedIn: Option[PlanetSideGUID] = mountedIn

private def MountedIn_=(cargo_guid: PlanetSideGUID): Option[PlanetSideGUID] = MountedIn_=(Some(cargo_guid))

private def MountedIn_=(cargo_guid: Option[PlanetSideGUID]): Option[PlanetSideGUID] = {
    mountedIn = cargo_guid
    MountedIn
  }
}
