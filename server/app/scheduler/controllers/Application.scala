package scheduler.controllers

import java.util.Date
import javax.inject.Inject

import play.api.mvc._
import scheduler.helpers.JsonParser
import shared.domain.{BookedMeeting, Meeting}
import upickle.default._
/**
  * Created by Szymon Barańczyk on 2017-02-13.
  */
class Application @Inject() extends Controller with JsonParser {

  def index = Action {
    Ok(scheduler.views.html.index("Nothing for now"))
  }

  def listMeetings = Action {
    val date = new Date()
    date.setDate(date.getDate + 1)
    val date2 = new Date()
    date2.setDate(date.getDate + 4)
    val meetings = List(Meeting(1, new Date(), false), Meeting(1, date, false), Meeting(1, date2, true))
    val json = write(meetings)
    Ok(json).withHeaders(("Content-Type", "application/json"))
  }

  def book = Action(parse.text) { request =>
    val meeting = read[BookedMeeting](request.body)
    println(meeting)
    NoContent
  }

}
