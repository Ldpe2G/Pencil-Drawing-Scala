ROOT=$(cd "$(dirname $0)"; pwd)

# better to copy your opencv-300.jar to replace this one
OPENCV_JAR_FILE=$ROOT/lib/opencv-300.jar

# needs to set to your opencv-3.0 build lib path 
LIBRARY_PATH=$HOME/opencv3.0/opencv-3.0.0/build/lib/

CLASS_PATH=$OPENCV_JAR_FILE:$ROOT/target/scala-2.11/classes/:$HOME/.ivy2/cache/org.scala-lang/scala-library/jars/scala-library-2.11.7.jar:\
$HOME/.ivy2/cache/org.scalanlp/breeze_2.11/jars/breeze_2.11-0.12.jar:\
$HOME/.ivy2/cache/org.scalanlp/breeze-macros_2.11/jars/breeze-macros_2.11-0.12.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/core/jars/core-1.1.2.jar:\
$HOME/.ivy2/cache/net.sourceforge.f2j/arpack_combined_all/jars/arpack_combined_all-0.1.jar:\
$HOME/.ivy2/cache/net.sf.opencsv/opencsv/jars/opencsv-2.3.jar:\
$HOME/.ivy2/cache/com.github.rwl/jtransforms/jars/jtransforms-2.4.0.jar:\
$HOME/.ivy2/cache/junit/junit/jars/junit-4.8.2.jar:\
$HOME/.ivy2/cache/org.apache.commons/commons-math3/jars/commons-math3-3.2.jar:\
$HOME/.ivy2/cache/org.spire-math/spire_2.11/jars/spire_2.11-0.7.4.jar:\
$HOME/.ivy2/cache/org.spire-math/spire-macros_2.11/jars/spire-macros_2.11-0.7.4.jar:\
$HOME/.ivy2/cache/org.slf4j/slf4j-api/jars/slf4j-api-1.7.5.jar:\
$HOME/.ivy2/cache/com.chuusai/shapeless_2.11/bundles/shapeless_2.11-2.0.0.jar:\
$HOME/.ivy2/cache/org.scalanlp/breeze-natives_2.11/jars/breeze-natives_2.11-0.12.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/netlib-native_ref-osx-x86_64/jars/netlib-native_ref-osx-x86_64-1.1-natives.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/native_ref-java/jars/native_ref-java-1.1.jar:\
$HOME/.ivy2/cache/com.github.fommil/jniloader/jars/jniloader-1.1.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/netlib-native_ref-linux-x86_64/jars/netlib-native_ref-linux-x86_64-1.1-natives.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/netlib-native_ref-linux-i686/jars/netlib-native_ref-linux-i686-1.1-natives.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/netlib-native_ref-win-x86_64/jars/netlib-native_ref-win-x86_64-1.1-natives.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/netlib-native_ref-win-i686/jars/netlib-native_ref-win-i686-1.1-natives.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/netlib-native_ref-linux-armhf/jars/netlib-native_ref-linux-armhf-1.1-natives.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/netlib-native_system-osx-x86_64/jars/netlib-native_system-osx-x86_64-1.1-natives.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/native_system-java/jars/native_system-java-1.1.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/netlib-native_system-linux-x86_64/jars/netlib-native_system-linux-x86_64-1.1-natives.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/netlib-native_system-linux-i686/jars/netlib-native_system-linux-i686-1.1-natives.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/netlib-native_system-linux-armhf/jars/netlib-native_system-linux-armhf-1.1-natives.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/netlib-native_system-win-x86_64/jars/netlib-native_system-win-x86_64-1.1-natives.jar:\
$HOME/.ivy2/cache/com.github.fommil.netlib/netlib-native_system-win-i686/jars/netlib-native_system-win-i686-1.1-natives.jar:\
$HOME/.ivy2/cache/org.scalanlp/breeze-viz_2.11/jars/breeze-viz_2.11-0.12.jar:\
$HOME/.ivy2/cache/jfree/jcommon/jars/jcommon-1.0.16.jar:\
$HOME/.ivy2/cache/jfree/jfreechart/jars/jfreechart-1.0.13.jar:\
$HOME/.ivy2/cache/org.apache.xmlgraphics/xmlgraphics-commons/jars/xmlgraphics-commons-1.3.1.jar:\
$HOME/.ivy2/cache/commons-io/commons-io/jars/commons-io-1.3.1.jar:\
$HOME/.ivy2/cache/commons-logging/commons-logging/jars/commons-logging-1.0.4.jar:\
$HOME/.ivy2/cache/com.lowagie/itext/jars/itext-2.1.5.jar:\
$HOME/.ivy2/cache/bouncycastle/bcmail-jdk14/jars/bcmail-jdk14-138.jar:\
$HOME/.ivy2/cache/bouncycastle/bcprov-jdk14/jars/bcprov-jdk14-138.jar

PYTHON_DIR_PATH=$ROOT/python	

IMG_PATH=$ROOT/images/img/flower.png
OUT_PATH=$ROOT/images/pencil_result.jpg
TEXTURE_IMG_PATH=$ROOT/images/textures/texture.jpg

# set to 0 generates the pencil drawing, 
# set to 1 generates the colourful pencil drawing
IS_DRAW_COLOR=1

# set to 1 will output the middle results
# set to 0 just output the final result
IS_SHOW_STEP=1



java -Xmx4G -cp $CLASS_PATH \
	-Djava.library.path=$LIBRARY_PATH \
	PencilStyle $IMG_PATH $OUT_PATH $TEXTURE_IMG_PATH $IS_DRAW_COLOR $PYTHON_DIR_PATH $IS_SHOW_STEP                     
