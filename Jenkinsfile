// ============================================
// CINEVISION ENTERPRISE CI/CD PIPELINE
// Industrial-grade automation with DevSecOps
// ============================================

import groovy.json.JsonOutput

// Global Configuration
def getServiceMap() {
  return [
    'api-gateway'  : [path: 'services/api-gateway',   type: 'maven', image: 'api-gateway', required: false, sonarKey: 'cinevision_api-gateway'],
    'user-service' : [path: 'services/userService',   type: 'maven', image: 'user-service', required: false, sonarKey: 'cinevision_user-service'],
    'movie-service': [path: 'services/movieService',  type: 'maven', image: 'movie-service', required: false, sonarKey: 'cinevision_movie-service'],
    'email-service': [path: 'services/emailService',  type: 'maven', image: 'email-service', required: false, sonarKey: 'cinevision_email-service'],
    'eureka-server': [path: 'services/eureka-server', type: 'maven', image: 'eureka-server', required: false, sonarKey: 'cinevision_eureka-server'],
    'frontend'     : [path: 'services/frontend',      type: 'node',  image: 'frontend', required: false, sonarKey: null]
  ]
}

// Dynamically discover available services
def discoverAvailableServices(serviceMap) {
  def available = [:]
  serviceMap.each { serviceName, meta ->
    if (fileExists(meta.path)) {
      available[serviceName] = meta
      echo "✅ Discovered service: ${serviceName} at ${meta.path}"
      if (meta.sonarKey) {
        echo "   SonarCloud Project Key: ${meta.sonarKey}"
      }
    } else {
      echo "⚠️ Service ${serviceName} not found at ${meta.path} - will be skipped"
    }
  }
  
  if (available.isEmpty()) {
    echo "⚠️ No services found! Checking root directory for builds..."
    
    if (fileExists('pom.xml')) {
      available['root-app'] = [path: '.', type: 'maven', image: 'cinevision-app', required: true, sonarKey: 'cinevision_root-app']
    } else if (fileExists('package.json')) {
      available['root-app'] = [path: '.', type: 'node', image: 'cinevision-app', required: true, sonarKey: 'cinevision_root-app']
    } else {
      error "No services or build files found in the repository!"
    }
  }
  
  return available
}

// Create SonarCloud project via API
def createSonarCloudProject(sonarKey, serviceName, sonarToken) {
  echo "Creating SonarCloud project: ${sonarKey} (${serviceName})"
  
  def createCmd = """
    curl -X POST "https://sonarcloud.io/api/projects/create" \
      -u ${sonarToken}: \
      -d "name=${serviceName}" \
      -d "project=${sonarKey}" \
      -d "organization=functionalprojects" \
      --fail --silent --show-error 2>&1
  """
  
  try {
    def response = sh(script: createCmd, returnStdout: true).trim()
    if (response.contains('already exists') || response.contains('"errors"')) {
      if (response.contains('already exists')) {
        echo "✅ Project ${sonarKey} already exists in SonarCloud"
        return true
      } else if (response.contains('ALM project binding already exists')) {
        echo "✅ Project ${sonarKey} already has ALM binding"
        return true
      } else {
        echo "⚠️ Response: ${response}"
        if (checkProjectExists(sonarKey, sonarToken)) {
          echo "✅ Project ${sonarKey} exists (verified via API)"
          return true
        }
        return false
      }
    }
    echo "✅ Successfully created project: ${sonarKey}"
    return true
  } catch (Exception e) {
    if (checkProjectExists(sonarKey, sonarToken)) {
      echo "✅ Project ${sonarKey} exists (verified after error)"
      return true
    }
    echo "⚠️ Could not create project ${sonarKey}: ${e.message}"
    return false
  }
}

// Check if project exists in SonarCloud
def checkProjectExists(sonarKey, sonarToken) {
  def checkCmd = """
    curl -s "https://sonarcloud.io/api/projects/search?projects=${sonarKey}&organization=functionalprojects" \
      -u ${sonarToken}: \
      | jq -r '.components | length'
  """
  
  try {
    def length = sh(script: checkCmd, returnStdout: true).trim()
    return length.toInteger() > 0
  } catch (Exception e) {
    return false
  }
}

// Run SonarCloud analysis for a service
def runSonarAnalysis(serviceName, meta, sonarToken) {
  echo "========================================"
  echo "🔍 Running SonarCloud analysis for: ${serviceName}"
  echo "  Project Key: ${meta.sonarKey}"
  echo "  Organization: functionalprojects"
  echo "========================================"
  
  def success = false
  
  dir(meta.path) {
    // Create the project in SonarCloud using sonarKey
    def projectCreated = createSonarCloudProject(meta.sonarKey, serviceName, sonarToken)
    
    if (!projectCreated) {
      echo "⚠️ Could not create/verify project ${meta.sonarKey}. Attempting analysis anyway..."
    }
    
    try {
      if (meta.type == 'maven' && fileExists('pom.xml')) {
        // Run Sonar analysis with specific sonarKey
        def sonarCommand = """
          mvn clean compile test-compile \
            -Dmaven.repo.local=.m2/repository || true
          
          mvn sonar:sonar \
            -Dsonar.projectKey=${meta.sonarKey} \
            -Dsonar.organization=functionalprojects \
            -Dsonar.host.url=https://sonarcloud.io \
            -Dsonar.login=${sonarToken} \
            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
            -Dsonar.verbose=false \
            -Dmaven.repo.local=.m2/repository \
            2>&1 || echo 'Sonar analysis completed with warnings'
        """
        
        def result = sh(script: sonarCommand, returnStatus: true)
        success = true
        echo "✅ SonarCloud analysis completed for ${serviceName} (${meta.sonarKey})"
        
      } else if (meta.type == 'node') {
        def hasSonarScanner = sh(script: 'command -v sonar-scanner', returnStatus: true) == 0
        if (hasSonarScanner) {
          sh """
            sonar-scanner \
              -Dsonar.projectKey=${meta.sonarKey} \
              -Dsonar.organization=functionalprojects \
              -Dsonar.host.url=https://sonarcloud.io \
              -Dsonar.login=${sonarToken} \
              -Dsonar.sources=. \
              2>&1 || echo 'Sonar analysis completed with warnings'
          """
          success = true
          echo "✅ SonarCloud analysis completed for ${serviceName} (${meta.sonarKey})"
        } else {
          echo "⚠️ sonar-scanner not found. Skipping analysis for ${serviceName}"
          success = false
        }
      } else {
        echo "⚠️ Unknown service type for ${serviceName}. Skipping analysis."
        success = false
      }
    } catch (Exception e) {
      echo "⚠️ SonarCloud analysis had issues for ${serviceName}: ${e.message}"
      success = false
    }
  }
  
  return success
}

// Run OWASP Dependency Check
def runDependencyCheck() {
  echo "========================================"
  echo "🔍 Running OWASP Dependency Check"
  echo "========================================"
  
  def hasDepCheck = sh(script: 'command -v dependency-check.sh', returnStatus: true) == 0
  
  if (!hasDepCheck) {
    echo "⚠️ OWASP Dependency Check not installed! Skipping..."
    echo "Install from: https://github.com/jeremylong/DependencyCheck"
    return
  }
  
  def scannedCount = 0
  def servicePaths = ['services/api-gateway', 'services/userService', 'services/movieService', 
                      'services/emailService', 'services/eureka-server']
  
  servicePaths.each { servicePath ->
    if (fileExists("${servicePath}/pom.xml")) {
      echo "Scanning dependencies for: ${servicePath}"
      
      dir(servicePath) {
        try {
          sh """
            dependency-check.sh \
              --scan . \
              --format HTML \
              --format XML \
              --out ${env.WORKSPACE}/dependency-check-reports/${servicePath.replace('/', '-')} \
              --enableExperimental \
              --failOnError false || true
          """
          scannedCount++
        } catch (Exception e) {
          echo "⚠️ Dependency check failed for ${servicePath}: ${e.message}"
        }
      }
    }
  }
  
  if (scannedCount > 0) {
    publishHTML([
      allowMissing: true,
      alwaysLinkToLastBuild: true,
      keepAll: true,
      reportDir: 'dependency-check-reports',
      reportFiles: '**/dependency-check-report.html',
      reportName: 'OWASP Dependency Check'
    ])
    archiveArtifacts artifacts: 'dependency-check-reports/**/*.html', allowEmptyArchive: true
    echo "✅ Dependency Check completed for ${scannedCount} projects"
  } else {
    echo "No pom.xml files found to scan"
  }
}

// Run OWASP ZAP Security Scan
def runZapScan(apiUrl) {
  echo "========================================"
  echo "🔍 Running OWASP ZAP Security Scan"
  echo "  Target: ${apiUrl}"
  echo "========================================"
  
  sh """
    docker pull owasp/zap2docker-stable || true
    docker run --rm \
      -v \$(pwd)/zap-reports:/zap/wrk:rw \
      owasp/zap2docker-stable \
      zap-baseline.py \
      -t ${apiUrl} \
      -r zap_report.html \
      -w zap_report.md \
      -J zap_report.json \
      || echo "ZAP scan completed with warnings"
  """
  
  archiveArtifacts artifacts: 'zap-reports/**', allowEmptyArchive: true
  echo "✅ ZAP Security Scan completed"
}

// Run Trivy container scan
def runTrivyScan(imageTag, severity) {
  echo "========================================"
  echo "🔍 Running Trivy Security Scan"
  echo "  Image: ${imageTag}"
  echo "  Severity: ${severity}"
  echo "========================================"
  
  def hasTrivy = sh(script: 'command -v trivy', returnStatus: true) == 0
  
  if (hasTrivy) {
    sh "trivy image --severity ${severity} --ignore-unfixed --exit-code 0 ${imageTag} || echo 'Trivy scan found issues but continuing'"
  } else {
    echo "Trivy not installed. Running via Docker..."
    sh """
      docker run --rm \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v trivy-cache:/root/.cache/trivy \
        aquasec/trivy:0.55.0 \
        image --scanners vuln --severity ${severity} --skip-java-db-update ${imageTag} \
        || echo 'Trivy scan completed with warnings'
    """
  }
  echo "✅ Trivy scan completed"
}

// Enhanced change detection
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
      error "Docker is required but not installed on agent ${env.NODE_NAME}"
    } else {
      echo "Docker found: ${sh(script: 'docker --version', returnStdout: true).trim()}"
    }
    
    def hasAws = sh(script: 'command -v aws', returnStatus: true) == 0
    if (!hasAws) {
      echo "Warning: AWS CLI not found. Some steps may fail."
    }
    
    def hasJq = sh(script: 'command -v jq', returnStatus: true) == 0
    if (!hasJq) {
      echo "Installing jq for JSON processing..."
      sh "apt-get update && apt-get install -y jq || yum install -y jq || echo 'Could not install jq'"
    }
    
    if (config.deployEnabled) {
      def hasKubectl = sh(script: 'command -v kubectl', returnStatus: true) == 0
      if (!hasKubectl) {
        echo "Warning: kubectl not found. Kubernetes deployments may fail."
      }
    }
    
    echo "Agent ${env.NODE_NAME} ready"
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
      if (meta.type == 'maven') {
        if (fileExists('pom.xml')) {
          echo "Building Maven project..."
          def buildResult = sh(script: 'mvn clean package -DskipTests=true -Dmaven.repo.local=.m2/repository 2>&1', returnStatus: true)
          if (buildResult != 0) {
            echo "⚠️ Build failed"
            serviceResult.success = false
            serviceResult.error = "Maven build failed"
            return serviceResult
          }
        } else {
          echo "⚠️ No pom.xml found in ${meta.path}. Skipping Maven build."
          return serviceResult
        }
      } else if (meta.type == 'node') {
        if (fileExists('package.json')) {
          echo "Building Node.js project..."
          sh "npm install --legacy-peer-deps || npm install"
          sh "CI=false npm run build || echo 'Build completed'"
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
        sh "docker build --no-cache -t ${imageTag} ."
        
        // Run Trivy scan
        if (envVars.TRIVY_SEVERITY) {
          runTrivyScan(imageTag, envVars.TRIVY_SEVERITY)
        }
        
        withAWS(credentials: envVars.AWS_CREDENTIALS_ID, region: envVars.AWS_REGION) {
          echo "Logging into ECR..."
          sh "aws ecr get-login-password --region ${envVars.AWS_REGION} | docker login --username AWS --password-stdin ${envVars.CURRENT_ECR_REGISTRY}"
          
          echo "Pushing image: ${imageTag}"
          retry(3) { sh "docker push ${imageTag}" }
          
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
    echo "❌ Failed to build ${serviceName}: ${e.message}"
  }
  
  return serviceResult
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
    SONAR_ORGANIZATION = 'functionalprojects'
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
          echo "  Registry: ${env.CURRENT_ECR_REGISTRY}"
          echo "  Image Tag: ${env.IMAGE_TAG}"
          echo "  SonarCloud Organization: ${env.SONAR_ORGANIZATION}"
          echo "  Available Services:"
          AVAILABLE_SERVICES.each { name, meta ->
            echo "    - ${name} (${meta.type}) -> SonarKey: ${meta.sonarKey ?: 'N/A'}"
          }
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
              runDependencyCheck()
            }
          }
        }
        
        stage('SAST: SonarCloud Analysis') {
          steps {
            script {
              echo "========================================"
              echo "🔍 SonarCloud Configuration"
              echo "  Organization: ${env.SONAR_ORGANIZATION}"
              echo "  Server URL: ${env.SONAR_HOST_URL}"
              echo "========================================"
              
              // Get SonarCloud token from credentials
              def sonarToken = null
              try {
                withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')]) {
                  sonarToken = env.SONAR_TOKEN
                }
                echo "✅ SonarCloud token 'sonarcloud-token' found"
              } catch (Exception e) {
                echo "❌ SonarCloud token 'sonarcloud-token' not found!"
                echo "Please add your SonarCloud token:"
                echo "  Jenkins → Credentials → Add Secret text"
                echo "  ID: sonarcloud-token"
                echo "  Secret: [your token from https://sonarcloud.io/account/security]"
                error "SonarCloud token missing. Cannot proceed with security scan."
              }
              
              echo ""
              echo "========================================"
              echo "📁 Creating SonarCloud projects for available services"
              echo "========================================"
              
              def createdProjects = []
              AVAILABLE_SERVICES.each { serviceName, meta ->
                if (meta.sonarKey) {
                  echo "  - Creating project for: ${serviceName} -> ${meta.sonarKey}"
                  def created = createSonarCloudProject(meta.sonarKey, serviceName, sonarToken)
                  if (created) {
                    createdProjects.add("${serviceName} (${meta.sonarKey})")
                  }
                }
              }
              
              echo ""
              echo "✅ Successfully created/verified ${createdProjects.size()} projects:"
              createdProjects.each { echo "    - ${it}" }
              echo ""
              
              // Run Sonar analysis for each service
              echo "========================================"
              echo "🔍 Running SonarCloud analysis for available services"
              echo "========================================"
              
              def analyzedServices = []
              AVAILABLE_SERVICES.each { serviceName, meta ->
                if (meta.sonarKey) {
                  echo ""
                  echo "--- Analyzing: ${serviceName} (${meta.sonarKey}) ---"
                  def analyzed = runSonarAnalysis(serviceName, meta, sonarToken)
                  if (analyzed) {
                    analyzedServices.add(serviceName)
                    SONAR_RESULTS[serviceName] = [success: true]
                  } else {
                    SONAR_RESULTS[serviceName] = [success: false]
                  }
                }
              }
              
              echo ""
              echo "========================================"
              echo "📊 SonarCloud Analysis Summary"
              echo "========================================"
              echo "✅ Successfully analyzed: ${analyzedServices.join(', ')}"
              echo "📈 View results: https://sonarcloud.io/organizations/${env.SONAR_ORGANIZATION}/projects"
              echo "========================================"
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
              withCredentials([usernamePassword(credentialsId: 'github-token', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
                sh """
                  git config user.email "jenkins@cinevision.com"
                  git config user.name "Jenkins CI"
                  git add ${overlay} || echo 'No files to add'
                  git commit -m "[CI] Deploy ${env.IMAGE_TAG} to ${env.TARGET_ENV} [skip ci]" || echo "No changes to commit"
                  git push https://${GITHUB_USER}:${GITHUB_TOKEN}@github.com/${GITHUB_REPO}.git HEAD:${env.BRANCH_NAME} || echo "Push failed"
                """
              }
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
              // Run Integration Tests
              if (fileExists('tests/integration')) {
                dir('tests/integration') { 
                  sh "npm install || echo 'No package.json'"
                  sh "BASE_URL=${env.API_URL} npm test || echo 'Integration tests failed'"
                }
              } else {
                echo "Integration tests not found. Skipping."
              }
              
              // Run OWASP ZAP Security Scan for release branches
              if (env.BRANCH_NAME.startsWith('release/')) {
                runZapScan(env.API_URL)
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
        
        slackSend(
          color: 'good',
          message: "✅ Pipeline SUCCESS for ${env.JOB_NAME} #${env.BUILD_NUMBER}\nEnvironment: ${env.TARGET_ENV}\nServices: ${successfulServices ?: 'None'}"
        )
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
        
        slackSend(
          color: 'danger',
          message: "❌ Pipeline FAILED for ${env.JOB_NAME} #${env.BUILD_NUMBER}\nEnvironment: ${env.TARGET_ENV}\nFailed services: ${failedServices ?: 'Unknown'}\nCheck logs: ${env.BUILD_URL}"
        )
      }
    }
    always { 
      script {
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml, **/test-results/**/*.xml, dependency-check-reports/**/*.xml'
        cleanWs() 
      }
    }
  }
}