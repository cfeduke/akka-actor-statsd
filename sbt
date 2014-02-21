#!/bin/sh
# attempts to execute ~/.sbtrc then <project>/.sbtrc
java \
  -Xms512M \
  -Xmx1536M \
  -Xss1M \
  -XX:+CMSClassUnloadingEnabled \
  -XX:MaxPermSize=768M \
  -jar `dirname $0`/sbt-launch.jar \
  "$@"
