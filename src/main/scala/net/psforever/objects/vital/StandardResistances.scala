// Copyright (c) 2017 PSForever
package net.psforever.objects.vital

import net.psforever.objects.vital.resistance.{ResistanceCalculations, ResistanceSelection}

object NoResistance
    extends ResistanceCalculations(
      ResistanceCalculations.AlwaysValidTarget,
      ResistanceCalculations.NoResistExtractor
    )

object InfantryHitResistance
    extends ResistanceCalculations(
      ResistanceCalculations.ValidInfantryTarget,
      ResistanceCalculations.ExoSuitDirectExtractor
    )

object InfantrySplashResistance
    extends ResistanceCalculations(
      ResistanceCalculations.ValidInfantryTarget,
      ResistanceCalculations.ExoSuitSplashExtractor
    )

object InfantryLashResistance
    extends ResistanceCalculations(
      ResistanceCalculations.ValidInfantryTarget,
      ResistanceCalculations.MaximumResistance
    )

object InfantryAggravatedResistance
    extends ResistanceCalculations(
      ResistanceCalculations.ValidInfantryTarget,
      ResistanceCalculations.ExoSuitAggravatedExtractor
    )

object VehicleHitResistance
    extends ResistanceCalculations(
      ResistanceCalculations.ValidVehicleTarget,
      ResistanceCalculations.VehicleDirectExtractor
    )

object VehicleSplashResistance
    extends ResistanceCalculations(
      ResistanceCalculations.ValidVehicleTarget,
      ResistanceCalculations.VehicleSplashExtractor
    )

object VehicleLashResistance
    extends ResistanceCalculations(
      ResistanceCalculations.ValidVehicleTarget,
      ResistanceCalculations.NoResistExtractor
    )

object VehicleAggravatedResistance
    extends ResistanceCalculations(
      ResistanceCalculations.ValidVehicleTarget,
      ResistanceCalculations.VehicleAggravatedExtractor
    )

object AmenityHitResistance
    extends ResistanceCalculations(
      ResistanceCalculations.ValidAmenityTarget,
      ResistanceCalculations.OtherDirectExtractor
    )

object AmenitySplashResistance
    extends ResistanceCalculations(
      ResistanceCalculations.ValidAmenityTarget,
      ResistanceCalculations.OtherSplashExtractor
    )

object NoResistanceSelection extends ResistanceSelection {
  private def Direct: ResistanceSelection.Format     = NoResistance.Calculate
  private def Splash: ResistanceSelection.Format     = NoResistance.Calculate
  private def Lash: ResistanceSelection.Format       = NoResistance.Calculate
  private def Aggravated: ResistanceSelection.Format = NoResistance.Calculate
  private def Radiation: ResistanceSelection.Format  = ResistanceSelection.None
}

object StandardInfantryResistance extends ResistanceSelection {
  private def Direct: ResistanceSelection.Format     = InfantryHitResistance.Calculate
  private def Splash: ResistanceSelection.Format     = InfantrySplashResistance.Calculate
  private def Lash: ResistanceSelection.Format       = InfantryLashResistance.Calculate
  private def Aggravated: ResistanceSelection.Format = InfantryAggravatedResistance.Calculate
  private def Radiation: ResistanceSelection.Format  = InfantrySplashResistance.Calculate
}

object StandardVehicleResistance extends ResistanceSelection {
  private def Direct: ResistanceSelection.Format     = VehicleHitResistance.Calculate
  private def Splash: ResistanceSelection.Format     = VehicleSplashResistance.Calculate
  private def Lash: ResistanceSelection.Format       = VehicleLashResistance.Calculate
  private def Aggravated: ResistanceSelection.Format = VehicleAggravatedResistance.Calculate
  private def Radiation: ResistanceSelection.Format  = ResistanceSelection.None
}

object StandardAmenityResistance extends ResistanceSelection {
  private def Direct: ResistanceSelection.Format     = AmenityHitResistance.Calculate
  private def Splash: ResistanceSelection.Format     = AmenityHitResistance.Calculate
  private def Lash: ResistanceSelection.Format       = ResistanceSelection.None
  private def Aggravated: ResistanceSelection.Format = ResistanceSelection.None
  private def Radiation: ResistanceSelection.Format  = ResistanceSelection.None
}
