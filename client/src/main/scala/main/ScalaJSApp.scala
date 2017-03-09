package main


import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.vdom.html_<^._
import moment._
import org.scalajs.dom._
import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.collection.immutable.Seq
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
/**
  * Created by Szymon Barańczyk on 2017-02-13.
  */
case class MomentMeeting(id: Int, date: Date, isTaken: Boolean)

case class ParsedMeeting(id: Int, date: Long, isTaken: Boolean)
@JSExport
object ScalaJSApp extends js.JSApp {

  @scala.scalajs.js.annotation.JSExport
  override def main(): Unit = {
    case class State(viewed: Date, selected: Option[Date])
    case class Props(today: Date, available: Seq[MomentMeeting])
    case class DayProps(day: Date, backend: BackendCalendar, isToday: Boolean, isSelected: Boolean, isThisMonth: Boolean, isMeeting: Boolean, isInPast: Boolean)
    case class WeekProps(date: Date, backend: BackendCalendar, viewed: Date, selected: Option[Date], today: Date, available: Seq[Date])

    val Day = ScalaComponent.build[DayProps]("Day")
      .render_P(p => {
        val onClick = if (p.isThisMonth && !p.isInPast && p.isMeeting) p.backend.select(p.day)(_) else (e: ReactEvent) => Callback {}
        val `class` = "day" + (if (!p.isThisMonth || p.isInPast) " inactive" else "") +
          (if (p.isSelected) " selected" else "") +
          (if (p.isMeeting) " meeting" else "") +
          (if (p.isToday) " today" else "")

        <.td(
          p.day.format("D"),
          ^.onClick ==> onClick,
          ^.`class` := `class`
        )
      })
      .build
    val MonthTab = ScalaComponent.build[(Date, BackendCalendar)]("MonthTab")
      .render_P { case (d, b) => <.tr(
        <.th(^.colSpan := 7,
          <.span("<", ^.onClick ==> b.prev),
          <.span(d.format("MMMM")),
          <.span(">", ^.onClick ==> b.next)
        ))
      }
      .build
    val Week = ScalaComponent.build[WeekProps]("Week")
      .render_P(p => {
        val dateIter = Moment(p.date)
        val days = (0 to 6).map(_ => {
          val matchedMeeting: Option[Date] = p.available.find(d => d.isSame(dateIter, "day"))
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
        <.tr(days.toVdomArray)
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
        val uniqueMeetingDays = props.available.map(m => m.date.format("YYYYMMDD")).distinct.map(s => Moment(s))
        val weeks = ListBuffer[Scala.Unmounted[WeekProps, Unit, Unit]]()
        while (startOfTheWeek.month() != state.viewed.month() + 1) {
          weeks += Week(WeekProps(Moment(startOfTheWeek), this, Moment(state.viewed), state.selected, props.today, uniqueMeetingDays))
          startOfTheWeek.add(1, "week")
        }
        //        val p = CSSTransitionGroupProps()
        //        p.transitionName = "scheduler"
        //        p.transitionEnterTimeout = 1000
        //        p.transitionLeaveTimeout = 1000
        //        CSSTransitionGroup(
        <.table(<.tbody(MonthTab((Moment(startOfTheMonth), this)), weeks.toVdomArray))
        //       )
      }
    }
    val today: Date = Moment()
    val Calendar = ScalaComponent.build[Props]("Calendar")
      .initialState(State(Moment(today), None))
      .renderBackend[BackendCalendar]
      .build
    Ajax.get("/meetings").onSuccess { case xhr =>
      val moments = read[Seq[ParsedMeeting]](xhr.responseText).map(t => {
        println(t.date)
        MomentMeeting(t.id, Moment(t.date), t.isTaken)
      })

      Calendar(Props(today, moments)).renderIntoDOM(document.getElementById("here"))
      console.log("jej")
    }
  }
}



