/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.concurrent.akka



import scala.concurrent.{Awaitable, ExecutionContext}
//import scala.util.continuations._



trait Future[+T] extends scala.concurrent.Future[T] with Awaitable[T] {
  
  implicit def executor: ExecutionContextImpl
  
  /** For use only within a Future.flow block or another compatible Delimited Continuations reset block.
   *  
   *  Returns the result of this Future without blocking, by suspending execution and storing it as a
   *  continuation until the result is available.
   */
  //def apply(): T @cps[Future[Any]] = shift(this flatMap (_: T => Future[Any]))
  
  /** Tests whether this Future has been completed.
   */
  final def isCompleted: Boolean = value.isDefined
  
  /** The contained value of this Future. Before this Future is completed
   *  the value will be None. After completion the value will be Some(Right(t))
   *  if it contains a valid result, or Some(Left(error)) if it contains
   *  an exception.
   */
  def value: Option[Either[Throwable, T]]
  
  def onComplete[U](func: Either[Throwable, T] => U): this.type
  
  /** Creates a new Future[A] which is completed with this Future's result if
   *  that conforms to A's erased type or a ClassCastException otherwise.
   */
  final def mapTo[T](implicit m: Manifest[T]) = {
    val p = executor.promise[T]
    
    onComplete {
      case l @ Left(t) => p complete l.asInstanceOf[Either[Throwable, T]]
      case Right(v) =>
        p complete (try {
          Right(boxedType(m.erasure).cast(v).asInstanceOf[T])
        } catch {
          case e: ClassCastException ⇒ Left(e)
        })
    }
    
    p.future
  }

  /** Used by for-comprehensions.
   */
  final def withFilter(p: T => Boolean) = new FutureWithFilter[T](this, p)

  final class FutureWithFilter[+A](self: Future[A], p: A => Boolean) {
    def foreach(f: A => Unit): Unit = self filter p foreach f
    def map[B](f: A => B) = self filter p map f
    def flatMap[B](f: A => Future[B]) = self filter p flatMap f
    def withFilter(q: A => Boolean): FutureWithFilter[A] = new FutureWithFilter[A](self, x ⇒ p(x) && q(x))
  }
  
}




