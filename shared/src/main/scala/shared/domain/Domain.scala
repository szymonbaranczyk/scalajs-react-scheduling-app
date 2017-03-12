package shared.domain

import java.util.Date


case class Meeting(id: Int, date: Date, isTaken: Boolean)

case class BookedMeeting(user: String, meeting: Meeting)