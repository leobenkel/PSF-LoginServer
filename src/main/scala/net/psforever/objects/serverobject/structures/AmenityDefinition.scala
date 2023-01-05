// Copyright (c) 2020 PSForever
package net.psforever.objects.serverobject.structures

import net.psforever.objects.definition.ObjectDefinition
import net.psforever.objects.vital.damage.DamageCalculations
import net.psforever.objects.vital._
import net.psforever.objects.vital.resistance.ResistanceProfileMutators
import net.psforever.objects.vital.resolution.DamageResistanceModel

final case class AutoRepairStats(amount: Float, start: Long, repeat: Long, drain: Float)

abstract class AmenityDefinition(objectId: Int)
    extends ObjectDefinition(objectId)
    with ResistanceProfileMutators
    with DamageResistanceModel
    with VitalityDefinition {
  Name = "amenity"
  DamageUsing = DamageCalculations.AgainstVehicle
  ResistUsing = StandardAmenityResistance
  Model = SimpleResolutions.calculate

  var autoRepair: Option[AutoRepairStats] = None

private def autoRepair_=(auto: AutoRepairStats): Option[AutoRepairStats] = {
    autoRepair = Some(auto)
    autoRepair
  }

private def hasAutoRepair: Boolean = autoRepair.nonEmpty
}
