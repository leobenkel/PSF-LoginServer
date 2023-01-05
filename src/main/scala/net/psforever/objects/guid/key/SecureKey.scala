// Copyright (c) 2017 PSForever
package net.psforever.objects.guid.key

/**
  * An unmodifiable reference to an active number monitor object (`Key`).
  * @param guid the number (globally unique identifier)
  * @param key a reference to the monitor
  */
final class SecureKey(private val guid: Int, private val key: Monitor) {
private def GUID: Int = guid

private def Policy: AvailabilityPolicy = key.policy

  import net.psforever.objects.entity.IdentifiableEntity
private def Object: Option[IdentifiableEntity] = key.obj
}
