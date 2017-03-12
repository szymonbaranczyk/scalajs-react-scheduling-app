package main

import upickle.Js

/**
  * Created by Szymon Barańczyk on 2017-03-12.
  */
trait JsonParser {

  implicit val DateWriter = upickle.default.Writer[java.util.Date] {
    case d => Js.Str(d.getTime.toString)
  }
  implicit val DateReader = upickle.default.Reader[java.util.Date] {
    case Js.Str(str) =>
      new java.util.Date(str.toLong)
  }

}
