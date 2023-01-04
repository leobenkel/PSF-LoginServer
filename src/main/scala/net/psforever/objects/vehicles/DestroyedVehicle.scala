// Copyright (c) 2017 PSForever
package net.psforever.objects.vehicles

/**
  */
object DestroyedVehicle extends Enumeration {
  type Type = Value

  val Ams                        = Value(47)
  val Ant                        = Value(61)
  val Apc                        = Value(65)
  val Dropship                   = Value(260)
  val Flail                      = Value(295)
  val Liberator                  = Value(439)
  private val LightGunship       = Value(442)
  private val Lightning          = Value(447)
  private val Lodestar           = Value(460)
  private val Magrider           = Value(471)
  val Mosquito                   = Value(573)
  val MediumTransport            = Value(533)
  val Prowler                    = Value(698)
  private val QuadAssault        = Value(708)
  private val QuadStealth        = Value(711)
  val Router                     = Value(742)
  val Skyguard                   = Value(785)
  private val Switchblade        = Value(848)
  private val ThreeManHeavyBuggy = Value(863)
  private val TwoManAssaultBuggy = Value(897)
  private val TwoManHeavyBuggy   = Value(899)
  val TwoManHoverBuggy           = Value(901)
  val Vanguard                   = Value(924)
}
