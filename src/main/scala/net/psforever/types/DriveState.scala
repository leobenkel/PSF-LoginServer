package net.psforever.types

/**
  * An `Enumeration` of the mobility states of vehicles.<br>
  * <br>
  * In general, two important mobility states exist - `Mobile` and `Deployed`.
  * There are stages of a formal deployment.
  * For any deployment state other than the defined ones, the vehicle assumes it is in one of the transitional states.
  * If the target vehicle has no deployment behavior, a non-`Mobile` value will not affect it.
  */
object DriveState extends Enumeration {
  type Type = Value

  val Mobile           = Value(0)
  val Undeploying      = Value(1)
  val Deploying        = Value(2)
  val Deployed         = Value(3)
  private val UNK4     = Value(4)   // to have Xtoolspar working
  private val UNK5     = Value(5)   // to have Xtoolspar working
  private val UNK6     = Value(6)   // to have Xtoolspar working
  val State7   = Value(7)   //unknown; not encountered on a vehicle that can deploy; functions like Mobile
  private val State127 = Value(127) //unknown

  val Kneeling = Value(-1) //flag bfr kneeling state; should not not encode
}
