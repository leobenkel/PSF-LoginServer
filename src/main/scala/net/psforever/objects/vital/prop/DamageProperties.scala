// Copyright (c) 2020 PSForever
package net.psforever.objects.vital.prop

import net.psforever.objects.ballistics.{AggravatedDamage, ChargeDamage}
import net.psforever.objects.equipment.JammingUnit
import net.psforever.objects.vital.base.{DamageModifiers, DamageType}
import net.psforever.objects.vital.damage.StandardDamageProfile

/**
  * Information that explains aspects of the the damage being performed that go beyond simple numbers.
  * Activation of these "special effects" may or may not even require the damage to be countable
  * which is the context in which it is formally normalized.
  */
trait DamageProperties extends StandardDamageProfile with JammingUnit with DamageModifiers {

  /** the type of damage cuased */
  private var damageType: DamageType.Value = DamageType.None

  /** an auxiliary type of damage caused */
  private var damageTypeSecondary: DamageType.Value = DamageType.None

  /** against Infantry targets, damage does not apply to armor damage */
  private var damageToHealthOnly: Boolean = false

  /** against Vehicle targets, damage does not apply to vehicle shield */
  private var damageToVehicleOnly: Boolean = false

  /** against battleframe targets, damage does not apply to battleframe robotics shield;
    * this is equivalent to the property "bfr_permeate_shield"
    */
  private var damageToBFROnly: Boolean = false

  /** use a specific modifier as a part of damage calculations */
  private var useDamage1Subtract: Boolean = false

  /** some other entity confers damage;
    * a set value should be the damager's object uid
    * usually corresponding to a projectile;
    * also used to produce staged projectiles
    */
  private var damageProxy: List[Int] = Nil

  /** na;
    * currently used with jammer properties only
    */
  private var additionalEffect: Boolean = false

  /** confers aggravated damage burn to its target */
  private var aggravatedDamage: Option[AggravatedDamage] = None

  /** modifies based on some measure of time */
  private var charging: Option[ChargeDamage] = None

  /** a destroyed mine will detonate rather than fizzle-out */
  private var sympathy: Boolean = false

  private def UseDamage1Subtract: Boolean = useDamage1Subtract

  private def UseDamage1Subtract_=(useDamage1Subtract: Boolean): Boolean = {
    this.useDamage1Subtract = useDamage1Subtract
    UseDamage1Subtract
  }

  def CausesDamageType: DamageType.Value = damageType

  def CausesDamageType_=(damageType1: DamageType.Value): DamageType.Value = {
    damageType = damageType1
    CausesDamageType
  }

  def CausesDamageTypeSecondary: DamageType.Value = damageTypeSecondary

  def CausesDamageTypeSecondary_=(damageTypeSecondary1: DamageType.Value): DamageType.Value = {
    damageTypeSecondary = damageTypeSecondary1
    CausesDamageTypeSecondary
  }

  def AllDamageTypes: Set[DamageType.Value] = {
    Set(damageType, damageTypeSecondary).filterNot(_ == DamageType.None)
  }

  private def DamageToHealthOnly: Boolean = damageToHealthOnly

  private def DamageToHealthOnly_=(healthOnly: Boolean): Boolean = {
    damageToHealthOnly = healthOnly
    DamageToHealthOnly
  }

  private def DamageToVehicleOnly: Boolean = damageToVehicleOnly

  private def DamageToVehicleOnly_=(vehicleOnly: Boolean): Boolean = {
    damageToVehicleOnly = vehicleOnly
    DamageToVehicleOnly
  }

  private def DamageToBattleframeOnly: Boolean = damageToBFROnly

  private def DamageToBattleframeOnly_=(bfrOnly: Boolean): Boolean = {
    damageToBFROnly = bfrOnly
    DamageToBattleframeOnly
  }

  private def DamageProxy: List[Int] = damageProxy

  private def DamageProxy_=(proxyObjectId: Int): List[Int] = {
    damageProxy = damageProxy :+ proxyObjectId
    DamageProxy
  }

  private def DamageProxy_=(proxyObjectId: List[Int]): List[Int] = {
    damageProxy = proxyObjectId
    DamageProxy
  }

  private def AdditionalEffect: Boolean = additionalEffect

  private def AdditionalEffect_=(effect: Boolean): Boolean = {
    additionalEffect = effect
    AdditionalEffect
  }

  private def Aggravated: Option[AggravatedDamage] = aggravatedDamage

  private def Aggravated_=(damage: AggravatedDamage): Option[AggravatedDamage] = Aggravated_=(Some(damage))

  private def Aggravated_=(damage: Option[AggravatedDamage]): Option[AggravatedDamage] = {
    aggravatedDamage = damage
    Aggravated
  }

  private def Charging: Option[ChargeDamage] = charging

  private def Charging_=(damage: ChargeDamage): Option[ChargeDamage] = Charging_=(Some(damage))

  private def Charging_=(damage: Option[ChargeDamage]): Option[ChargeDamage] = {
    charging = damage
    Charging
  }

  private def SympatheticExplosion: Boolean = sympathy

  private def SympatheticExplosion_=(chain: Boolean): Boolean = {
    sympathy = chain
    SympatheticExplosion
  }
}
