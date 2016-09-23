import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.core.CvType
import org.opencv.core.Point
import org.opencv.core.Core
import org.opencv.core.Size
import java.io.PrintWriter
import org.opencv.core.Scalar
import org.opencv.core.Rect
import java.util.ArrayList
import breeze.linalg._
import breeze.math._
import breeze.numerics._
import scala.io.Source
import org.opencv.highgui.Highgui


/**
 * @author Depeng Liang
 */
object Utils {
  
  case class Configs(
    lineLenDivisor: Int = 40,                 // larger for a shorter line fragment
    lineThicknessDivisor: Int = 8,         // smaller for thiner outline sketches
    lambda: Float= 2f,                             //larger for smoother tonal mappings
    textureResizeRatio: Float = 0.2f,
    textureFileName: String = "./images/textures/a.jpg",
    pythonDirPath: String = "./python",
    showStep: Boolean = false,
    outPath: String = ""
  )
  
  def imgRead(imgPath: String): Mat = {
    Highgui.imread(imgPath, 1)
  }
  
  def imgWrite(img: Mat, path: String): Unit = {
    Highgui.imwrite(path, img)
  }
  
  // compute the image gradient
  def getGradient(img: Mat): Mat = {
    img.convertTo(img, CvType.CV_32F)
    val kernel = new Mat(3, 3, CvType.CV_32F)
    val iX = new Mat(img.height(), img.width(), CvType.CV_32F)
    val iY = new Mat(img.height(), img.width(), CvType.CV_32F)
    
    val point = new Point(-1, -1)
    kernel.put(0, 0, Array(
        -1f, 0f, 1f,
        -1f, 0f, 1f,
        -1f, 0f, 1f)
    )
    Imgproc.filter2D(img, iX, -1, kernel, point, 0)
    kernel.put(0, 0, Array(
        1f, 1f, 1f,
        0f, 0f, 0f,
        -1f, -1f, -1f)
    )
    Imgproc.filter2D(img, iY, -1, kernel, point, 0)
    
    var array = new Array[Float]((img.total * img.channels).toInt)

    iX.get(0, 0, array)
    array = array.map(x => x * x)
    iX.put(0, 0, array)
    
    iY.get(0, 0, array)
    array = array.map(x => x * x)
    iY.put(0, 0, array)
    
    val result = new Mat(img.height(), img.width(), CvType.CV_32F)
    Core.add(iX, iY, result)
    Core.sqrt(result, result)
    result
  }
  
  // create the 8 directional line segments
  def createLineSegments(lineLen: Int, lineThicknessDivisor: Int): (List[Mat], List[Mat]) = {
    val halfLineLen = (lineLen + 1) / 2
    val lineSegs = (1 to 8).map( x => Mat.zeros(lineLen, lineLen, CvType.CV_32F) ).toArray
    var y = 0
    val array = new Array[Float](lineLen * lineLen)
     
    val center = new Point(halfLineLen-1, halfLineLen-1)
    val rotateMat90 = Imgproc.getRotationMatrix2D(center, 90, 1.0)
    val dstSize = new Size(lineLen, lineLen)

    for(n <- 0 to 7) {
      if (n == 0 || n == 1 || n == 2 || n ==7) {
        lineSegs(n).get(0, 0, array)
        for (x <- 1 to lineLen) {
          y = halfLineLen - Math.round((x - halfLineLen) * Math.tan(Math.PI / 8 * n)).toInt
          if (y >0 && y <= lineLen) {
            array((y - 1) * lineLen + (x - 1)) = 1
          }
        }
        lineSegs(n).put(0, 0, array)
        if (n == 0 || n == 1 || n == 2) {
          Imgproc.warpAffine(lineSegs(n), lineSegs(n+4), rotateMat90, dstSize)
        } 
      }
    }
    Imgproc.warpAffine(lineSegs(7), lineSegs(3), rotateMat90, dstSize)
    
    // add some thickness to lineSegs
    val width = Math.round(lineLen / lineThicknessDivisor)
    val kernel = Mat.ones(width, width, CvType.CV_32F)
    val point = new Point(-1, -1)
    val thickLineSegs = (1 to 8).map( x => Mat.zeros(lineLen, lineLen, CvType.CV_32F) ).toArray
    for (n <- 0 until lineSegs.length) {
      Imgproc.filter2D(lineSegs(n), thickLineSegs(n), -1, kernel, point, 0)
      thickLineSegs(n).get(0, 0, array)
      val max = (array(0) /: array){ (acc, elem) => if (acc < elem) elem else acc }
      thickLineSegs(n).put(0, 0, array.map(_ / max))
    }
    
    (lineSegs.toList, thickLineSegs.toList)
  }
  
  // create the sketch
  def createSketch(imgGradient: Mat, lineSegs: List[Mat], thickLineSegs: List[Mat]): Mat = {
    val totals = imgGradient.height() * imgGradient.width()
    val G = (1 to lineSegs.length).map(x => Mat.zeros(imgGradient.height(), imgGradient.width(), CvType.CV_32F))
    val point = new Point(-1, -1)
    for (n <- 0 until lineSegs.length) {
      Imgproc.filter2D(imgGradient, G(n), -1, lineSegs(n), point, 0)
    }
    val gIndex = {
      val arrays = G.map { mat => 
        val array = new Array[Float](totals)
        mat.get(0, 0, array)
        array
      }
      val result = new Array[Int](totals)
      var max = 0f
      var idx = 0
      for (i <- 0 until totals) {
        max = arrays(0)(i)
        idx = 0
        for (n <- 1 until lineSegs.length) {
          if (arrays(n)(i) > max) {
            max = arrays(n)(i)
            idx = n
          }
        }
        result(i) = idx
      }
      result
    }
    
    val C = (1 to lineSegs.length).map(x => Mat.zeros(imgGradient.height(), imgGradient.width(), CvType.CV_32F))
    val gradArray = new Array[Float](totals)
    imgGradient.get(0, 0, gradArray)
    for (n <- 0 until lineSegs.length) {
      val tmpA = gradArray.zip(gIndex).map( x=> if(x._2 == n) x._1 else 0)
      C(n).put(0, 0, tmpA)      
    }
    
    val sPn = (1 to lineSegs.length).map(x => Mat.zeros(imgGradient.height(), imgGradient.width(), CvType.CV_32F))
    for (n <- 0 until lineSegs.length) {
       Imgproc.filter2D(C(n), sPn(n), -1, thickLineSegs(n), point, 0)  
    }
    
    val sP = (Mat.zeros(imgGradient.height(), imgGradient.width(), CvType.CV_32F) /: sPn){ (sum, elem) =>
      Core.add(sum, elem, sum)
      sum
    }

    sP.get(0, 0, gradArray)
    val newGA = gradArray.map( 255 - _)
    val (min, max) = ((newGA(0), newGA(0)) /: newGA){ (acc, elem) =>
      val mi = if (elem < acc._1) elem else acc._1
      val ma = if (elem > acc._2) elem else acc._2
      (mi, ma)
    }
    val resultA = newGA.map( x => (x-min) / (max-min) )
    sP.put(0, 0, resultA)
    sP
  }
  
  // First setp, compute the outline sketch
  def computeSketch(img: Mat, cons: Configs): Mat = {
     // calculate 'lineLen', the length of the line segments
    val lineLen = {
      val tmp = (Math.min(img.height(), img.width()) / cons.lineLenDivisor).toInt
      if (tmp % 2 == 1) tmp
      else tmp + 1
    }
    val imgG = getGradient(img)
    if (cons.showStep) {
      imgWrite(imgG, s"${cons.outPath}/gradient.jpg")
    }
    val (lineSegs, thickLineSegs) = createLineSegments(lineLen, cons.lineThicknessDivisor)
    createSketch(imgG, lineSegs, thickLineSegs)
  }
  
  // Histogram match the img against a theoretically natural histogram
  // which is believed to mimic real pencil drawings
  // img must be a grayscale image within 0 and 255
  def naturalHistogramMatching(img: Mat): Mat = {
    // Prepare the histogram of img, ho
    val ho = (1 to 256).map(x => 0f).toArray
    val totals = img.height() * img.width()
    val array = new Array[Float](totals)
    img.convertTo(img, CvType.CV_32F)
    img.get(0, 0, array)
    val po = {
      val tmp = (0 to 255).map( p => array.filter( _ == p).size )
      val sum = tmp.sum
      tmp.map(_.toFloat / sum)
    }
    ho(0) = po(0)
    for (i <- 1 to 255) {
      ho(i) = ho(i - 1) + po(i)
    }
    
    // Prepare the 'natural' histogram which is 'histo'
    val prob = {
      val tmp =  (0 to 255).map(p)
      val sum = tmp.sum
      tmp.map(_ / sum).map(_.toFloat)
    }
    val histo = new Array[Float](256)
    histo(0) = prob(0)
    for(i <- 1 to 255) {
      histo(i) = histo(i - 1) + prob(i)
    }
    
    // Do the histogram matching
    val iAdjusted = Mat.zeros(img.height(), img.width(), CvType.CV_32F)
    var histogramValue = 0f
    for(i <- 0 until totals) {
      histogramValue = ho(array(i).toInt)
      array(i) = ((Math.abs(histo(0) - histogramValue) , 0) /: histo.map(x => Math.abs(x - histogramValue)).zipWithIndex) { (min, elem) =>
        if(elem._1 < min._1) elem else min  
      }._2
    }
    iAdjusted.put(0, 0, array.map(_ / 255))
    iAdjusted
  }
  
  def p(x: Int) = 76 * p1(x) + 22 * p2(x) + 2 * p3(x)
  
  def p1(x: Int) = {
    (1.toDouble / 9) * Math.exp(-(255 - x).toDouble / 9) * heaviside(255 - x)
  }
  
  def p2(x: Int) = {
    (1.toDouble / (255 - 105)) * (heaviside(x - 105) - heaviside(x - 255))
  }
  
  def p3(x: Int) = {
    (1.toDouble / Math.sqrt(Math.PI * 22)) * Math.exp(-((x - 90) * (x - 90)).toDouble / (2 * 121))
  }
  
  def heaviside(x: Int) = if (x >=0) 1 else 0
  
  // img must be within 0 and 1
  def verticalStitch(img: Mat, height: Int): Mat = {
    var iStitched = img
    val windowSize = Math.round(img.height().toFloat / 4)
    val up = new Mat(img, new Rect(0, img.height() - windowSize, img.width(), windowSize))
    val down = new Mat(img, new Rect(0, 0, img.width(), windowSize))
    val aUp = Mat.zeros(windowSize, up.width(), CvType.CV_32F)
    val aDown = Mat.zeros(windowSize, up.width(), CvType.CV_32F)
    val dividWindowSize = (1 to windowSize).map(_.toFloat / windowSize)
    var aUpArray = new Array[Float](up.width() * windowSize)
    var aDownArray = new Array[Float](up.width() * windowSize)
    up.get(0, 0, aUpArray)
    down.get(0, 0, aDownArray)
    aUpArray = aUpArray.grouped(up.width()).zipWithIndex.toArray.flatMap { row =>  
      row._1.map( x => x * (1 - dividWindowSize(row._2)))
    }
    aDownArray = aDownArray.grouped(up.width()).zipWithIndex.toArray.flatMap { row =>  
      row._1.map( x => x * dividWindowSize(row._2))
    }
    aUp.put(0, 0, aUpArray)
    aDown.put(0, 0, aDownArray)
    val upAddDown = new Mat
    Core.add(aUp, aDown, upAddDown)
    var list = new ArrayList[Mat]()
    while (iStitched.height() < height) {
      list.add(new Mat(iStitched, new Rect(0, 0, iStitched.width(), iStitched.height() - windowSize)))
      list.add(upAddDown)
      list.add( new Mat(iStitched, new Rect(0, windowSize, iStitched.width(), iStitched.height() - windowSize)))
      Core.vconcat(list, iStitched)
      list = new ArrayList()
    } 
    new Mat(iStitched, new Rect(0, 0, iStitched.width(), height))
  }
  
  // Continuously repeat the image I horizontally until the width of the
  // resulting image is 'width'.
  // We use alpha blending to smooth the borders in the replication 
  // I must be within 0 and 1
  def horizontalStitch(img: Mat, width: Int): Mat = {
    var iStitched = img
    val windowSize = Math.round(img.width().toFloat / 4)
    val left = new Mat(img, new Rect((img.width() - windowSize), 0, windowSize, img.height()))
    val right = new Mat(img, new Rect(0, 0, windowSize, img.height()))
    val aLeft = Mat.zeros(left.height(), windowSize, CvType.CV_32F)
    val aRight = Mat.zeros(left.height(), windowSize, CvType.CV_32F)
    
    val dividWindowSize = (1 to windowSize).map(_.toFloat / windowSize)
    var aLeftArray = new Array[Float](left.height() * windowSize)
    var aRightArray = new Array[Float](left.height() * windowSize)
    left.get(0, 0, aLeftArray)
    right.get(0, 0, aRightArray)
    aLeftArray = aLeftArray.grouped(windowSize).toArray.flatMap { row =>  
      row.zip(dividWindowSize).map( x => x._1 * (1 - x._2))
    }
    aRightArray = aRightArray.grouped(windowSize).toArray.flatMap { row =>  
      row.zip(dividWindowSize).map( x => x._1 * x._2)
    }
    aLeft.put(0, 0, aLeftArray)
    aRight.put(0, 0, aRightArray)
    val leftAddRight = new Mat
    Core.add(aLeft, aRight, leftAddRight)
    var list = new ArrayList[Mat]()
    while (iStitched.width() < width) {
      list.add(new Mat(iStitched, new Rect(0, 0, iStitched.width() - windowSize, iStitched.height())))
      list.add(leftAddRight)
      list.add( new Mat(iStitched, new Rect(windowSize, 0, iStitched.width() - windowSize, iStitched.height())))
      Core.hconcat(list, iStitched)
      list = new ArrayList()
    }
    new Mat(iStitched, new Rect(0, 0, width, iStitched.height()))
  }  

  // stich pencil texture image
  def stichTexturetIMage(textureFileName: String, textureResizeRatio: Float,
      imgHeight: Int, imgWidth: Int): Mat = {
    val texture = {
      val tmpImg = new Mat
      Imgproc.cvtColor(imgRead(textureFileName), tmpImg, Imgproc.COLOR_RGB2GRAY)
      val croppedTmp = new Mat(tmpImg,  new Rect(100, 100, tmpImg.width() - 200, tmpImg.height() - 200))
      val resizedTmp = new Mat
      val scale = textureResizeRatio * Math.min(imgWidth, imgHeight).toFloat / 1024
      val newWidth = croppedTmp.width() * scale
      val newHeight = croppedTmp.height() * scale
      Imgproc.resize(croppedTmp, resizedTmp, new Size(newWidth, newHeight), 0.5, 0.5, Imgproc.INTER_LINEAR)
      Imgproc.GaussianBlur(resizedTmp, resizedTmp, new Size(3, 3), 0)
      resizedTmp.convertTo(resizedTmp, CvType.CV_32F, 1.0/255.0)
      resizedTmp
    }
    verticalStitch(horizontalStitch(texture, imgWidth), imgHeight)
  }
  
  // solve for beta
  def solveBeta(jTexture: Mat, toneAdjImg: Mat, imgHeight: Int, imgWidth: Int, lambda: Float, cons: Configs): Array[Float] = {
    // width of big matrix
    val sizz = imgHeight * imgWidth
    var nzmax = 2 * (sizz - 1)
    var i = (1 to nzmax).map( x => Math.ceil((x.toDouble + 0.1) / 2).toInt - 1)
    var j = (1 to nzmax).map( x => Math.ceil((x.toDouble - 0.1) / 2).toInt - 1)
    var s = (1 to nzmax).map( x => -2f * ( x % 2) + 1)

    val dx = {
      val builder = new CSCMatrix.Builder[Float](sizz, sizz)
      for (k <- 0 until nzmax) {
        builder.add(i(k), j(k), s(k))
      }
      builder.result
    }
    nzmax = 2 * (sizz - imgWidth)
    i = (1 to nzmax).map { x =>
      if (x % 2 == 1) Math.ceil((x.toDouble + 0.1) / 2).toInt - 1 
      else Math.ceil((x.toDouble -1 + 0.1) / 2).toInt + imgWidth - 1
    }
    j = (1 to nzmax).map( x => Math.ceil((x.toDouble - 0.1) / 2).toInt - 1)
    s = (1 to nzmax).map( x => -2f * ( x % 2) + 1)
    val dy = {
      val builder = new CSCMatrix.Builder[Float](sizz, sizz)
      for (k <- 0 until nzmax) {
        builder.add(i(k), j(k), s(k))
      }
      builder.result
    }

    val array = new Array[Float](sizz)
    jTexture.get(0, 0, array)
    val jSparse = {
      val builder = new CSCMatrix.Builder[Float](sizz, sizz)
      val logArray = array.map(Math.log(_).toFloat)
      for (j <- 0 until sizz) {
        builder.add(j, j, logArray(j))
      }
      builder.result
    }

    val jToneAdjImg1d = {  
      val builderToneAdj = new CSCMatrix.Builder[Float](sizz, 1)
      toneAdjImg.get(0, 0, array)
      val logArray = array.map(Math.log(_).toFloat)
      for (j <- 0 until sizz) {
        builderToneAdj.add(j, 0, logArray(j)) 
      }
      builderToneAdj.result
    }
    val beta1D = {
      val left = (jSparse * jSparse + ((dx.t * dx + dy.t * dy) * lambda.toFloat))
      val right = (jSparse * jToneAdjImg1d)
      solve(left, right, cons)
    }
    beta1D
  }
    
  def solve(A: CSCMatrix[Float], b: CSCMatrix[Float], cons: Configs): Array[Float] = {

    val iterA = A.activeIterator
    val writerA = new PrintWriter(s"${cons.pythonDirPath}/AA.txt")
    writerA.write(s"${A.rows} ${A.cols}\n")
    while (iterA.hasNext) {
      val tmp = iterA.next()
      writerA.write(s"${tmp._1._1} ${tmp._1._2} ${tmp._2}\n")
    }
    writerA.flush()
    writerA.close()
    
    val iterB = b.activeIterator
    val writerB = new PrintWriter(s"${cons.pythonDirPath}/bb.txt")
    writerB.write(s"${b.rows} ${b.cols}\n")
    while (iterB.hasNext) {
      val tmp = iterB.next()
      writerB.write(s"${tmp._1._1} ${tmp._1._2} ${tmp._2}\n")
    }
    writerB.flush()
    writerB.close()
    println("solving ....")
    val p = new ProcessBuilder(Array("python", s"${cons.pythonDirPath}/solve.py"): _*)
                    .redirectErrorStream(true)
                    .start()
    p.waitFor()
    val x = Source.fromFile(s"${cons.pythonDirPath}/xx.txt").mkString.split("\n").map(_.toFloat)
    println("done .")
    x
  }
  
  // Second step, compute the texture tone drawing
  def computeToneDrawing(img: Mat, cons: Configs): Mat = {
    val toneAdjImg = naturalHistogramMatching(img)
    if (cons.showStep) {
      val tmp = new Mat
      toneAdjImg.copyTo(tmp)
      val arrays = new Array[Float](toneAdjImg.width() * toneAdjImg.height())
      tmp.get(0, 0, arrays)
      tmp.put(0, 0, arrays.map(x => x * 255))
      imgWrite(tmp, s"${cons.outPath}/toneMap.jpg")
    }
    val jTexture = stichTexturetIMage(cons.textureFileName, cons.textureResizeRatio, img.height(), img.width())
 
    val beta = solveBeta(jTexture, toneAdjImg, img.height(), img.width(), cons.lambda, cons)
    
    val T = jTexture
    var array = new Array[Float](T.height() * T.width())
    T.get(0, 0, array)
    array = array.zip(beta).map( x => Math.pow(x._1, x._2).toFloat)
    
    val (min, max) = ((array(0), array(0)) /: array){ (acc, elem) =>
      val mi = if (elem < acc._1) elem else acc._1
      val ma = if (elem > acc._2) elem else acc._2
      (mi, ma)
    }
    val normArray = array.map( x =>  (x-min) / (max-min) )
    T.put(0, 0, normArray)
    T
  }
  
  // convert img to gray if it is colour image
  // else return the origin image
  def getJAndHistoType(img: Mat): Mat = {
      val tmp = new Mat()
      if (img.channels() == 3) {
        Imgproc.cvtColor(img, tmp, Imgproc.COLOR_RGB2GRAY)
      } else {
        img.copyTo(tmp)
      }
      tmp
  }
  
  def pencilDraw(img: Mat, cons: Configs): Mat = {
    val imgJ = getJAndHistoType(img)
    val sketchImg = computeSketch(imgJ, cons)
    if (cons.showStep) {
      val tmp = new Mat
      sketchImg.copyTo(tmp)
      val tmpArray = new Array[Float](tmp.width() * tmp.height())
      tmp.get(0, 0, tmpArray)
      tmp.put(0, 0, tmpArray.map(x => x * 255))
      imgWrite(tmp, s"${cons.outPath}/lineDrawing.jpg")
    }
    val toneImg = computeToneDrawing(imgJ, cons)
    val result = sketchImg.mul(toneImg)
    val array = new Array[Float](result.width() * result.height())
    result.get(0, 0, array)
    result.put(0, 0, array.map(x => x * 255))
    result
  }
  
  def rgbToYcbcr(img: Mat): Mat = {
    img.convertTo(img, CvType.CV_32F)
    val (y, cB, cR) = {
      val layers = new ArrayList[Mat]()
      Core.split(img, layers)
      val totals = img.width() * img.height()
      val rA = new Array[Float](totals)
      val gA = new Array[Float](totals)
      val bA = new Array[Float](totals)
      layers.get(2).get(0, 0, rA)
      layers.get(1).get(0, 0, gA)
      layers.get(0).get(0, 0, bA)
      
      val yA = new Array[Float](totals)
      val cbA = new Array[Float](totals)
      val crA = new Array[Float](totals)
      
      for (i <- 0 until totals) {
        yA(i) = 0.257f * rA(i) + 0.504f * gA(i) + 0.098f * bA(i) + 16
        cbA(i) = -0.148f * rA(i) - 0.291f * gA(i) + 0.439f * bA(i) + 128
        crA(i) = 0.439f * rA(i) - 0.368f * gA(i) - 0.071f * bA(i) + 128
      }
      
      val yy = new Mat(img.height(), img.width(), CvType.CV_32F)
      yy.put(0, 0, yA)
      val cbb = new Mat(img.height(), img.width(), CvType.CV_32F)
      cbb.put(0, 0, cbA)
      val crr = new Mat(img.height(), img.width(), CvType.CV_32F)
      crr.put(0, 0, crA)
      (yy, cbb, crr)
    }
    val result = new Mat()
    val layers = new ArrayList[Mat]()
    layers.add(cR)
    layers.add(cB)
    layers.add(y)
    Core.merge(layers, result)
    result
  }

  def ycbcrToRgb(img: Mat): Mat = {
    img.convertTo(img, CvType.CV_32F)
    val (r, g, b) = {
      val layers = new ArrayList[Mat]()
      Core.split(img, layers)
      val totals = img.width() * img.height()
      val yA = new Array[Float](totals)
      val cbA = new Array[Float](totals)
      val crA = new Array[Float](totals)
      layers.get(2).get(0, 0, yA)
      layers.get(1).get(0, 0, cbA)
      layers.get(0).get(0, 0, crA)
      
      val rA = new Array[Float](totals)
      val gA = new Array[Float](totals)
      val bA = new Array[Float](totals)
      
      for (i <- 0 until totals) {
        rA(i) = 1.164f * (yA(i) - 16) + 1.596f * (crA(i) - 128)
        gA(i) = 1.164f * (yA(i) - 16) - 0.813f * (crA(i) - 128) - 0.392f * (cbA(i) - 128)
        bA(i) = 1.164f * (yA(i) - 16) + 2.017f * (cbA(i) - 128)
      }
      
      val rr = new Mat(img.height(), img.width(), CvType.CV_32F)
      rr.put(0, 0, rA)
      val gg = new Mat(img.height(), img.width(), CvType.CV_32F)
      gg.put(0, 0, gA)
      val bb = new Mat(img.height(), img.width(), CvType.CV_32F)
      bb.put(0, 0, bA)
      (rr, gg, bb)
    }
    val result = new Mat()
    val layers = new ArrayList[Mat]()
    layers.add(b)
    layers.add(g)
    layers.add(r)
    Core.merge(layers, result)
    result
  }
  
  def colourPencilDraw(img: Mat, cons: Configs): Mat = {
    if (img.channels() < 3) {
      println("please input rgb image!")
      null
    } else {
      val yImg = rgbToYcbcr(img)
      val layers = new ArrayList[Mat]()
      Core.split(yImg, layers)
      layers.get(2).convertTo(layers.get(2), CvType.CV_8U)
      val newLayers = new ArrayList[Mat]()
      newLayers.add(layers.get(0))
      newLayers.add(layers.get(1))
      newLayers.add(pencilDraw(layers.get(2), cons))
      Core.merge(newLayers, yImg)
      ycbcrToRgb(yImg)
    }
  }
  
  
  
}
