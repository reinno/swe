package swe.model

case class HistoryEvent(id: Long,
                        eventType: String,
                        timestamp: String,
                        attributes: EventAttributes)
