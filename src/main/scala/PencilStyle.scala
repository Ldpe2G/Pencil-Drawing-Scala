import org.opencv.core.Core
import Utils._
import java.io.InputStreamReader
import java.io.BufferedReader

object PencilStyle {
  
  def main(args: Array[String]): Unit = {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    if(args.length < 6) {
      println("usage: PencilStyle imagePath resultPath textureImgPath isDrawColor pythonDirPath isShowSteps")
    }
    val img = imgRead(args(0))
    
    val (showStep, outputPath) = if (args(5).toInt == 1) (true, args(1).substring(0, args(1).lastIndexOf("/"))) else (false, "")
    
    val cons = Configs(
        30,                   // larger for a shorter line fragment
        8,                     // smaller for thiner outline sketches
        2f,                    //larger for smoother tonal mappings
        0.2f,
        args(2),
        args(4),
        showStep,
        outputPath
    )
    val result = {
      if (args(3).toInt == 0) pencilDraw(img, cons)
      else colourPencilDraw(img, cons)
    }
    imgWrite(result, args(1))
  }
  
}