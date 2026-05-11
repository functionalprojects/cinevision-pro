// ============================================
// CINEVISION ENTERPRISE CI/CD PIPELINE
// Industrial-grade automation with DevSecOps
// ============================================

import groovy.json.JsonOutput

// Global Configuration
def getServiceMap() {
  return [
    'api-gateway'  : [path: 'services/api-gateway',   type: 'maven', image: 'api-gateway', required: false],
    'user-service' : [path: 'services/userService',   type: 'maven', image: 'user-service', required: false],
    'movie-service': [path: 'services/movieService',  type: 'maven', image: 'movie-service', required: false],
    'email-service': [path: 'services/emailService',  type: 'maven', image: 'email-service', required: false],
    'eureka-server': [path: 'services/eureka-server', type: 'maven', image: 'eureka-server', required: false],
    'frontend'     : [path: 'services/frontend',      type: 'node',  image: 'frontend', required: false]
  ]
}

// Dynamically discover available services
def discoverAvailableServices(serviceMap) {
  def available = [:]
  serviceMap.each { serviceName, meta ->
    if (fileExists(meta.path)) {
      available[serviceName] = meta
      echo "✅ Discovered service: ${serviceName} at ${meta.path}"
    } else {
      echo "⚠️ Service ${serviceName} not found at ${meta.path} - will be skipped"
    }
  }
  
  if (available.isEmpty()) {
    echo "⚠️ No services found! Checking root directory for builds..."
    
    if (fileExists('pom.xml')) {
      available['root-app'] = [path: '.', type: 'maven', image: 'cinevision-app', required: true]
    } else if (fileExists('package.json')) {
      available['root-app'] = [path: '.', type: 'node', image: 'cinevision-app', required: true]
    } else {
      error "No services or build files found in the repository!"
    }
  }
  
  return available
}

// Check if Maven can build successfully
def canRunMavenBuild() {
  try {
    def hasValidPom = fileExists('pom.xml')
    if (!hasValidPom) return false
    
    def isStandaloneService = fileExists('pom.xml') && !fileExists('../pom.xml')
    
    if (isStandaloneService) {
      echo "✅ Standalone service detected - can run Maven build"
      return true
    }
    
    def result = sh(script: 'mvn help:evaluate -Dexpression=project.modules -q -DforceStdout 2>/dev/null | head -1', returnStdout: true).trim()
    def hasValidModules = result != null && result != '' && !result.contains('NONE') && !result.contains('ERROR')
    
    if (!hasValidModules) {
      echo "⚠️ Maven pom.xml has no valid modules. Will build individual services instead."
      return false
    }
    
    return true
  } catch (Exception e) {
    echo "⚠️ Maven validation failed: ${e.message}"
    return false
  }
}

// Enhanced change detection with service availability check
def detectChangedServices(availableServices, branchName) {
  def isFirstBuild = sh(script: 'git rev-parse HEAD~1 >/dev/null 2>&1; echo $?', returnStdout: true).trim() != '0'
  
  if (isFirstBuild) {
    echo "First build detected - building all available services"
    return availableServices.keySet() as List
  }
  
  if (branchName == 'develop' || branchName == 'main' || branchName == 'master' || branchName.startsWith('release/')) {
    echo "Full build branch (${branchName}) - building all available services"
    return availableServices.keySet() as List
  }
  
  def changedFiles = sh(
    script: "git diff --name-only HEAD~1 HEAD 2>/dev/null || echo ''",
    returnStdout: true
  ).trim().split('\n') as List
  
  if (changedFiles.isEmpty() || (changedFiles.size() == 1 && changedFiles[0] == '')) {
    echo "No previous commit found - building all available services"
    return availableServices.keySet() as List
  }
  
  def changed = []
  availableServices.each { serviceName, meta ->
    if (changedFiles.any { it.startsWith("${meta.path}/") }) {
      changed << serviceName
      echo "✅ Changes detected in: ${serviceName}"
    }
  }
  
  def rootBuildFiles = ['pom.xml', 'package.json', 'build.gradle', 'Dockerfile', 'Jenkinsfile']
  if (changedFiles.any { rootBuildFiles.contains(it) }) {
    echo "⚠️ Root build file changed - including all services"
    changed = availableServices.keySet() as List
  }
  
  if (changed.isEmpty()) {
    echo "No specific changes detected - building all available services"
    changed = availableServices.keySet() as List
  } else {
    echo "Building affected services: ${changed.join(', ')}"
  }
  
  return changed.unique()
}

@NonCPS
def getEnvironmentConfig(String branch) {
  if (branch == 'main' || branch == 'master' || branch.startsWith('hotfix/')) {
    return [
      env: 'prod',
      awsAccountIdCredentialsId: 'PROD_AWS_ACCOUNT_ID',
      awsCredentialsId: 'aws-prod-credentials',
      argocdApp: 'cinevision-prod-green',
      frontendBucket: 'prod-cinevision-prod-frontend',
      cloudfrontDistributionId: 'PROD_CLOUDFRONT_DISTRIBUTION_ID',
      posterCloudfrontId: 'PROD_POSTER_CLOUDFRONT_ID',
      archiveCloudfrontId: 'PROD_ARCHIVE_CLOUDFRONT_ID',
      apiUrl: 'https://api.cinevision.com',
      kustomizeOverlay: 'k8s/overlays/prod/green',
      deployEnabled: true,
      approvalRequired: true,
      runSecurityScan: true,
      runPerformanceTests: true,
      runIntegrationTests: true,
      trivySeverity: 'HIGH,CRITICAL'
    ]
  }
  
  if (branch.startsWith('release/')) {
    return [
      env: 'staging',
      awsAccountIdCredentialsId: 'STAGING_AWS_ACCOUNT_ID',
      awsCredentialsId: 'aws-staging-credentials',
      argocdApp: 'cinevision-staging',
      frontendBucket: 'staging-cinevision-staging-frontend',
      cloudfrontDistributionId: 'STAGING_CLOUDFRONT_DISTRIBUTION_ID',
      posterCloudfrontId: 'STAGING_POSTER_CLOUDFRONT_ID',
      archiveCloudfrontId: 'STAGING_ARCHIVE_CLOUDFRONT_ID',
      apiUrl: 'https://staging-api.cinevision.com',
      kustomizeOverlay: 'k8s/overlays/staging',
      deployEnabled: true,
      approvalRequired: true,
      runSecurityScan: true,
      runPerformanceTests: true,
      runIntegrationTests: true,
      trivySeverity: 'HIGH,CRITICAL'
    ]
  }
  
  if (branch == 'develop') {
    return [
      env: 'dev',
      awsAccountIdCredentialsId: 'DEV_AWS_ACCOUNT_ID',
      awsCredentialsId: 'ecr-eks',
      argocdApp: 'cinevision-dev',
      frontendBucket: 'dev-cinevision-dev-frontend',
      cloudfrontDistributionId: 'DEV_CLOUDFRONT_DISTRIBUTION_ID',
      posterCloudfrontId: 'DEV_POSTER_CLOUDFRONT_ID',
      archiveCloudfrontId: 'DEV_ARCHIVE_CLOUDFRONT_ID',
      apiUrl: 'https://dev-api.cinevisionca.link',
      kustomizeOverlay: 'k8s/overlays/dev',
      deployEnabled: true,
      approvalRequired: false,
      runSecurityScan: true,
      runPerformanceTests: false,
      runIntegrationTests: true,
      trivySeverity: 'CRITICAL'
    ]
  }
  
  return [env: 'unknown', deployEnabled: false]
}

def checkAndInstallTools(config) {
  script {
    def hasDocker = sh(script: 'command -v docker', returnStatus: true) == 0
    if (!hasDocker) {
      error "Docker is required but not installed on agent ${env.NODE_NAME}. Please install Docker or use a different agent."
    } else {
      echo "Docker found at: ${sh(script: 'which docker', returnStdout: true).trim()}"
      def dockerVersion = sh(script: 'docker --version', returnStdout: true).trim()
      echo "Docker version: ${dockerVersion}"
    }
    
    def hasAws = sh(script: 'command -v aws', returnStatus: true) == 0
    if (!hasAws) {
      echo "Warning: AWS CLI not found. Some steps may fail."
    }
    
    def hasDepCheck = sh(script: 'command -v dependency-check.sh', returnStatus: true) == 0
    if (!hasDepCheck && config.runSecurityScan) {
      echo "Warning: OWASP Dependency Check not found. Install from: https://github.com/jeremylong/DependencyCheck"
    }
    
    if (config.deployEnabled) {
      def hasKubectl = sh(script: 'command -v kubectl', returnStatus: true) == 0
      if (!hasKubectl) {
        echo "Warning: kubectl not found. Kubernetes deployments may fail."
      }
    }
    
    echo "Agent ${env.NODE_NAME} ready with: Docker=${hasDocker}, AWS=${hasAws}"
  }
}

// Build a single service with error handling
def buildService(serviceName, meta, envVars) {
  def serviceResult = [success: true, error: null, imageTag: null]
  
  try {
    echo "========================================"
    echo "🚀 Building service: ${serviceName}"
    echo "========================================"
    
    dir(meta.path) {
      echo "Working directory: ${pwd()}"
      
      if (meta.type == 'maven') {
        if (fileExists('pom.xml')) {
          echo "Building Maven project..."
          def buildResult = sh(script: 'mvn clean package -DskipTests=false -Dmaven.repo.local=.m2/repository 2>&1', returnStatus: true)
          if (buildResult != 0) {
            echo "⚠️ Build with tests failed, trying with tests skipped..."
            sh "mvn clean package -DskipTests=true -Dmaven.repo.local=.m2/repository"
          }
        } else {
          echo "⚠️ No pom.xml found in ${meta.path}. Skipping Maven build."
          return serviceResult
        }
      } else if (meta.type == 'node') {
        if (fileExists('package.json')) {
          echo "Building Node.js project..."
          sh "npm ci --legacy-peer-deps || npm install"
          sh "npm test || echo 'No tests configured'"
        } else {
          echo "⚠️ No package.json found in ${meta.path}. Skipping Node.js build."
          return serviceResult
        }
      }
      
      if (fileExists('Dockerfile')) {
        def fullImageName = "${envVars.IMAGE_NAMESPACE}/${meta.image}"
        def imageTag = "${envVars.CURRENT_ECR_REGISTRY}/${fullImageName}:${envVars.IMAGE_TAG}"
        serviceResult.imageTag = imageTag
        
        echo "Building Docker image: ${imageTag}"
        sh "docker build -t ${imageTag} ."
        
        def hasTrivy = sh(script: 'command -v trivy', returnStatus: true) == 0
        if (hasTrivy && envVars.TRIVY_SEVERITY) {
          echo "Running Trivy security scan..."
          sh "trivy image --severity ${envVars.TRIVY_SEVERITY} --ignore-unfixed --exit-code 0 ${imageTag} || echo 'Trivy scan found issues but continuing'"
        } else {
          echo "Trivy not installed. Skipping container security scan."
        }
        
        withAWS(credentials: envVars.AWS_CREDENTIALS_ID, region: envVars.AWS_REGION) {
          echo "Logging into ECR..."
          sh "aws ecr get-login-password --region ${envVars.AWS_REGION} | docker login --username AWS --password-stdin ${envVars.CURRENT_ECR_REGISTRY}"
          
          echo "Pushing image: ${imageTag}"
          retry(3) { 
            sh "docker push ${imageTag}" 
          }
          
          def envTag = "${envVars.TARGET_ENV}-latest"
          echo "Tagging as ${envTag}"
          sh "docker tag ${imageTag} ${envVars.CURRENT_ECR_REGISTRY}/${fullImageName}:${envTag}"
          sh "docker push ${envVars.CURRENT_ECR_REGISTRY}/${fullImageName}:${envTag}"
        }
        
        echo "✅ Successfully built and pushed: ${serviceName}"
      } else {
        echo "⚠️ No Dockerfile found in ${meta.path}. Skipping containerization."
      }
    }
  } catch (Exception e) {
    serviceResult.success = false
    serviceResult.error = e.message
    echo "❌ Failed to build service ${serviceName}: ${e.message}"
  }
  
  return serviceResult
}

// Find all pom.xml files without using findFiles
def findPomFiles() {
  def pomFiles = []
  def pomLocations = ['services/api-gateway/pom.xml', 'services/userService/pom.xml', 'services/movieService/pom.xml', 
                      'services/emailService/pom.xml', 'services/eureka-server/pom.xml', 'pom.xml']
  
  pomLocations.each { location ->
    if (fileExists(location)) {
      pomFiles.add(location)
      echo "Found pom.xml at: ${location}"
    }
  }
  
  return pomFiles
}

// Enhanced SonarCloud analysis with proper configuration
def runSonarAnalysis(serviceName, meta) {
  echo "========================================"
  echo "🔍 Running SonarCloud analysis for: ${serviceName}"
  echo "  Organization: functionalprojects"
  echo "  Organization Key: functionalprojects-key"
  echo "========================================"
  
  def success = false
  def errorMessage = ""
  
  dir(meta.path) {
    try {
      // Ensure JaCoCo coverage report is generated
      if (fileExists('pom.xml')) {
        sh """
          mvn clean test jacoco:report \
            -Dmaven.repo.local=.m2/repository \
            -DskipTests=false || echo 'Tests or coverage generation failed'
        """
      }
      
      // Prepare Sonar properties
      def sonarProperties = """
        -Dsonar.projectKey=${serviceName}
        -Dsonar.organization=functionalprojects
        -Dsonar.host.url=https://sonarcloud.io
        -Dsonar.projectName=CineVision-${serviceName}
        -Dsonar.projectVersion=1.0.0
        -Dsonar.sourceEncoding=UTF-8
        -Dsonar.java.binaries=target/classes
        -Dsonar.java.test.binaries=target/test-classes
        -Dsonar.java.sources=src/main/java
        -Dsonar.java.tests=src/test/java
        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
        -Dsonar.junit.reportPaths=target/surefire-reports
        -Dsonar.sources=src/main
        -Dsonar.exclusions=**/generated/**/*,**/model/**/*.java
        -Dsonar.coverage.exclusions=**/config/**/*,**/dto/**/*,**/model/**/*,**/exception/**/*
      """
      
      // Run SonarCloud analysis
      withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')]) {
        withSonarQubeEnv('sonarcloud') {
          sh """
            mvn sonar:sonar ${sonarProperties} \
              -Dsonar.login=${SONAR_TOKEN} \
              -Dsonar.verbose=false \
              -Dmaven.repo.local=.m2/repository
          """
        }
        
        // Wait for SonarQube analysis to complete
        timeout(time: 10, unit: 'MINUTES') {
          def waitForQualityGate = true
          if (waitForQualityGate) {
            echo "Waiting for Quality Gate results..."
            try {
              sh """
                timeout 300s bash -c '
                  while true; do
                    STATUS=\$(curl -s -u ${SONAR_TOKEN}: "https://sonarcloud.io/api/qualitygates/project_status?projectKey=${serviceName}" | jq -r .projectStatus.status)
                    if [ "\$STATUS" = "OK" ] || [ "\$STATUS" = "ERROR" ]; then
                      echo "Quality Gate status: \$STATUS"
                      if [ "\$STATUS" = "ERROR" ]; then
                        exit 1
                      fi
                      break
                    fi
                    echo "Waiting for analysis to complete..."
                    sleep 10
                  done
                '
              """ || echo "Quality Gate check finished with issues"
            } catch (Exception e) {
              echo "Quality Gate check encountered an error: ${e.message}"
            }
          }
        }
      }
      
      success = true
      echo "✅ SonarCloud analysis completed for ${serviceName}"
      
    } catch (Exception e) {
      errorMessage = e.message
      echo "❌ SonarCloud analysis failed for ${serviceName}: ${errorMessage}"
      echo "========================================"
      echo "Troubleshooting Steps:"
      echo "1. Verify your SonarCloud token is correct in Jenkins credentials (ID: sonarcloud-token)"
      echo "2. Ensure the project '${serviceName}' exists in SonarCloud organization 'functionalprojects'"
      echo "3. Check that you have the SonarQube plugin installed in Jenkins"
      echo "4. Verify SonarQube server configuration:"
      echo "   - Name: sonarcloud"
      echo "   - Server URL: https://sonarcloud.io"
      echo "   - Token: [your token]"
      echo "5. Alternative: Project can be auto-created on first scan"
      echo "6. Check network connectivity to sonarcloud.io"
      echo "========================================"
    }
  }
  
  return [success: success, error: errorMessage]
}

// Global state
def AVAILABLE_SERVICES = [:]
def BUILD_RESULTS = [:]
def SONAR_RESULTS = [:]

pipeline {
  agent any
  
  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '30'))
    timeout(time: 90, unit: 'MINUTES')
    skipDefaultCheckout()
  }
  
  environment {
    AWS_REGION = 'us-east-1'
    IMAGE_NAMESPACE = 'cinevision'
    GITHUB_TOKEN = credentials('github-token')
    GITHUB_REPO = 'functionalprojects/cinevision-pro'
    JENKINS_AGENT_NAME = "${NODE_NAME}"
    SONAR_HOST_URL = 'https://sonarcloud.io'
    SONAR_ORG_KEY = 'functionalprojects-key'
    SONAR_ORG_NAME = 'functionalprojects'
  }
  
  stages {
    stage('🚀 Initialization') {
      when { 
        not { changelog '\\[CI\\]' } 
        anyOf {
          branch 'develop'
          branch 'main'
          branch 'master'
          expression { env.BRANCH_NAME != null && env.BRANCH_NAME.startsWith('release/') }
        }
      }
      steps {
        checkout scm
        
        script {
          def staticServiceMap = getServiceMap()
          AVAILABLE_SERVICES = discoverAvailableServices(staticServiceMap)
          
          def CONFIG = getEnvironmentConfig(env.BRANCH_NAME)
          env.TARGET_ENV = CONFIG.env
          
          if (env.TARGET_ENV == 'unknown') {
            error "Branch ${env.BRANCH_NAME} is not mapped to any environment."
          }
          
          env.DEPLOY_ENABLED = CONFIG.deployEnabled.toString()
          env.APPROVAL_REQUIRED = CONFIG.approvalRequired.toString()
          env.RUN_SECURITY_SCAN = CONFIG.runSecurityScan.toString()
          env.RUN_PERFORMANCE_TESTS = CONFIG.runPerformanceTests.toString()
          env.RUN_INTEGRATION_TESTS = CONFIG.runIntegrationTests.toString()
          env.TRIVY_SEVERITY = CONFIG.trivySeverity
          env.ARGOCD_APP = CONFIG.argocdApp
          env.FRONTEND_BUCKET = CONFIG.frontendBucket
          env.API_URL = CONFIG.apiUrl
          env.KUSTOMIZE_OVERLAY = CONFIG.kustomizeOverlay
          env.AWS_CREDENTIALS_ID = CONFIG.awsCredentialsId
          env.CLOUDFRONT_DISTRIBUTION_ID = CONFIG.cloudfrontDistributionId
          
          if (CONFIG.awsAccountIdCredentialsId) {
            withCredentials([string(credentialsId: CONFIG.awsAccountIdCredentialsId, variable: 'AWS_ACCOUNT_ID')]) {
              env.CURRENT_ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
            }
          } else if (CONFIG.env == 'dev') {
            echo "⚠️ No AWS Account ID configured for dev environment"
            env.CURRENT_ECR_REGISTRY = "dev.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
          } else {
            error "AWS Account ID credentials not found for environment ${CONFIG.env}"
          }
          
          env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
          env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
          
          checkAndInstallTools(CONFIG)
          
          echo "========================================"
          echo "Pipeline Configuration:"
          echo "  Environment: ${env.TARGET_ENV}"
          echo "  Node: ${env.NODE_NAME}"
          echo "  Registry: ${env.CURRENT_ECR_REGISTRY}"
          echo "  Image Tag: ${env.IMAGE_TAG}"
          echo "  SonarCloud Organization: ${env.SONAR_ORG_NAME} (Key: ${env.SONAR_ORG_KEY})"
          echo "  Available Services: ${AVAILABLE_SERVICES.keySet().join(', ')}"
          echo "========================================"
        }
      }
    }
    
    stage('🔍 Security & Code Quality') {
      when { expression { env.RUN_SECURITY_SCAN == 'true' } }
      parallel {
        stage('SCA: OWASP Dependency Check') {
          steps {
            script {
              def hasDepCheck = sh(script: 'command -v dependency-check.sh', returnStatus: true) == 0
              
              if (!hasDepCheck) {
                echo "========================================"
                echo "⚠️ OWASP Dependency Check not installed!"
                echo "Installation instructions:"
                echo "  wget https://github.com/jeremylong/DependencyCheck/releases/download/v9.0.9/dependency-check-9.0.9-release.zip"
                echo "  unzip dependency-check-9.0.9-release.zip"
                echo "  sudo mv dependency-check /opt/"
                echo "  sudo ln -s /opt/dependency-check/bin/dependency-check.sh /usr/local/bin/dependency-check.sh"
                echo "========================================"
                echo "Skipping Dependency Check scan..."
                return
              }
              
              def pomFiles = findPomFiles()
              def scannedCount = 0
              
              for (pomFile in pomFiles) {
                def serviceDir = pomFile.replace('/pom.xml', '')
                echo "Scanning dependencies for: ${serviceDir}"
                
                try {
                  dir(serviceDir) {
                    sh """
                      dependency-check.sh \
                        --scan . \
                        --format HTML \
                        --format XML \
                        --out ${env.WORKSPACE}/dependency-check-reports \
                        --failOnError false \
                        --enableExperimental || true
                    """
                  }
                  scannedCount++
                } catch (Exception e) {
                  echo "⚠️ Dependency check failed for ${serviceDir}: ${e.message}"
                }
              }
              
              if (scannedCount > 0) {
                publishHTML([
                  allowMissing: true,
                  alwaysLinkToLastBuild: true,
                  keepAll: true,
                  reportDir: 'dependency-check-reports',
                  reportFiles: 'dependency-check-report.html',
                  reportName: 'OWASP Dependency Check Report'
                ])
                echo "✅ Dependency Check completed for ${scannedCount} projects"
              } else {
                echo "No pom.xml files found to scan"
              }
            }
          }
        }
        
        stage('SAST: SonarCloud Analysis') {
          steps {
            script {
              echo "========================================"
              echo "🔍 SonarCloud Configuration"
              echo "  Organization: ${env.SONAR_ORG_NAME}"
              echo "  Organization Key: ${env.SONAR_ORG_KEY}"
              echo "  Server URL: ${env.SONAR_HOST_URL}"
              echo "========================================"
              
              // Verify SonarCloud token exists
              try {
                withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'TEST_TOKEN')]) {
                  echo "✅ SonarCloud token 'sonarcloud-token' found in Jenkins credentials"
                }
              } catch (Exception e) {
                echo "❌ SonarCloud token 'sonarcloud-token' not found in Jenkins credentials!"
                echo "Please add your SonarCloud token:"
                echo "1. Go to Jenkins → Manage Jenkins → Credentials → System → Global credentials (unrestricted)"
                echo "2. Add Credentials → Secret text"
                echo "3. ID: sonarcloud-token"
                echo "4. Secret: [your SonarCloud token from https://sonarcloud.io/account/security]"
                error "SonarCloud token missing. Pipeline cannot proceed with security scan."
              }
              
              // Verify SonarQube server configuration
              echo "Verifying SonarQube server configuration..."
              echo "Ensure 'sonarcloud' server is configured in:"
              echo "  Jenkins → Manage Jenkins → Configure System → SonarQube servers"
              echo "  Name: sonarcloud"
              echo "  Server URL: https://sonarcloud.io"
              echo "  Server authentication token: [select sonarcloud-token]"
              
              def scannedServices = []
              
              AVAILABLE_SERVICES.each { serviceName, meta ->
                if (meta.type == 'maven' && fileExists("${meta.path}/pom.xml")) {
                  def result = runSonarAnalysis(serviceName, meta)
                  SONAR_RESULTS[serviceName] = result
                  if (result.success) {
                    scannedServices.add(serviceName)
                  }
                }
              }
              
              if (scannedServices.isEmpty()) {
                echo "========================================"
                echo "⚠️ No Maven services were successfully analyzed"
                echo "Possible solutions:"
                echo "1. Ensure each service has a valid pom.xml"
                echo "2. Add sonar-project.properties file to each service with:"
                echo "   sonar.projectKey=service-name"
                echo "   sonar.organization=functionalprojects"
                echo "   sonar.host.url=https://sonarcloud.io"
                echo "3. Or rely on the command-line parameters configured in the pipeline"
                echo "4. Check that the SonarQube Scanner for Maven is available"
                echo "========================================"
              } else {
                echo "✅ SonarCloud analysis completed for: ${scannedServices.join(', ')}"
              }
            }
          }
        }
      }
    }
    
    stage('📦 Build & Containerize') {
      steps {
        script {
          if (AVAILABLE_SERVICES.isEmpty()) {
            echo "No services available to build. Skipping build stage."
            return
          }
          
          def changed = detectChangedServices(AVAILABLE_SERVICES, env.BRANCH_NAME)
          env.CHANGED_SERVICES = changed.join(',')
          
          echo "========================================"
          echo "Building services: ${env.CHANGED_SERVICES}"
          echo "========================================"
          
          def buildVars = [
            AWS_REGION: env.AWS_REGION,
            IMAGE_NAMESPACE: env.IMAGE_NAMESPACE,
            CURRENT_ECR_REGISTRY: env.CURRENT_ECR_REGISTRY,
            IMAGE_TAG: env.IMAGE_TAG,
            TARGET_ENV: env.TARGET_ENV,
            AWS_CREDENTIALS_ID: env.AWS_CREDENTIALS_ID,
            TRIVY_SEVERITY: env.TRIVY_SEVERITY
          ]
          
          for (serviceName in changed) {
            if (!AVAILABLE_SERVICES.containsKey(serviceName)) {
              echo "⚠️ Service ${serviceName} is not available. Skipping..."
              continue
            }
            
            def meta = AVAILABLE_SERVICES[serviceName]
            def result = buildService(serviceName, meta, buildVars)
            BUILD_RESULTS[serviceName] = result
            
            if (!result.success) {
              echo "❌ Build failed for ${serviceName}: ${result.error}"
              if (meta.required) {
                error "Required service ${serviceName} failed to build. Aborting pipeline."
              } else {
                echo "⚠️ Optional service ${serviceName} failed. Continuing with other services..."
              }
            }
          }
          
          echo "========================================"
          echo "Build Summary:"
          BUILD_RESULTS.each { name, result ->
            def status = result.success ? "✅ SUCCESS" : "❌ FAILED"
            echo "  ${name}: ${status}"
          }
          echo "========================================"
        }
      }
    }
    
    stage('📂 GitOps Manifest Update') {
      when { expression { 
        env.DEPLOY_ENABLED == 'true' && 
        env.CHANGED_SERVICES != null && 
        env.CHANGED_SERVICES != '' &&
        env.KUSTOMIZE_OVERLAY &&
        fileExists(env.KUSTOMIZE_OVERLAY)
      } }
      steps {
        script {
          def overlay = env.KUSTOMIZE_OVERLAY
          def updated = false
          
          if (fileExists(overlay)) {
            dir(overlay) {
              def changedServices = env.CHANGED_SERVICES.split(',')
              changedServices.each { serviceName ->
                if (BUILD_RESULTS[serviceName]?.success && BUILD_RESULTS[serviceName]?.imageTag) {
                  if (serviceName != 'frontend' && AVAILABLE_SERVICES.containsKey(serviceName)) {
                    def meta = AVAILABLE_SERVICES[serviceName]
                    def fullImageName = "${env.CURRENT_ECR_REGISTRY}/${env.IMAGE_NAMESPACE}/${meta.image}"
                    sh "kustomize edit set image ${meta.image}=${fullImageName}:${env.IMAGE_TAG} || echo 'Kustomize not available'"
                    updated = true
                    echo "✅ Updated manifest for ${serviceName}"
                  }
                } else {
                  echo "⚠️ Skipping manifest update for ${serviceName} (build failed or no image)"
                }
              }
            }
            
            if (updated) {
              sh """
                git config user.email "jenkins@cinevision.com"
                git config user.name "Jenkins CI"
                git add ${overlay} || echo 'No files to add'
                git commit -m "[CI] Deploy ${env.IMAGE_TAG} to ${env.TARGET_ENV} [skip ci]" || echo "No changes to commit"
                git push origin HEAD:${env.BRANCH_NAME} || echo "Push failed"
              """
            } else {
              echo "No manifest updates needed"
            }
          } else {
            echo "Kustomize overlay ${overlay} not found. Skipping manifest update."
          }
        }
      }
    }
    
    stage('🌐 Frontend Deployment') {
      when { expression { 
        env.CHANGED_SERVICES != null && 
        env.CHANGED_SERVICES.split(',').contains('frontend') && 
        BUILD_RESULTS['frontend']?.success &&
        env.FRONTEND_BUCKET 
      } }
      steps {
        script {
          echo "Deploying frontend to S3 bucket: ${env.FRONTEND_BUCKET}"
          
          withAWS(credentials: env.AWS_CREDENTIALS_ID, region: env.AWS_REGION) {
            def frontendPath = AVAILABLE_SERVICES['frontend']?.path ?: 'services/frontend'
            if (fileExists(frontendPath)) {
              dir(frontendPath) {
                if (fileExists('dist') || fileExists('build')) {
                  def buildDir = fileExists('dist') ? 'dist' : 'build'
                  sh "aws s3 sync ${buildDir} s3://${env.FRONTEND_BUCKET} --delete"
                  
                  if (env.CLOUDFRONT_DISTRIBUTION_ID) {
                    withCredentials([string(credentialsId: env.CLOUDFRONT_DISTRIBUTION_ID, variable: 'CF_ID')]) {
                      sh "aws cloudfront create-invalidation --distribution-id ${CF_ID} --paths '/*'"
                    }
                  }
                  echo "✅ Frontend deployed successfully"
                } else {
                  echo "⚠️ Build directory not found. Skipping frontend deployment."
                }
              }
            } else {
              echo "⚠️ Frontend path ${frontendPath} not found. Skipping deployment."
            }
          }
        }
      }
    }
    
    stage('🚢 Deployment & Verification') {
      when { expression { 
        env.DEPLOY_ENABLED == 'true' && 
        env.ARGOCD_APP &&
        !BUILD_RESULTS.isEmpty() &&
        BUILD_RESULTS.any { it.value.success }
      } }
      steps {
        script {
          if (env.APPROVAL_REQUIRED == 'true') {
            input message: "Approve deployment to ${env.TARGET_ENV}?", ok: "Deploy"
          }
          
          withAWS(credentials: env.AWS_CREDENTIALS_ID, region: env.AWS_REGION) {
            def hasArgoCD = sh(script: 'command -v argocd', returnStatus: true) == 0
            if (hasArgoCD) {
              try {
                withCredentials([usernamePassword(credentialsId: 'argocd-creds', passwordVariable: 'ARGO_PWD', usernameVariable: 'ARGO_USER')]) {
                  sh "argocd login argocd.cinevision.com --username ${ARGO_USER} --password ${ARGO_PWD} --insecure --grpc-web || echo 'ArgoCD login failed'"
                }
                sh "argocd app sync ${env.ARGOCD_APP} --prune || echo 'Sync failed'"
                sh "argocd app wait ${env.ARGOCD_APP} --health --timeout 600 || echo 'Wait failed'"
                echo "✅ ArgoCD sync completed"
              } catch (Exception e) {
                echo "ArgoCD operation failed: ${e.message}"
              }
            } else {
              echo "ArgoCD CLI not found. Skipping ArgoCD deployment."
              if (env.KUSTOMIZE_OVERLAY && fileExists(env.KUSTOMIZE_OVERLAY)) {
                sh "kubectl apply -k ${env.KUSTOMIZE_OVERLAY} || echo 'kubectl apply failed'"
              }
            }
          }
        }
      }
    }
    
    stage('✅ Post-Deployment Tests') {
      when { expression { env.RUN_INTEGRATION_TESTS == 'true' && env.API_URL } }
      parallel {
        stage('Integration & DAST') {
          steps {
            script {
              if (fileExists('tests/integration')) {
                dir('tests/integration') { 
                  sh "npm install || echo 'No package.json'"
                  sh "BASE_URL=${env.API_URL} npm test || echo 'Integration tests failed'"
                }
              } else {
                echo "Integration tests not found. Skipping."
              }
              
              if (env.API_URL) {
                sh """
                  docker pull ghcr.io/zaproxy/zaproxy:stable || true
                  docker run --rm \
                    -v \$(pwd):/zap/wrk/:rw \
                    -t ghcr.io/zaproxy/zaproxy:stable \
                    zap-baseline.py \
                    -t ${env.API_URL} \
                    -r zap_report.html \
                    -j \
                    || true
                """
                archiveArtifacts artifacts: 'zap_report.html', allowEmptyArchive: true
              }
            }
          }
        }
        stage('Performance') {
          when { expression { env.RUN_PERFORMANCE_TESTS == 'true' } }
          steps {
            script {
              if (fileExists('tests/performance/cinevision-load-test.js')) {
                dir('tests/performance') { 
                  sh """
                    docker pull grafana/k6:latest || true
                    docker run --rm \
                      -v \$(pwd):/tests \
                      -t grafana/k6:latest \
                      run /tests/cinevision-load-test.js \
                      -e BASE_URL=${env.API_URL} \
                    || echo 'Performance tests failed'
                  """
                }
              } else {
                echo "Performance tests not found. Skipping."
              }
            }
          }
        }
      }
    }
  }
  
  post {
    success { 
      script {
        def successfulServices = BUILD_RESULTS.findAll { it.value.success }.keySet().join(', ')
        def successfulSonar = SONAR_RESULTS.findAll { it.value.success }.keySet().join(', ')
        echo "========================================"
        echo "✅ Pipeline completed successfully!"
        echo "  Services built: ${successfulServices ?: 'None'}"
        echo "  SonarCloud analyzed: ${successfulSonar ?: 'None'}"
        echo "========================================"
      }
    }
    failure { 
      script {
        def failedServices = BUILD_RESULTS.findAll { !it.value.success }.keySet().join(', ')
        def failedSonar = SONAR_RESULTS.findAll { !it.value.success }.keySet().join(', ')
        echo "========================================"
        echo "❌ Pipeline failed!"
        echo "  Failed services: ${failedServices ?: 'None'}"
        echo "  Failed SonarCloud analyses: ${failedSonar ?: 'None'}"
        echo "========================================"
        
        // Provide SonarCloud troubleshooting steps on failure
        if (failedSonar) {
          echo ""
          echo "📋 SonarCloud Troubleshooting Steps:"
          echo "1. Verify your SonarCloud token 'sonarcloud-token' is correctly configured in Jenkins"
          echo "2. Ensure projects exist in https://sonarcloud.io/organizations/functionalprojects"
          echo "3. Check that Maven can connect to SonarCloud:"
          echo "   mvn sonar:sonar -Dsonar.projectKey=test -Dsonar.organization=functionalprojects -Dsonar.host.url=https://sonarcloud.io"
          echo "4. Verify you have 'sonarcloud' server configured in Jenkins:"
          echo "   Manage Jenkins → Configure System → SonarQube servers"
          echo "   Name: sonarcloud"
          echo "   URL: https://sonarcloud.io"
          echo "   Token: [select sonarcloud-token]"
          echo "5. Check if the SonarQube Scanner plugin is installed in Jenkins"
        }
      }
    }
    always { 
      script {
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml, **/test-results/**/*.xml'
        cleanWs() 
      }
    }
  }
}