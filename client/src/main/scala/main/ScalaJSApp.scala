package main


import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import moment._
import org.scalajs.dom._
import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
/**
  * Created by Szymon Barańczyk on 2017-02-13.
  */
case class MomentMeeting(id: Int, date: Date, isTaken: Boolean)
@JSExport
object ScalaJSApp extends js.JSApp {

  @scala.scalajs.js.annotation.JSExport
  override def main(): Unit = {
    case class State(viewed: Date, selected: Option[Date])
    case class Props(today: Date, available: List[Date])
    case class DayProps(day: Date, backend: BackendCalendar, isToday: Boolean, isSelected: Boolean, isThisMonth: Boolean, isMeeting: Boolean, isInPast: Boolean)
    case class WeekProps(date: Date, backend: BackendCalendar, viewed: Date, selected: Option[Date], today: Date, available: List[Date])

    val Day = ReactComponentB[DayProps]("Day")
      .render_P(p => {
        val onClick = if (p.isThisMonth) p.backend.select(p.day)(_) else (e: ReactEvent) => Callback {}
        val `class` = "day" + (if (!p.isThisMonth || p.isInPast) " inactive " else "") +
          (if (p.isSelected) " selected " else "") +
          (if (p.isToday) " today " else "")
        <.td(
          p.day.format("D"),
          ^.onClick ==> onClick,
          ^.`class` := `class`
        )
      })
      .build
    val MonthTab = ReactComponentB[(Date, BackendCalendar)]("MonthTab")
      .render_P { case (d, b) => <.tr(
        <.th(^.colSpan := 7,
          <.span("<", ^.onClick ==> b.prev),
          <.span(d.format("MMMM")),
          <.span(">", ^.onClick ==> b.next)
        ))
      }
      .build
    val Week = ReactComponentB[WeekProps]("Week")
      .render_P(p => {
        val dateIter = Moment(p.date)
        val matchedMeeting: Option[Date] = p.available.find(d => d.isSame(p.date, "day"))
        val days = (0 to 6).map(_ => {
          val tag = Day(DayProps(Moment(dateIter), p.backend,
            isToday = dateIter.isSame(p.today, "day"),
            isSelected = p.selected match {
              case Some(date) => date.isSame(dateIter, "day")
              case None => false
            },
            isThisMonth = dateIter.month() == p.viewed.month(),
            isMeeting = matchedMeeting match {
              case Some(_) => true
              case None => false
            },
            isInPast = p.today.isAfter(dateIter, "day")))
          dateIter.add(1, "day")
          tag
        })
        <.tr(days)
      }).build
    class BackendCalendar($: BackendScope[Props, State]) {
      def select(d: Date)(e: ReactEvent): Callback = {
        console.log("clicked " + d.format("DD-MM-YYYY"))
        $.modState(state => State(state.viewed, Some(d)))
      }

      def prev(e: ReactEvent): Callback = {
        console.log("prev")
        $.modState(state => State(state.viewed.add(-1, "month"), state.selected))
      }

      def next(e: ReactEvent): Callback = {
        console.log("next")
        $.modState(state => State(state.viewed.add(1, "month"), state.selected))
      }

      def render(props: Props, state: State) = {
        val startOfTheMonth = Moment(state.viewed).startOf("month")
        val startOfTheWeek = Moment(startOfTheMonth).startOf("week")
        val weeks = ListBuffer[TagMod]()
        while (startOfTheWeek.month() != state.viewed.month() + 1) {
          weeks += Week(WeekProps(Moment(startOfTheWeek), this, Moment(state.viewed), state.selected, props.today, props.available))
          startOfTheWeek.add(1, "week")
        }
        <.table(<.tbody(MonthTab((Moment(startOfTheMonth), this)),
          weeks))
      }
    }
    val today: Date = Moment()
    val Calendar = ReactComponentB[Props]("Calendar")
      .initialState(State(Moment(today), None))
      .renderBackend[BackendCalendar]
      .build
    Ajax.get("/meetings").onSuccess { case xhr =>
      console.log(read[Seq[(Int, Int, Boolean)]](xhr.responseText).map(t => MomentMeeting(t._1, Moment(t._2), t._3)))
    }
    ReactDOM.render(Calendar(Props(today, List[Date]())), document.getElementById("here"))
  }

}


