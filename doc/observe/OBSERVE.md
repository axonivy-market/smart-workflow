

download:
wget -O opentelemetry-javaagent.jar -q -N https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/2.25.0/opentelemetry-javaagent-2.25.0.jar

copy the 'jvm.options' 
into an engine of your choice.

starting the dev-engine:
- does not work if run by vsc extension
- switch to run engine manually in axonivy extension settings
- launch the engine with the agent in place
- launch VSC afterwards  (reload windows)

