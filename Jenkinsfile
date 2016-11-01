node ('linux'){
   wrap([$class: 'TimestamperBuildWrapper']) {
    def mvnHome
    stage ('Clean workspace') {
       step([$class: 'WsCleanup'])
    }
    stage('Preparation') { // for display purposes
      // Get some code from a GitHub repository
      checkout scm
      mvnHome = tool 'M3'
    }
    stage('Build') {
      // Run the maven build
      if (isUnix()) {
         sh "'${mvnHome}/bin/mvn' -Dmaven.test.failure.ignore clean package"
         //sh "'${mvnHome}/bin/mvn' org.sonarsource.scanner.maven:sonar-maven-plugin:3.1.1:sonar"
      } else {
         bat(/"${mvnHome}\bin\mvn" -Dmaven.test.failure.ignore clean package/)
      }
    }
    stage('Unit Results') {
      junit allowEmptyResults: true, keepLongStdio: true, testResults: '**/target/surefire-reports/TEST-*.xml'
      //junit '**/target/surefire-reports/TEST-*.xml'
      archive 'target/*.jar'
    }
    stage('Upload to Nexus') {
       
       nexusArtifactUploader artifactId: '${JOB_NAME}', classifier: 'debug', credentialsId: '', file: '/var/lib/jenkins/workspace/new-job/multi-module/webapp/target/webapp.war', groupId: 'multi-module-parent', nexusPassword: 'admin123', nexusUrl: '192.168.59.103:8082/nexus', nexusUser: 'admin', nexusVersion: 'nexus2', protocol: 'http', repository: 'snapshots', type: 'war', version: '1.0.${BUILD_NUMBER}-SNAPSHOT'
    }
  }
}
