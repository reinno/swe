package swe.model

sealed abstract class ChildPolicy(val value: String)

object ChildPolicy {
  case object Terminate extends ChildPolicy("TERMINATE")
  case object RequestCancel extends ChildPolicy("REQUEST_CANCEL")
  case object Abandon extends ChildPolicy("ABANDON")

  def unapply(s: String): Option[ChildPolicy] =
    s match {
      case "TERMINATE"      => Some(Terminate)
      case "REQUEST_CANCEL" => Some(RequestCancel)
      case "ABANDON"        => Some(Abandon)
      case _                => None
    }
}