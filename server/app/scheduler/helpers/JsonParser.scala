package scheduler.helpers

import play.api.libs.json.{Format, Json}
import shared.domain._

/**
  * Created by Szymon Barańczyk on 2017-03-09.
  */
trait JsonParser {
  implicit val format: Format[Meeting] = Json.format
}
