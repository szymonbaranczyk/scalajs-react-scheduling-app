package main

import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import moment.{Moment, _}
import org.scalajs.dom._
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html.Paragraph
import shared.domain.{BookedMeeting, Meeting}
import upickle.default._

import scala.collection.Seq
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

/**
  * Created by Szymon Barańczyk on 2017-02-13.
  */
case class MomentMeeting(id: Int, date: Date, isTaken: Boolean)

case class DayMeeting(date: Date, isTaken: Boolean)

case class ParsedMeeting(id: Int, date: Long, isTaken: Boolean)

@JSExport
object ScalaJSApp extends js.JSApp with JsonParser {

  @scala.scalajs.js.annotation.JSExport
  override def main(): Unit = {
    case class SchedulerProps(today: Date)
    case class SchedulerState(uniqueMeetingMap: Map[String, Seq[MomentMeeting]], selectedDate: Option[Date], selectedTime: Option[MomentMeeting], transition: Boolean)
    case class CalendarState(viewed: Date)
    case class TimeContainerProps(meeting: MomentMeeting, backend: BackendScheduler, transitionLeaving: Boolean)
    case class CalendarProps(today: Date, availableDays: Seq[DayMeeting], selectedDate: Option[Date], backendScheduler: BackendScheduler)
    case class DayProps(day: Date, backendScheduler: BackendScheduler, isToday: Boolean, isSelected: Boolean,
                        isThisMonth: Boolean, isMeeting: Boolean, isInPast: Boolean, isFree: Boolean)
    case class WeekProps(date: Date, backendScheduler: BackendScheduler, viewed: Date, selected: Option[Date], today: Date, available: Seq[DayMeeting])
    val Day = ScalaComponent.build[DayProps]("Day")
      .render_P(p => {
        val onClick = if (p.isThisMonth && !p.isInPast && p.isMeeting && p.isFree) p.backendScheduler.selectDate(p.day)(_) else (e: ReactEvent) => Callback {}
        val `class` = "day" + (if (!p.isThisMonth || p.isInPast) " inactive" else "") +
          (if (p.isSelected) " selected-date" else "") +
          (if (p.isMeeting) " meeting" else "") +
          (if (!p.isFree && p.isMeeting) " occupied" else "") +
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
        <.th(^.colSpan := 7, ^.`class` := "month-tab",
          <.span("<", ^.onClick ==> b.prev, ^.`class` := "left-arrow"),
          <.span(d.format("MMMM"), ^.`class` := "month-name"),
          <.span(">", ^.onClick ==> b.next, ^.`class` := "right-arrow")
        ))
      }
      .build
    val Week = ScalaComponent.build[WeekProps]("Week")
      .render_P(p => {
        val dateIter = Moment(p.date)
        val days = (0 to 6).map(_ => {
          val matchedMeeting: Option[DayMeeting] = p.available.find(m => m.date.isSame(dateIter, "day"))
          val tag = Day(DayProps(Moment(dateIter), p.backendScheduler,
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
            isInPast = p.today.isAfter(dateIter, "day"),
            isFree = matchedMeeting match {
              case Some(DayMeeting(_, false)) => true
              case _ => false
            }))
          dateIter.add(1, "day")
          tag
        })
        <.tr(days.toVdomArray)
      }).build
    class BackendCalendar($: BackendScope[CalendarProps, CalendarState]) {


      def prev(e: ReactEvent): Callback = {
        console.log("prev")
        $.modState(state => CalendarState(state.viewed.add(-1, "month")))
      }

      def next(e: ReactEvent): Callback = {
        console.log("next")
        $.modState(state => CalendarState(state.viewed.add(1, "month")))
      }

      def render(props: CalendarProps, state: CalendarState) = {
        val startOfTheMonth = Moment(state.viewed).startOf("month")
        val startOfTheWeek = Moment(startOfTheMonth).startOf("week")
        val weeks = ListBuffer[Scala.Unmounted[WeekProps, Unit, Unit]]()
        while (startOfTheWeek.month() != state.viewed.month() + 1) {
          weeks += Week(WeekProps(Moment(startOfTheWeek), props.backendScheduler, Moment(state.viewed), props.selectedDate, props.today, props.availableDays))
          startOfTheWeek.add(1, "week")
        }

        <.table(<.tbody(MonthTab((Moment(startOfTheMonth), this)), weeks.toVdomArray), ^.key := "Calendar")

      }
    }
    val today: Date = Moment()
    val Calendar = ScalaComponent.build[CalendarProps]("Calendar")
      .initialState(CalendarState(Moment(today)))
      .renderBackend[BackendCalendar]
      .build
    //    val Time = ScalaComponent.build[(MomentMeeting,BackendScheduler)]("Time")
    //        .render_P(
    //          p => {
    //            val callback = p._2.selectTime(p._1)(_)
    //            <.div(p._1.date.format("HH:mm"), ^.onClick ==> callback)
    //          }
    //        ).build
    val TimeContainer = ScalaComponent.build[(Option[Seq[MomentMeeting]], BackendScheduler)]("TimeContainer")
      .render_P(
        p => {
          val list = p._1 match {
            case Some(seq) =>
              seq.map(m => {
                val callback = p._2.selectTime(m)(_)
                <.div(m.date.format("HH:mm"), ^.onClick ==> callback, ^.`class` := "time")
              })
            case None => Seq[TagOf[Paragraph]]()
          }
          <.div(list.toVdomArray, ^.`class` := "time-container", ^.key := "Time")
        }
      ).build
    class BackendForm($: BackendScope[(MomentMeeting, BackendScheduler), String]) {
      def handleChange(e: ReactEventFromInput) =
        $.setState(e.target.value)

      def render(props: (MomentMeeting, BackendScheduler), state: String) = {
        <.div(
          <.i(^.`class` := "fa fa-arrow-left", ^.onClick ==> (props._2.clearTimeSelection _)),
          <.span(props._1.date.format("HH:mm")),
          <.div(^.`class` := "form-group",
            <.label(^.`for` := "usr", "Name:"),
            <.input(^.`type` := "text", ^.`class` := "form-control", ^.id := "usr", ^.onChange ==> handleChange),
            <.button("Submit", ^.`class` := "", ^.onClick ==> (props._2.submit(state)(_)))

          )
        )
      }
    }
    val Form = ScalaComponent.build[(MomentMeeting, BackendScheduler)]("Form")
      .initialState("")
      .renderBackend[BackendForm]
      .build
    class BackendScheduler($: BackendScope[SchedulerProps, SchedulerState]) {
      def selectDate(d: Date)(e: ReactEvent): Callback = {
        console.log("clicked " + d.format("DD-MM-YYYY"))
        $.modState(state => SchedulerState(state.uniqueMeetingMap, Some(d), None, false))
      }

      def selectTime(m: MomentMeeting)(e: ReactEvent): Callback = {
        console.log("clicked " + m.date.format("DD-MM-YYYY"))
        //temporary hack
        $.modState(state => SchedulerState(state.uniqueMeetingMap, state.selectedDate, None, transition = true)).runNow()
        $.modState(state => SchedulerState(state.uniqueMeetingMap, state.selectedDate, Some(m), transition = false)).delayMs(700).runNow()
        Callback {}
      }

      def clearTimeSelection(e: ReactEvent): Callback = {
        $.modState(state => SchedulerState(state.uniqueMeetingMap, state.selectedDate, state.selectedTime, transition = true)).runNow()
        $.modState(state => SchedulerState(state.uniqueMeetingMap, state.selectedDate, None, transition = false)).delayMs(700).runNow()
        Callback {}
      }

      def submit(data: String)(e: ReactEvent): Callback = {
        console.log("submitted " + data)

        $.state.map(state => {
          val time = state.selectedTime.get
          console.log(time.date.unix().toLong)
          val meeting = BookedMeeting(data, Meeting(time.id, new java.util.Date(time.date.format("x").toLong), true))
          Ajax.post("/meetings/book", write(meeting))
        }).runNow()
        $.modState(state => SchedulerState(state.uniqueMeetingMap, state.selectedDate, state.selectedTime, transition = true)).runNow()
        $.modState(state => SchedulerState(state.uniqueMeetingMap, None, None, transition = false)).delayMs(700).runNow()

        Callback {}
      }

      def render(props: SchedulerProps, state: SchedulerState) = {
        val uniqueMeetingDays = state.uniqueMeetingMap.map { case (k, seq) => DayMeeting(Moment(k), seq.forall(meet => meet.isTaken)) }.toSeq
        val calendar: TagMod = <.div(Calendar(CalendarProps(props.today, uniqueMeetingDays, state.selectedDate, this)), ^.`class` := "col-sm-6 col-xs-12")
        val `class` = " col-sm-6 col-xs-12 second-container " + (if (state.transition) "second-container-transition" else "")
        val secondContainer: TagMod = <.div(state.selectedTime match {
          case None =>
            TimeContainer((
              state.selectedDate match {
                case Some(date) =>
                  state.uniqueMeetingMap.get(date.format("YYYYMMDD")) match {
                    case Some(meetings) => Some(meetings)
                    case None => Some(Seq[MomentMeeting]())
                  }
                case None => None
              },
              this)
            )
          case Some(meeting) => Form(meeting, this)
        }, ^.`class` := `class`, ^.key := "SecondContainer")
        //TransitionGroup won't work, as one item has to fade out BEFORE second one will fade in.
        //        val p = CSSTransitionGroupProps()
        //        p.transitionName = "scheduler"
        //        p.transitionEnterTimeout = 1000
        //        p.transitionLeaveTimeout = 1000
        <.div(
          //          CSSTransitionGroup(p)(
          calendar,
          secondContainer,
          ^.`class` := "row")
      }


    }


    Ajax.get("/meetings").onSuccess { case xhr =>
      val meetings = read[Seq[Meeting]](xhr.responseText).map(t => {
        println(t.date)
        MomentMeeting(t.id, Moment(t.date.getTime), t.isTaken)
      })
      val uniqueMeetingMap: Map[String, Seq[MomentMeeting]] = meetings.groupBy(m => m.date.format("YYYYMMDD"))
      val Scheduler = ScalaComponent.build[SchedulerProps]("Scheduler")
        .initialState(SchedulerState(uniqueMeetingMap, None, None, true))
        .renderBackend[BackendScheduler]
        .build
      Scheduler(SchedulerProps(today)).renderIntoDOM(document.getElementById("here"))
      console.log("jej")
    }
  }
}



