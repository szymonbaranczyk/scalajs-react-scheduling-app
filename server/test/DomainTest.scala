import org.scalactic.TolerantNumerics
import org.scalatest.FlatSpec
import shared.{Player, Position}

/**
  * Created by Szymon Barańczyk on 2017-02-15.
  */
class DomainTest extends FlatSpec {
  implicit val doubleEquality = TolerantNumerics.tolerantDoubleEquality(0.01)

  val p = Player(
    Position(10, 4, 45),
    6.5,
    0,
    "lol"
  )
  val p2 = p.move()
  val p3: Player = p2.changeSpeed(2, 10)
  val p4 = p3.move()

  "Player" should "move correctly" in {
    val p2 = p.move()
    assert(p2.position.x === 14.59)
    assert(p2.position.y === 8.59)
    assert(p2.position.direction === 45.0)
    assert(p2.velocity === 6.5)
  }

  "Player" should "change speed correctly" in {

    assert(p3.position.direction === 55.0)
    assert(p3.velocity === 8.5)
  }

  "Player" should "move correctly after change of speed" in {
    assert(p4.position.x === 19.47)
    assert(p4.position.y === 15.55)
    assert(p4.position.direction === 55.0)
    assert(p4.velocity === 8.5)
  }


}

