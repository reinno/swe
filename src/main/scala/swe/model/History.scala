package swe.model

case class History(events: List[HistoryEvent],
                   nextPageToken: String)
