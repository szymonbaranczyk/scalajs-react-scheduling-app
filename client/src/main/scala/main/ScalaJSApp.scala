package main


import org.scalajs.dom._
import org.scalajs.dom.html.{Input, Table}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import moment._
import moment.Duration

import scala.collection.mutable.ListBuffer
/**
  * Created by Szymon Barańczyk on 2017-02-13.
  */

@JSExport
object ScalaJSApp extends js.JSApp {
  case class State(viewed:Date,selected:Option[Date])
  case class Props(today:Date,available:List[Date])
  @scala.scalajs.js.annotation.JSExport
  override def main(): Unit = {

    val Day = ReactComponentB[(Date, BackendCalendar)]("Day")
      .render_P{case (d,b) => <.td(d.day(), ^.onClick ==> b.select(d))}
      .build
    val Month = ReactComponentB[Date]("Month")
      .render_P(d => <.tr( <.th(^.colSpan := 7, d.month())))
      .build
    val Week = ReactComponentB[(Date, BackendCalendar)]("Week")
        .render_P{case (d,b) =>
          val days = (0 to 6).map(_ => {
            val tag = <.td(d.format("D"), ^.onClick ==> b.select(d))
            d.add(Moment.duration(1, "day"))
            tag
          })
          <.tr(days)
        }.build
    class BackendCalendar($: BackendScope[Props, State]) {
      def select(d: Date)(e:ReactEvent): Callback = {
        console.log("clicked")
        $.modState(state => State(state.viewed,state.selected))
      }
      def render(props: Props, state: State) = {
        val startOfTheMonth = Moment(state.viewed).startOf("month")
        val startOfTheWeek = Moment(startOfTheMonth)
        val weeks = ListBuffer[TagMod]()
        while(startOfTheWeek.month() == state.viewed.month()) {
          weeks += Week((Moment(startOfTheWeek), this))
          startOfTheWeek.add(1,"week")
        }
        <.table(<.tbody(Month(Moment(startOfTheMonth)),
          weeks))
      }
    }
    val today: Date = Moment()
    val Calendar = ReactComponentB[Props]("Calendar")
      .initialState(State(today,None))
      .renderBackend[BackendCalendar]
      .build
    ReactDOM.render(Calendar(Props(today,List[Date]())), document.getElementById("here"))
  }

}


