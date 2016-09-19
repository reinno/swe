package swe.model

class ActivityTaskInfo(execution: Option[WorkflowExecution],
                       activityType: ActivityType,
                       startTimeStamp: String,
                       lastHeartBeatTimeStamp: String,
                       closeTimeStamp: String,
                       currentStatus: String,
                       closeStatus: String,
                       cancelRequested: Boolean)