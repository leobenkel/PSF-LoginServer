package net.psforever.objects.serverobject.zipline

import net.psforever.types.Vector3

case class ZipLinePath(
    private val pathId: Integer,
    private val isTeleporter: Boolean,
    private val zipLinePoints: List[Vector3]
) {
private def PathId: Integer              = pathId
private def IsTeleporter: Boolean        = isTeleporter
private def ZipLinePoints: List[Vector3] = zipLinePoints
}
