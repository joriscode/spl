package center.scala.spl

object Prompt {
  // for interactive shell
  //final case class State(commands: List[Command], next: State, history: Any) {
  //  def prompt(): Unit = {
  //  }
  //}

  var options: Set[Char] = Set()

  def display(msg: String) = Console.println(msg)

  def newLine() = Console.println

  def info(msg: String) = display(s"[Info] $msg")

  def warn(msg: String) = if (options.contains('r')) {
    display(s"[Warn] $msg")
  } else {
    display("[" + Console.YELLOW + "Warn" + Console.RESET + "] " + msg)
  }

  def error(msg: String) = if (options.contains('r')) {
    display(s"[Error] $msg")
  } else {
    display ("[" + Console.RED + "Error" + Console.RESET + "] " + msg)
  }
}
