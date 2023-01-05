// Copyright (c) 2021 PSForever
package net.psforever.objects.serverobject.mount

import net.psforever.objects.definition.BasicDefinition

trait MountableSpaceDefinition[A]
  extends BasicDefinition {
private def occupancy: Int

private def restriction: MountRestriction[A]

private def bailable: Boolean
}
