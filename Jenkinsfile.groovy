properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5')), parameters([string(defaultValue: 'AP5P5hu1iVpC7PnPLwoSg6YCs3k', description: '', name: 'SERVER_ID')]), pipelineTriggers([pollSCM('* * * * *')])])
node {
   
   def mvnHome
   ansiColor('xterm') {
      
      timestamps {

         withSonarQubeEnv {
         
            stage('Preparation') { scmcheckout() }
            stage ('SonarQube Analysis') { sonarQubeAnalysis() }
            stage ('Maven Build') { build() }
            stage ('Unit Test') { Results() }
            stage ('Upload Artifact') { UploadArtifact() }
            step([$class: 'WsCleanup'])

         }     
      
      }


   }
   

}

def scmcheckout() {
   
   git 'https://github.com/peddadabrp/sample-maven-project.git'
   mvnHome = tool 'M2'
   
}
def sonarQubeAnalysis() {
   
   sh "'${mvnHome}/bin/mvn' org.sonarsource.scanner.maven:sonar-maven-plugin:3.2:sonar"
}
def build() {
   
   // Run the maven build
   if (isUnix()) {
      sh "'${mvnHome}/bin/mvn' -Dmaven.test.failure.ignore clean package"
   } else {
      bat(/"${mvnHome}\bin\mvn" -Dmaven.test.failure.ignore clean package/)
   }

}
def Results() {
   
   junit '**/target/surefire-reports/TEST-*.xml'
   archive 'target/*.jar'

}
def UploadArtifactToArtifactory() {
 
    echo 'printing Build Stamp on Artifact'
    sh 'mv $WORKSPACE/target/sample-maven-project-0.1-SNAPSHOT-sources.jar $WORKSPACE/target/sample-maven-project-0.1.${BUILD_NUMBER}-SNAPSHOT-sources.jar'  
   // Get Artifactory server instance, defined in the Artifactory Plugin administration page.
    def server = Artifactory.server SERVER_ID

    // Read the upload spec which was downloaded from github.
    def uploadSpec = '$WORKSPACE/target/sample-maven-project-0.1.${BUILD_NUMBER}-SNAPSHOT-sources.jar'
    // Upload to Artifactory.
    def buildInfo1 = server.upload spec: uploadSpec

    // Read the upload spec and upload files to Artifactory.
    def downloadSpec = readFile 'jenkins-pipeline-examples/resources/props-download.json'
    def buildInfo2 = server.download spec: downloadSpec

    // Merge the upload and download build-info objects.
    buildInfo1.append buildInfo2

    // Publish the build to Artifactory
    server.publishBuildInfo buildInfo1   
    
}
def UploadArtifact() {

    echo 'printing Build Stamp on Artifacts'
    sh 'mv $WORKSPACE/target/sample-maven-project-0.1-SNAPSHOT-sources.jar $WORKSPACE/target/sample-maven-project-0.1.${BUILD_NUMBER}-SNAPSHOT-sources.jar'
    sh 'mv $WORKSPACE/target/sample-maven-project-0.1-SNAPSHOT-javadoc.jar $WORKSPACE/target/sample-maven-project-0.1.${BUILD_NUMBER}-SNAPSHOT-javadoc.jar'
    sh 'mv $WORKSPACE/target/sample-maven-project-0.1-SNAPSHOT.jar $WORKSPACE/target/sample-maven-project-0.1.${BUILD_NUMBER}-SNAPSHOT.jar'
    fingerprint 'target/**.jar'
    sh 'ls -lart target/'
    def server = Artifactory.server "$SERVER_ID"
    def buildInfo = Artifactory.newBuildInfo()
    buildInfo.env.capture = true
    buildInfo.env.collect()

    def uploadSpec = """{
      "files": [
        {
          "pattern": "target/**.jar",
          "target": "libs-snapshot-local"
        }, {
          "pattern": "target/*.pom",
          "target": "libs-snapshot-local"
        }, {
          "pattern": "target/*.war",
          "target": "libs-snapshot-local"
        }
      ]
    }"""
    // Upload to Artifactory.
    server.upload spec: uploadSpec, buildInfo: buildInfo

    buildInfo.retention maxBuilds: 10, maxDays: 7, deleteBuildArtifacts: true
    // Publish build info.
    server.publishBuildInfo buildInfo
}
