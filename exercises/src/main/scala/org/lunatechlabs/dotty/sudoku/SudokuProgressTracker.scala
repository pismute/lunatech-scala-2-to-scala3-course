package org.lunatechlabs.dotty.sudoku

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, Behavior }

object SudokuProgressTracker:

  enum Command:
    case NewUpdatesInFlight(count: Int)
    case SudokuDetailState(index: Int, state: ReductionSet)

  export Command._

  // My responses
  sealed trait Response
  final case class Result(sudoku: Sudoku) extends Response

  def apply(rowDetailProcessors: Map[Int, ActorRef[SudokuDetailProcessor.Command]],
            sudokuSolver: ActorRef[Response]
  ): Behavior[Command] =
    Behaviors.setup { context =>
      new SudokuProgressTracker(rowDetailProcessors, context, sudokuSolver)
        .trackProgress(updatesInFlight = 0)
    }

class SudokuProgressTracker private (
  rowDetailProcessors: Map[Int, ActorRef[SudokuDetailProcessor.Command]],
  context: ActorContext[SudokuProgressTracker.Command],
  sudokuSolver: ActorRef[SudokuProgressTracker.Response]
):

  import SudokuProgressTracker._

  def trackProgress(updatesInFlight: Int): Behavior[Command] =
    Behaviors.receiveMessage {
      case Command.NewUpdatesInFlight(updateCount) if updatesInFlight - 1 == 0 =>
        rowDetailProcessors.foreach ((_, processor) =>
            processor ! SudokuDetailProcessor.GetSudokuDetailState(context.self)
        )
        collectEndState()
      case Command.NewUpdatesInFlight(updateCount) =>
        trackProgress(updatesInFlight + updateCount)
      case msg: Command.SudokuDetailState =>
        context.log.error("Received unexpected message in state 'trackProgress': {}", msg)
        Behaviors.same
    }

  def collectEndState(remainingRows: Int = 9,
                      endState: Vector[Command.SudokuDetailState] = Vector.empty[Command.SudokuDetailState]
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case detail: Command.SudokuDetailState if remainingRows == 1 =>
        sudokuSolver ! Result(
          (detail +: endState).sortBy { case Command.SudokuDetailState(idx, _) => idx }.map {
            case Command.SudokuDetailState(_, state) => state
          }
        )
        trackProgress(updatesInFlight = 0)
      case detail: Command.SudokuDetailState =>
        collectEndState(remainingRows = remainingRows - 1, detail +: endState)
      case msg: Command.NewUpdatesInFlight =>
        context.log.error("Received unexpected message in state 'collectEndState': {}", msg)
        Behaviors.same
    }
