package scheduler.controllers

import javax.inject.Inject

import play.api.mvc._
import upickle.default._
/**
  * Created by Szymon Barańczyk on 2017-02-13.
  */
class Application @Inject() extends Controller {

  def index = Action {
    Ok(scheduler.views.html.index("Nothing for now"))
  }

}