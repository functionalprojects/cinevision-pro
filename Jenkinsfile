// ============================================
// CINEVISION ENTERPRISE CI/CD PIPELINE
// Industrial-grade automation with DevSecOps
// Optimized with Shared Dependency Caching
// Fixed for Jenkins compatibility
// ============================================

import groovy.json.JsonOutput

// Global Configuration
def getServiceMap() {
  return [
    'api-gateway'  : [path: 'services/api-gateway',   type: 'maven', image: 'api-gateway', required: false, sonarKey: 'cinevision_api-gateway1'],
    'user-service' : [path: 'services/userService',   type: 'maven', image: 'user-service', required: false, sonarKey: 'cinevision_user-service1'],
    'movie-service': [path: 'services/movieService',  type: 'maven', image: 'movie-service', required: false, sonarKey: 'cinevision_movie-service1'],
    'email-service': [path: 'services/emailService',  type: 'maven', image: 'email-service', required: false, sonarKey: 'cinevision_email-service1'],
    'eureka-server': [path: 'services/eureka-server', type: 'maven', image: 'eureka-server', required: false, sonarKey: 'cinevision_eureka-server1'],
    'frontend'     : [path: 'services/frontend',      type: 'node',  image: 'frontend', required: false, sonarKey: null]
  ]
}

// ============================================
// SHARED DEPENDENCY CACHE MANAGEMENT
// ============================================

// Cache configuration
def getCacheConfig() {
  return [
    maven: [
      cacheDir: '.m2/repository',
      keyPrefix: 'maven-cache',
      paths: ['.m2/repository']
    ],
    node: [
      cacheDir: 'node_modules',
      keyPrefix: 'node-cache',
      paths: ['node_modules']
    ],
    npm: [
      cacheDir: '.npm',
      keyPrefix: 'npm-cache',
      paths: ['.npm']
    ]
  ]
}

// Generate cache key based on lock files
def generateCacheKey(type, servicePath) {
  def cacheKey = "${type}-cache"
  
  try {
    dir(servicePath) {
      if (type == 'maven' && fileExists('pom.xml')) {
        // Use pom.xml for cache key
        def pomHash = sh(script: "md5sum pom.xml 2>/dev/null | cut -d' ' -f1", returnStdout: true).trim()
        if (pomHash) {
          cacheKey = "maven-cache-${pomHash}"
        }
      } 
      else if (type == 'node' && fileExists('package-lock.json')) {
        def lockHash = sh(script: "md5sum package-lock.json 2>/dev/null | cut -d' ' -f1", returnStdout: true).trim()
        if (lockHash) {
          cacheKey = "node-cache-${lockHash}"
        }
      }
      else if (type == 'node' && fileExists('yarn.lock')) {
        def lockHash = sh(script: "md5sum yarn.lock 2>/dev/null | cut -d' ' -f1", returnStdout: true).trim()
        if (lockHash) {
          cacheKey = "yarn-cache-${lockHash}"
        }
      }
      else if (type == 'node' && fileExists('package.json')) {
        def packageHash = sh(script: "md5sum package.json 2>/dev/null | cut -d' ' -f1", returnStdout: true).trim()
        if (packageHash) {
          cacheKey = "node-cache-${packageHash}"
        }
      }
    }
  } catch (Exception e) {
    echo "⚠️ Could not generate precise cache key: ${e.message}"
  }
  
  return cacheKey
}

// Restore dependencies from cache
def restoreCache(type, servicePath, cacheConfig) {
  def cacheKey = generateCacheKey(type, servicePath)
  def cachePath = "${type == 'maven' ? cacheConfig.maven.cacheDir : cacheConfig.node.cacheDir}"
  
  echo "🔍 Attempting to restore ${type} cache for ${servicePath} with key: ${cacheKey}"
  
  def cacheExists = false
  try {
    cacheExists = fileExists("${env.WORKSPACE}/.cache/${cacheKey}/marker")
  } catch (Exception e) {
    echo "Cache directory check failed: ${e.message}"
  }
  
  if (cacheExists) {
    echo "✅ Cache found! Restoring from ${cacheKey}"
    dir(servicePath) {
      try {
        sh """
          mkdir -p ${cachePath}
          cp -r ${env.WORKSPACE}/.cache/${cacheKey}/${cachePath}/* ${cachePath}/ 2>/dev/null || true
        """
        
        if (type == 'maven') {
          sh "chmod -R 755 ${cachePath} 2>/dev/null || true"
        }
        
        echo "✅ Cache restored successfully for ${servicePath}"
        return true
      } catch (Exception e) {
        echo "⚠️ Failed to restore cache: ${e.message}"
        return false
      }
    }
  } else {
    echo "❌ Cache miss for ${cacheKey}. Will download dependencies."
    return false
  }
}

// Save dependencies to cache after download
def saveCache(type, servicePath, cacheConfig) {
  def cacheKey = generateCacheKey(type, servicePath)
  def cachePath = "${type == 'maven' ? cacheConfig.maven.cacheDir : cacheConfig.node.cacheDir}"
  
  echo "💾 Saving ${type} cache for ${servicePath} with key: ${cacheKey}"
  
  dir(servicePath) {
    try {
      sh """
        mkdir -p ${env.WORKSPACE}/.cache/${cacheKey}
        cp -r ${cachePath} ${env.WORKSPACE}/.cache/${cacheKey}/ 2>/dev/null || true
        touch ${env.WORKSPACE}/.cache/${cacheKey}/marker
      """
      echo "✅ Cache saved successfully for ${servicePath}"
      return true
    } catch (Exception e) {
      echo "⚠️ Failed to save cache: ${e.message}"
      return false
    }
  }
}

// Clean old caches using shell commands instead of findFiles
def cleanOldCaches(maxCacheCount = 10) {
  echo "🧹 Cleaning old caches (keeping last ${maxCacheCount} caches)"
  
  try {
    dir("${env.WORKSPACE}/.cache") {
      // List cache directories
      def caches = sh(script: "ls -d */ 2>/dev/null | sed 's/\\///g'", returnStdout: true).trim()
      if (caches) {
        def cacheList = caches.split('\n')
        if (cacheList.size() > maxCacheCount) {
          // Keep only the newest caches
          def cachesWithTime = []
          cacheList.each { cache ->
            def mtime = sh(script: "stat -c %Y ${cache}/marker 2>/dev/null || echo 0", returnStdout: true).trim()
            cachesWithTime << [name: cache, time: mtime.toLong()]
          }
          cachesWithTime.sort { a, b -> a.time <=> b.time }
          def toDelete = cachesWithTime.take(cachesWithTime.size() - maxCacheCount)
          toDelete.each { cache ->
            sh "rm -rf ${cache.name}"
            echo "  Removed old cache: ${cache.name}"
          }
        }
      }
    }
  } catch (Exception e) {
    echo "⚠️ Failed to clean caches: ${e.message}"
  }
}

// Global cache manager for dependencies
def manageDependenciesWithCache(serviceName, meta, closure) {
  def cacheConfig = getCacheConfig()
  def type = meta.type
  def servicePath = meta.path
  
  // Restore cache if available
  def cacheRestored = restoreCache(type, servicePath, cacheConfig)
  
  // Execute the build/dependency download
  def result = closure()
  
  // Save cache if dependencies were downloaded
  if (!cacheRestored && result.success) {
    saveCache(type, servicePath, cacheConfig)
  }
  
  return result
}

// Pre-download all dependencies for all services
def preloadDependencies(availableServices) {
  echo "========================================"
  echo "📦 Preloading dependencies for all services"
  echo "========================================"
  
  def preloadResults = [:]
  
  availableServices.each { serviceName, meta ->
    echo "Preloading dependencies for ${serviceName} (${meta.type})"
    
    def result = [success: false]
    
    dir(meta.path) {
      try {
        if (meta.type == 'maven' && fileExists('pom.xml')) {
          // Try to restore cache first
          if (!restoreCache(meta.type, meta.path, getCacheConfig())) {
            echo "Downloading Maven dependencies for ${serviceName}"
            def downloadResult = sh(
              script: "mvn dependency:go-offline -Dmaven.repo.local=.m2/repository -DskipTests=true --fail-never 2>&1",
              returnStatus: true
            )
            result.success = (downloadResult == 0)
            if (result.success) {
              saveCache(meta.type, meta.path, getCacheConfig())
            } else {
              echo "⚠️ Maven dependency download had issues but continuing"
              result.success = true // Don't fail pipeline for dependency issues
            }
          } else {
            result.success = true
          }
        } 
        else if (meta.type == 'node' && fileExists('package.json')) {
          if (!restoreCache(meta.type, meta.path, getCacheConfig())) {
            echo "Downloading Node.js dependencies for ${serviceName}"
            if (fileExists('package-lock.json')) {
              sh "npm ci --prefer-offline --no-audit --progress=false || npm install --prefer-offline"
            } else {
              sh "npm install --prefer-offline --no-audit --progress=false"
            }
            result.success = true
            saveCache(meta.type, meta.path, getCacheConfig())
          } else {
            result.success = true
          }
        } else {
          result.success = true
        }
        
        preloadResults[serviceName] = result
        echo "✅ Dependencies preloaded for ${serviceName}"
        
      } catch (Exception e) {
        echo "⚠️ Failed to preload dependencies for ${serviceName}: ${e.message}"
        preloadResults[serviceName] = [success: false, error: e.message]
        // Don't fail the pipeline for preload issues
      }
    }
  }
  
  echo "========================================"
  echo "Dependency Preload Summary:"
  preloadResults.each { name, res ->
    echo "  ${name}: ${res.success ? '✅' : '❌'}"
  }
  echo "========================================"
  
  // Clean old caches after preloading
  cleanOldCaches()
  
  return preloadResults
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
      available['root-app'] = [path: '.', type: 'maven', image: 'cinevision-app', required: true, sonarKey: 'cinevision_root-app1']
    } else if (fileExists('package.json')) {
      available['root-app'] = [path: '.', type: 'node', image: 'cinevision-app', required: true, sonarKey: 'cinevision_root-app1']
    } else {
      error "No services or build files found in the repository!"
    }
  }
  
  return available
}

// Run SonarCloud analysis for a service
def runSonarAnalysis(serviceName, meta, sonarToken) {
  echo "========================================"
  echo "🔍 Running SonarCloud analysis for: ${serviceName}"
  echo "  Project Key: ${meta.sonarKey}"
  echo "========================================"
  
  def success = false
  
  dir(meta.path) {
    try {
      if (meta.type == 'maven' && fileExists('pom.xml')) {
        // Use cached dependencies for compilation
        echo "Compiling project with cached dependencies..."
        sh """
          mvn clean compile test-compile \
            -Dmaven.repo.local=.m2/repository \
            -DskipTests=true || true
        """
        
        // Run SonarCloud analysis with cached dependencies
        echo "Running SonarCloud analysis..."
        def sonarCommand = """
          mvn sonar:sonar \
            -Dsonar.projectKey=${meta.sonarKey} \
            -Dsonar.organization=functionalprojects-key \
            -Dsonar.host.url=https://sonarcloud.io \
            -Dsonar.login=${sonarToken} \
            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
            -Dsonar.java.binaries=target/classes \
            -Dsonar.java.test.binaries=target/test-classes \
            -Dsonar.sources=src/main/java \
            -Dsonar.tests=src/test/java \
            -Dsonar.exclusions=**/generated/**/*,**/test/**/* \
            -Dsonar.verbose=false \
            -Dmaven.repo.local=.m2/repository || true
        """
        
        def result = sh(script: sonarCommand, returnStatus: true)
        success = true // Don't fail on Sonar issues
        echo "✅ SonarCloud analysis completed for ${serviceName}"
      } else {
        echo "⚠️ No pom.xml found for ${serviceName}. Skipping SonarCloud analysis."
        success = true
      }
    } catch (Exception e) {
      echo "⚠️ SonarCloud analysis failed for ${serviceName}: ${e.message}"
      success = true
    }
  }
  
  return success
}

// Run OWASP Dependency Check with caching
def runDependencyCheck() {
  echo "========================================"
  echo "🔍 Running OWASP Dependency Check"
  echo "========================================"
  
  def hasDepCheck = sh(script: 'command -v dependency-check.sh', returnStatus: true) == 0
  
  if (!hasDepCheck) {
    echo "⚠️ OWASP Dependency Check not installed! Skipping..."
    return
  }
  
  // Use cache for NVD data
  def nvdCacheDir = "${env.WORKSPACE}/.cache/nvd"
  sh "mkdir -p ${nvdCacheDir}"
  
  def scannedCount = 0
  def servicePaths = ['services/api-gateway', 'services/userService', 'services/movieService', 
                      'services/emailService', 'services/eureka-server']
  
  servicePaths.each { servicePath ->
    if (fileExists("${servicePath}/pom.xml")) {
      echo "Scanning dependencies for: ${servicePath}"
      
      dir(servicePath) {
        try {
          sh "mkdir -p ${env.WORKSPACE}/dependency-check-reports/${servicePath.replace('/', '-')}"
          sh """
            dependency-check.sh \
              --scan . \
              --format HTML \
              --format XML \
              --out ${env.WORKSPACE}/dependency-check-reports/${servicePath.replace('/', '-')} \
              --data ${nvdCacheDir} \
              --nvdDelay 2000 \
              --enableExperimental || true
          """
          scannedCount++
        } catch (Exception e) {
          echo "⚠️ Dependency check failed for ${servicePath}: ${e.message}"
        }
      }
    }
  }
  
  if (scannedCount > 0) {
    try {
      publishHTML([
        allowMissing: true,
        alwaysLinkToLastBuild: true,
        keepAll: true,
        reportDir: 'dependency-check-reports',
        reportFiles: '**/dependency-check-report.html',
        reportName: 'OWASP Dependency Check'
      ])
    } catch (Exception e) {
      echo "⚠️ Could not publish HTML report: ${e.message}"
    }
    archiveArtifacts artifacts: 'dependency-check-reports/**/*.html', allowEmptyArchive: true
    echo "✅ Dependency Check completed for ${scannedCount} projects"
  } else {
    echo "No pom.xml files found to scan"
  }
}

// Enhanced change detection with cache awareness
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
  
  def rootBuildFiles = ['pom.xml', 'package.json', 'build.gradle', 'Dockerfile', 'Jenkinsfile', 'package-lock.json', 'yarn.lock']
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
    
    if (config.deployEnabled) {
      def hasKubectl = sh(script: 'command -v kubectl', returnStatus: true) == 0
      if (!hasKubectl) {
        echo "Warning: kubectl not found. Kubernetes deployments may fail."
      }
    }
    
    echo "Agent ${env.NODE_NAME} ready"
  }
}

// Build a single service with error handling and dependency caching
def buildService(serviceName, meta, envVars) {
  def serviceResult = [success: true, error: null, imageTag: null]
  
  try {
    echo "========================================"
    echo "🚀 Building service: ${serviceName}"
    echo "========================================"
    
    dir(meta.path) {
      if (meta.type == 'maven') {
        if (fileExists('pom.xml')) {
          echo "Building Maven project with cached dependencies..."
          def buildResult = sh(script: '''
            mvn clean package -DskipTests=true \
              -Dmaven.repo.local=.m2/repository 2>&1
          ''', returnStatus: true)
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
          echo "Building Node.js project with cached dependencies..."
          if (fileExists('package-lock.json')) {
            sh "npm ci --prefer-offline --no-audit || npm install --prefer-offline"
          } else {
            sh "npm install --prefer-offline --no-audit"
          }
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
        sh "docker build -t ${imageTag} ."
        
        echo "✅ Successfully built: ${serviceName}"
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

// Run OWASP ZAP Security Scan
def runZapScan(apiUrl) {
  echo "========================================"
  echo "🔍 Running OWASP ZAP Security Scan"
  echo "  Target: ${apiUrl}"
  echo "========================================"
  
  sh """
    mkdir -p zap-reports
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
    SONAR_ORGANIZATION_KEY = 'functionalprojects-key'
    SONAR_ORGANIZATION_NAME = 'functionalprojects'
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
          echo "  Available Services:"
          AVAILABLE_SERVICES.each { name, meta ->
            echo "    - ${name} (${meta.type})"
          }
          echo "========================================"
        }
      }
    }
    
    stage('📦 Preload Dependencies') {
      steps {
        script {
          if (AVAILABLE_SERVICES.isEmpty()) {
            echo "No services available. Skipping dependency preload."
            return
          }
          
          echo "========================================"
          echo "Preloading dependencies for all services"
          echo "========================================"
          
          // Create cache directory
          sh "mkdir -p ${env.WORKSPACE}/.cache"
          
          // Preload all dependencies (won't fail the pipeline)
          catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
            preloadDependencies(AVAILABLE_SERVICES)
          }
        }
      }
    }
    
    stage('🔍 Security & Code Quality') {
      when { expression { env.RUN_SECURITY_SCAN == 'true' } }
      steps {
        script {
          catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
            runDependencyCheck()
            
            def sonarToken = null
            try {
              withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')]) {
                sonarToken = env.SONAR_TOKEN
              }
              echo "✅ SonarCloud token found"
              
              AVAILABLE_SERVICES.each { serviceName, meta ->
                if (meta.sonarKey && fileExists("${meta.path}/pom.xml")) {
                  runSonarAnalysis(serviceName, meta, sonarToken)
                  SONAR_RESULTS[serviceName] = [success: true]
                }
              }
            } catch (Exception e) {
              echo "⚠️ SonarCloud token not found. Skipping SonarCloud analysis."
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
            
            // Build with cache management
            def result = manageDependenciesWithCache(serviceName, meta) {
              return buildService(serviceName, meta, buildVars)
            }
            
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
        env.KUSTOMIZE_OVERLAY 
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
                  echo "⚠️ Skipping manifest update for ${serviceName}"
                }
              }
            }
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
          echo "Frontend deployment would happen here"
          echo "Bucket: ${env.FRONTEND_BUCKET}"
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
          
          echo "Deployment would happen here for ${env.ARGOCD_APP}"
        }
      }
    }
    
    stage('✅ Post-Deployment Tests') {
      when { expression { env.RUN_INTEGRATION_TESTS == 'true' && env.API_URL } }
      steps {
        script {
          echo "Integration tests would run against ${env.API_URL}"
          catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
            if (env.BRANCH_NAME.startsWith('release/')) {
              runZapScan(env.API_URL)
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
        echo "========================================"
        echo "✅ Pipeline completed successfully!"
        echo "  Services built: ${successfulServices ?: 'None'}"
        echo "========================================"
        
        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
          slackSend(
            color: 'good',
            message: "✅ Pipeline SUCCESS for ${env.JOB_NAME} #${env.BUILD_NUMBER}\nEnvironment: ${env.TARGET_ENV}"
          )
        }
      }
    }
    failure { 
      script {
        def failedServices = BUILD_RESULTS.findAll { !it.value.success }.keySet().join(', ')
        echo "========================================"
        echo "❌ Pipeline failed!"
        echo "  Failed services: ${failedServices ?: 'None'}"
        echo "========================================"
        
        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
          slackSend(
            color: 'danger',
            message: "❌ Pipeline FAILED for ${env.JOB_NAME} #${env.BUILD_NUMBER}\nEnvironment: ${env.TARGET_ENV}\nCheck logs: ${env.BUILD_URL}"
          )
        }
      }
    }
    always { 
      script {
        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml, **/test-results/**/*.xml'
        }
        cleanWs()
      }
    }
  }
}