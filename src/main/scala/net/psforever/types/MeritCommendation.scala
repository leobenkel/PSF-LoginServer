// Copyright (c) 2017 PSForever
package net.psforever.types

import scodec.{Attempt, Codec, Err}
import scodec.codecs._

/**
  * An `Enumeration` of all merit commendation award categories organized into associated ribbons.
  * By astonishing coincidence, with exception of the first ten special awards, the rest of list is in alphabetical order.
  */
object MeritCommendation extends Enumeration {
  type Type = Value

  val //0
  FanFaire2005Commander, FanFaire2005Soldier, FanFaire2006Atlanta, HalloweenMassacre2006NC, HalloweenMassacre2006TR,
      HalloweenMassacre2006VS, FanFaire2007, FanFaire2008, FanFaire2009, AdvancedMedic1,
  //10
  AdvancedMedic2, AdvancedMedic3, AdvancedMedic4, AdvancedMedic5, AdvancedMedic6, AdvancedMedic7, AdvancedMedicAssists1,
      AdvancedMedicAssists2, AdvancedMedicAssists3, AdvancedMedicAssists4,
  //20
  AdvancedMedicAssists5, AdvancedMedicAssists6, AdvancedMedicAssists7, AirDefender1, AirDefender2, AirDefender3,
      AirDefender4, AirDefender5, AirDefender6, AirDefender7,
  //30
  AMSSupport1, AMSSupport2, AMSSupport3, AMSSupport4, AMSSupport5, AMSSupport6, AMSSupport7, AntiVehicular1,
      AntiVehicular2, AntiVehicular3,
  //40
  AntiVehicular4, AntiVehicular5, AntiVehicular6, AntiVehicular7, Avenger1, Avenger2, Avenger3, Avenger4, Avenger5,
      Avenger6,
  //50
  Avenger7, BendingMovieActor, BFRAdvanced, BFRAdvanced2, BFRAdvanced3, BFRAdvanced4, BFRAdvanced5, BFRBuster1,
      BFRBuster2, BFRBuster3,
  //60
  BFRBuster4, BFRBuster5, BFRBuster6, BFRBuster7, BlackOpsHunter1, BlackOpsHunter2, BlackOpsHunter3, BlackOpsHunter4,
      BlackOpsHunter5, BlackOpsParticipant,
  //70
  BlackOpsVictory, Bombadier1, Bombadier2, Bombadier3, Bombadier4, Bombadier5, Bombadier6, Bombadier7, BomberAce1,
      BomberAce2,
  //80
  BomberAce3, BomberAce4, BomberAce5, BomberAce6, BomberAce7, Boomer1, Boomer2, Boomer3, Boomer4, Boomer5,
  //90
  Boomer6, Boomer7, CalvaryDriver1, CalvaryDriver2, CalvaryDriver3, CalvaryDriver4, CalvaryDriver5, CalvaryDriver6,
      CalvaryDriver7, CalvaryPilot,
  //100
  CalvaryPilot2, CalvaryPilot3, CalvaryPilot4, CalvaryPilot5, CalvaryPilot6, CalvaryPilot7, CMTopOutfit, CombatMedic,
      CombatMedic2, CombatMedic3,
  //110
  CombatMedic4, CombatMedic5, CombatMedic6, CombatMedic7, CombatRepair1, CombatRepair2, CombatRepair3, CombatRepair4,
      CombatRepair5, CombatRepair6,
  //120
  CombatRepair7, ContestFirstBR40, ContestMovieMaker, ContestMovieMakerOutfit, ContestPlayerOfTheMonth,
      ContestPlayerOfTheYear, CSAppreciation, DefenseNC1, DefenseNC2, DefenseNC3,
  //130
  DefenseNC4, DefenseNC5, DefenseNC6, DefenseNC7, DefenseTR1, DefenseTR2, DefenseTR3, DefenseTR4, DefenseTR5,
      DefenseTR6,
  //140
  DefenseTR7, DefenseVS1, DefenseVS2, DefenseVS3, DefenseVS4, DefenseVS5, DefenseVS6, DefenseVS7, DevilDogsMovie,
      DogFighter1,
  //150
  DogFighter2, DogFighter3, DogFighter4, DogFighter5, DogFighter6, DogFighter7, DriverGunner1, DriverGunner2,
      DriverGunner3, DriverGunner4,
  //160
  DriverGunner5, DriverGunner6, DriverGunner7, EliteAssault0, EliteAssault1, EliteAssault2, EliteAssault3,
      EliteAssault4, EliteAssault5, EliteAssault6,
  //170
  EliteAssault7, EmeraldVeteran, Engineer1, Engineer2, Engineer3, Engineer4, Engineer5, Engineer6, EquipmentSupport1,
      EquipmentSupport2,
  //180
  EquipmentSupport3, EquipmentSupport4, EquipmentSupport5, EquipmentSupport6, EquipmentSupport7, EventNCCommander,
      EventNCElite, EventNCSoldier, EventTRCommander, EventTRElite,
  //190
  EventTRSoldier, EventVSCommander, EventVSElite, EventVSSoldier, Explorer1, FiveYearNC, FiveYearTR, FiveYearVS,
      FourYearNC, FourYearTR,
  //200
  FourYearVS, GalaxySupport1, GalaxySupport2, GalaxySupport3, GalaxySupport4, GalaxySupport5, GalaxySupport6,
      GalaxySupport7, Grenade1, Grenade2,
  //210
  Grenade3, Grenade4, Grenade5, Grenade6, Grenade7, GroundGunner1, GroundGunner2, GroundGunner3, GroundGunner4,
      GroundGunner5,
  //220
  GroundGunner6, GroundGunner7, HackingSupport1, HackingSupport2, HackingSupport3, HackingSupport4, HackingSupport5,
      HackingSupport6, HackingSupport7, HeavyAssault1,
  //230
  HeavyAssault2, HeavyAssault3, HeavyAssault4, HeavyAssault5, HeavyAssault6, HeavyAssault7, HeavyInfantry,
      HeavyInfantry2, HeavyInfantry3, HeavyInfantry4,
  //240
  InfantryExpert1, InfantryExpert2, InfantryExpert3, Jacking, Jacking2, Jacking3, Jacking4, Jacking5, Jacking6,
      Jacking7,
  //250
  KnifeCombat1, KnifeCombat2, KnifeCombat3, KnifeCombat4, KnifeCombat5, KnifeCombat6, KnifeCombat7, LightInfantry,
      LockerCracker1, LockerCracker2,
  //260
  LockerCracker3, LockerCracker4, LockerCracker5, LockerCracker6, LockerCracker7, LodestarSupport1, LodestarSupport2,
      LodestarSupport3, LodestarSupport4, LodestarSupport5,
  //270
  LodestarSupport6, LodestarSupport7, Loser, Loser2, Loser3, Loser4, MarkovVeteran, Max1, Max2, Max3,
  //280
  Max4, Max5, Max6, MaxBuster1, MaxBuster2, MaxBuster3, MaxBuster4, MaxBuster5, MaxBuster6, MediumAssault1,
  //290
  MediumAssault2, MediumAssault3, MediumAssault4, MediumAssault5, MediumAssault6, MediumAssault7, OneYearNC, OneYearTR,
      OneYearVS, Orion1,
  //300
  Orion2, Orion3, Orion4, Orion5, Orion6, Orion7, Osprey1, Osprey2, Osprey3, Osprey4,
  //310
  Osprey5, Osprey6, Osprey7, Phalanx1, Phalanx2, Phalanx3, Phalanx4, Phalanx5, Phalanx6, Phalanx7,
  //320
  PSUMaAttendee, PSUMbAttendee, QAAppreciation, ReinforcementHackSpecialist, ReinforcementInfantrySpecialist,
      ReinforcementSpecialist, ReinforcementVehicleSpecialist, RouterSupport1, RouterSupport2, RouterSupport3,
  //330
  RouterSupport4, RouterSupport5, RouterSupport6, RouterSupport7, RouterTelepadDeploy1, RouterTelepadDeploy2,
      RouterTelepadDeploy3, RouterTelepadDeploy4, RouterTelepadDeploy5, RouterTelepadDeploy6,
  //340
  RouterTelepadDeploy7, ScavengerNC1, ScavengerNC2, ScavengerNC3, ScavengerNC4, ScavengerNC5, ScavengerNC6,
      ScavengerTR1, ScavengerTR2, ScavengerTR3,
  //350
  ScavengerTR4, ScavengerTR5, ScavengerTR6, ScavengerVS1, ScavengerVS2, ScavengerVS3, ScavengerVS4, ScavengerVS5,
      ScavengerVS6, SixYearNC,
  //360
  SixYearTR, SixYearVS, Sniper1, Sniper2, Sniper3, Sniper4, Sniper5, Sniper6, Sniper7, SpecialAssault1,
  //370
  SpecialAssault2, SpecialAssault3, SpecialAssault4, SpecialAssault5, SpecialAssault6, SpecialAssault7,
      StandardAssault1, StandardAssault2, StandardAssault3, StandardAssault4,
  //380
  StandardAssault5, StandardAssault6, StandardAssault7, StracticsHistorian, Supply1, Supply2, Supply3, Supply4, Supply5,
      Supply6,
  //390
  Supply7, TankBuster1, TankBuster2, TankBuster3, TankBuster4, TankBuster5, TankBuster6, TankBuster7, ThreeYearNC,
      ThreeYearTR,
  //400
  ThreeYearVS, TinyRoboticSupport1, TinyRoboticSupport2, TinyRoboticSupport3, TinyRoboticSupport4, TinyRoboticSupport5,
      TinyRoboticSupport6, TinyRoboticSupport7, Transport1, Transport2,
  //410
  Transport3, Transport4, Transport5, Transport6, Transport7, TransportationCitation1, TransportationCitation2,
      TransportationCitation3, TransportationCitation4, TransportationCitation5,
  //420
  TwoYearNC, TwoYearTR, TwoYearVS, ValentineFemale, ValentineMale, WernerVeteran, XmasGingerman, XmasSnowman,
      XmasSpirit = Value

  /*
  The value None requires special consideration.
  - A Long number is required for this Enumeration codec.
  - Enumerations are designed to only handle Int numbers.
  - A Codec only handles unsigned numbers.
  - The value of MeritCommendation.None is intended to be 0xFFFFFFFF, which (a) is 4294967295 as a Long, but (b) is -1 as an Integer.
  - Due to (a), an Enumeration can not be used to represent that number.
  - Due to (b), a Codec can not be used to convert to that number.
   */
  val None = Value(-1)

  /**
    * Carefully and explicitly convert between `Codec[Long] -> Long -> Int -> MeritCommendation.Value`.
    */
  implicit val codec: Codec[MeritCommendation.Value] = uint32L.exmap[MeritCommendation.Value](
    {
      case 0xffffffffL =>
        Attempt.successful(MeritCommendation.None)
      case n =>
        if (n > Int.MaxValue) {
          Attempt.failure(Err(s"value $n is too high, above maximum integer value ${Int.MaxValue}"))
        } else if (n > 429) { // TODO remove that. It's for use Xtoolspar.
          Attempt.failure(Err(s"value $n should not exist"))
        } else {
          Attempt.successful(MeritCommendation(n.toInt))
        }
    },
    {
      case MeritCommendation.None =>
        Attempt.successful(0xffffffffL)
      case enum =>
        Attempt.successful(enum.id.toLong)
    }
  )
}
