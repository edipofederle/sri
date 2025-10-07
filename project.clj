(defproject sri "0.1.0-SNAPSHOT"
  :description "Small Ruby Interpreter - A safe Ruby interpreter with API for evaluating Ruby code"
  :url "https://github.com/edipo/sri"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :main ^:skip-aot sri.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :native-image {:dependencies [[org.graalvm.nativeimage/svm "22.3.0"]]}}
  :aliases {"native" ["shell"
                      "native-image"
                      "--report-unsupported-elements-at-runtime"
                      "--initialize-at-build-time"
                      "--no-server"
                      "--no-fallback"
                      "-jar" "./target/uberjar/sri-0.1.0-SNAPSHOT-standalone.jar"
                      "-H:Name=./target/sri"]}
  :repl-options {:init-ns sri.core})
