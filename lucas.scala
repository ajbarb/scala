package dos.proj1
 
import akka.actor._
import akka.routing.RoundRobinRouter
import scala.concurrent.duration._
 
object Square extends App {
	
	if (args.length < 2)
	{
		print("Please run with 2 args\n");
		System.exit(1);
	}
	
	val nrOfElements: Integer = args(0).toInt
	val seqLength: Integer = args(1).toInt
	var debug: Integer = 0
        var nrOfWorkers: Integer = 2
	if (args.length > 2)
		nrOfWorkers = args(2).toInt
	if (args.length > 3)
		debug = args(3).toInt
 
  calculate(nrOfWorkers, nrOfElements, nrOfMessages = 10000, seqLength)

  sealed trait PiMessage
  case object Calculate extends PiMessage
  case class Work(start: Int, nrOfElements: Int, seqLength: Int) extends PiMessage
  case class Result(value: List[Int]) extends PiMessage
  case class PiApproximation(pi: Double, duration: Duration)
 
  class Worker extends Actor {

    def checkIfSquare(input: Int): Int = {
      var ans: Int = Math.sqrt(input.toDouble).toInt
      var check: Int = input - ans*ans
      check
    }
 
    def calculatePiFor(start: Int, nrOfElements: Int, seqLength: Int): List[Int] = {
      var num: Array[Int] = new Array[Int](seqLength)
      var sum: Int = 0
      var shift: Int = 0
      var resultList:List[Int] = Nil
      var i,j: Int = 0
      if (debug > 1 )
     		print("start " + start + " end " + nrOfElements + "\n")
      for(j <- 0 until seqLength)
      {
        num(j) = (start + j) * (start + j)
        sum = sum + num(j)        
      }
      for (i <- start until (start + nrOfElements))
      {
        if (checkIfSquare(sum) == 0)
        {
          resultList = resultList ::: List(i) 
        }
        shift = num(0)
        for(j <- 0 until seqLength-1)
        {
          num(j) = num(j+1)
        }
        num(seqLength-1) = (i+seqLength)*(i+seqLength)
        sum = sum - shift + num(seqLength-1) 
      }
      resultList
    }
 
    def receive = {
      case Work(start, nrOfElements, seqLength) =>
        sender ! Result(calculatePiFor(start, nrOfElements, seqLength)) // perform the work
    }
  }
 
  class Master(nrOfWorkers: Int, nrOfMessages: Int, nrOfElements: Int, listener: ActorRef, seqLength: Int)
    extends Actor {
 
    var pi: Double = _
    var nrOfResults: Int = _
    val start: Long = System.currentTimeMillis
    val chunk = nrOfElements/nrOfWorkers;
    val residue = nrOfElements%nrOfWorkers;
    var extra: Int = 1 // Make it 1
    var i, count: Int = 0

    print("residue " + residue + " chunk " + chunk + " workers " + nrOfWorkers  +"\n")
    
 
    val workerRouter = context.actorOf(
      Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")
 
    def receive = {
      case Calculate =>

        for (i <- 0 until nrOfWorkers) 
        {
           if (i<residue) 
           {
              extra = i
              count = 1
           }
	   else
           {
		extra = residue
           	count = 0
	   }
           workerRouter ! Work(i * chunk + extra, chunk + count, seqLength)
        }
      case Result(value) =>
        nrOfResults += 1
        print(value + "\n")

        if (nrOfResults == nrOfWorkers) {
          // Send the result to the listener
  //        listener ! PiApproximation(pi, duration = (System.currentTimeMillis - start).millis)
          // Stops this actor and all its supervised children
		printf("Time to exit")
          context.stop(self)
	context.system.shutdown()
        }
    }
 
  }
 
  class Listener extends Actor {
    def receive = {
      case PiApproximation(pi, duration) =>
        println("\n\tPi approximation: \t\t%s\n\tCalculation time: \t%s"
          .format(pi, duration))
        context.system.shutdown()
    }
  }
 
 
  def calculate(nrOfWorkers: Int, nrOfElements: Int, nrOfMessages: Int, seqLength: Int) {
    // Create an Akka system
    val system = ActorSystem("PiSystem")
 
    // create the result listener, which will print the result and shutdown the system
    val listener = system.actorOf(Props[Listener], name = "listener")
 
    // create the master
    val master = system.actorOf(Props(new Master(
      nrOfWorkers, nrOfMessages, nrOfElements, listener, seqLength)),
      name = "master")
 
    // start the calculation
    master ! Calculate
 
  }
}
