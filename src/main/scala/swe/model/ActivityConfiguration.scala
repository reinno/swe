package swe.model

case class ActivityConfiguration(startToCloseTimeoutSec: Int,
                                 heartBeatTimeoutSec: Int,
                                 scheduleToStartTimeoutSec: Int,
                                 scheduleToEndTimeoutSec: Int,
                                 priority: String)
