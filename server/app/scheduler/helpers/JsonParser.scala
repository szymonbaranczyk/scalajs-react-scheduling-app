package scheduler.helpers

import play.api.libs.json.{Format, Json}
import shared.domain._
import upickle.Js
/**
  * Created by Szymon Barańczyk on 2017-03-09.
  */
trait JsonParser {
  implicit val format: Format[Meeting] = Json.format

  implicit val DateWriter = upickle.default.Writer[java.util.Date] {
    case d => Js.Str(d.getTime.toString)
  }
}
