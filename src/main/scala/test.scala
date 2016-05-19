import org.opencv.core.Core

object test {
  
  def run() =  {
    
  }
    
  def main(args: Array[String]): Unit = {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    println("Hello, OpenCV")
    // Load the native library.
  }
  
}