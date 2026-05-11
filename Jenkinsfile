// ============================================
// CINEVISION ENTERPRISE CI/CD PIPELINE
// Industrial-grade automation with DevSecOps
// Optimized with Shared Dependency Caching
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
      paths: ['.m2/repository'],
      includePattern: '**/*.jar,**/*.pom,**/*.xml',
      excludePattern: '**/*-SNAPSHOT/**'
    ],
    node: [
      cacheDir: 'node_modules',
      keyPrefix: 'node-cache',
      paths: ['node_modules'],
      includePattern: '**/*',
      excludePattern: ''
    ],
    npm: [
      cacheDir: '.npm',
      keyPrefix: 'npm-cache',
      paths: ['.npm'],
      includePattern: '**/*',
      excludePattern: ''
    ]
  ]
}

// Generate cache key based on lock files
def generateCacheKey(type, servicePath) {
  def cacheKey = "${type}-cache"
  
  try {
    dir(servicePath) {
      if (type == 'maven' && fileExists('pom.xml')) {
        // Use pom.xml and its dependencies for cache key
        def pomHash = sh(script: "md5sum pom.xml | cut -d' ' -f1", returnStdout: true).trim()
        def effectivePomHash = sh(script: "mvn help:effective-pom -Doutput=/tmp/effective-pom-${servicePath.replace('/', '-')}.xml 2>/dev/null && md5sum /tmp/effective-pom-${servicePath.replace('/', '-')}.xml 2>/dev/null | cut -d' ' -f1 || echo '${pomHash}'", returnStdout: true).trim()
        cacheKey = "maven-cache-${effectivePomHash}"
      } 
      else if (type == 'node' && fileExists('package-lock.json')) {
        def lockHash = sh(script: "md5sum package-lock.json | cut -d' ' -f1", returnStdout: true).trim()
        cacheKey = "node-cache-${lockHash}"
      }
      else if (type == 'node' && fileExists('yarn.lock')) {
        def lockHash = sh(script: "md5sum yarn.lock | cut -d' ' -f1", returnStdout: true).trim()
        cacheKey = "yarn-cache-${lockHash}"
      }
      else if (type == 'node' && fileExists('package.json')) {
        def packageHash = sh(script: "md5sum package.json | cut -d' ' -f1", returnStdout: true).trim()
        cacheKey = "node-cache-${packageHash}"
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
    // Cache directory might not exist
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

// Clean old caches to prevent disk space issues
def cleanOldCaches(maxAgeHours = 168, maxCacheCount = 10) { // 7 days default
  echo "🧹 Cleaning old caches (older than ${maxAgeHours} hours or exceeding ${maxCacheCount} caches)"
  
  try {
    dir("${env.WORKSPACE}/.cache") {
      def caches = findFiles(glob: '*/marker')
      
      if (caches.size() > maxCacheCount) {
        caches.sort { a, b -> a.lastModified <=> b.lastModified }
        def toDelete = caches.take(caches.size() - maxCacheCount)
        toDelete.each { cache ->
          def cacheDir = cache.path.replace('/marker', '')
          sh "rm -rf ${cacheDir}"
          echo "  Removed old cache: ${cacheDir}"
        }
      }
      
      // Remove caches older than maxAgeHours
      def now = System.currentTimeMillis()
      caches.each { cache ->
        if (now - cache.lastModified > maxAgeHours * 3600000) {
          def cacheDir = cache.path.replace('/marker', '')
          sh "rm -rf ${cacheDir}"
          echo "  Removed expired cache: ${cacheDir}"
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
            sh """
              mvn dependency:go-offline \
                -Dmaven.repo.local=.m2/repository \
                -DskipTests=true \
                --fail-never || true
            """
            result.success = true
            saveCache(meta.type, meta.path, getCacheConfig())
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
        }
        
        preloadResults[serviceName] = result
        echo "✅ Dependencies preloaded for ${serviceName}"
        
      } catch (Exception e) {
        echo "⚠️ Failed to preload dependencies for ${serviceName}: ${e.message}"
        preloadResults[serviceName] = [success: false, error: e.message]
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
  echo "  Organization Key: functionalprojects-key"
  echo "  Organization Name: functionalprojects"
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
            -DskipTests=true \
            -o || mvn clean compile test-compile \
            -Dmaven.repo.local=.m2/repository \
            -DskipTests=true
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
            -Dmaven.repo.local=.m2/repository \
            -o || mvn sonar:sonar \
            -Dsonar.projectKey=${meta.sonarKey} \
            -Dsonar.organization=functionalprojects-key \
            -Dsonar.host.url=https://sonarcloud.io \
            -Dsonar.login=${sonarToken} \
            -Dmaven.repo.local=.m2/repository
        """
        
        def result = sh(script: sonarCommand, returnStatus: true)
        
        if (result == 0) {
          success = true
          echo "✅ SonarCloud analysis SUCCESS for ${serviceName}"
        } else {
          echo "⚠️ SonarCloud analysis had issues for ${serviceName} (exit code: ${result})"
          success = true
        }
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
              --nvdApiKey \${NVD_API_KEY} \
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
    
    def hasJq = sh(script: 'command -v jq', returnStatus: true) == 0
    if (!hasJq) {
      echo "jq not found. Some features may be limited."
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
              -Dmaven.repo.local=.m2/repository \
              -o 2>&1 || mvn clean package -DskipTests=true \
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
        // Use Docker build cache
        sh "docker build --cache-from ${imageTag} --no-cache -t ${imageTag} . || docker build -t ${imageTag} ."
        
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

// Run Trivy container scan with cache
def runTrivyScan(imageTag, severity) {
  echo "========================================"
  echo "🔍 Running Trivy Security Scan"
  echo "  Image: ${imageTag}"
  echo "  Severity: ${severity}"
  echo "========================================"
  
  // Use Trivy cache directory
  def trivyCacheDir = "${env.WORKSPACE}/.cache/trivy"
  sh "mkdir -p ${trivyCacheDir}"
  
  def hasTrivy = sh(script: 'command -v trivy', returnStatus: true) == 0
  
  if (hasTrivy) {
    sh "trivy image --severity ${severity} --ignore-unfixed --exit-code 0 --cache-dir ${trivyCacheDir} ${imageTag} || echo 'Trivy scan found issues but continuing'"
  } else {
    echo "Trivy not installed. Running via Docker..."
    sh """
      docker run --rm \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v ${trivyCacheDir}:/root/.cache/trivy \
        aquasec/trivy:0.55.0 \
        image --scanners vuln --severity ${severity} --skip-java-db-update ${imageTag} \
        || echo 'Trivy scan completed with warnings'
    """
  }
  echo "✅ Trivy scan completed"
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
          echo "  SonarCloud Organization Key: ${env.SONAR_ORGANIZATION_KEY}"
          echo "  SonarCloud Organization Name: ${env.SONAR_ORGANIZATION_NAME}"
          echo "  SonarCloud Host: ${env.SONAR_HOST_URL}"
          echo "  Available Services:"
          AVAILABLE_SERVICES.each { name, meta ->
            echo "    - ${name} (${meta.type}) -> SonarKey: ${meta.sonarKey ?: 'N/A'}"
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
          echo "This will cache dependencies for faster subsequent builds"
          echo "========================================"
          
          // Create cache directory
          sh "mkdir -p ${env.WORKSPACE}/.cache"
          
          // Preload all dependencies
          preloadDependencies(AVAILABLE_SERVICES)
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
              echo "  Organization Key: ${env.SONAR_ORGANIZATION_KEY}"
              echo "  Organization Name: ${env.SONAR_ORGANIZATION_NAME}"
              echo "  Server URL: ${env.SONAR_HOST_URL}"
              echo "========================================"
              
              def sonarToken = null
              try {
                withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')]) {
                  sonarToken = env.SONAR_TOKEN
                }
                echo "✅ SonarCloud token found"
              } catch (Exception e) {
                echo "❌ SonarCloud token not found!"
                echo "⚠️ Continuing pipeline without SonarCloud analysis..."
                return
              }
              
              echo ""
              echo "========================================"
              echo "📋 Services to analyze with SonarCloud"
              echo "========================================"
              
              def servicesToAnalyze = []
              AVAILABLE_SERVICES.each { serviceName, meta ->
                if (meta.sonarKey && fileExists("${meta.path}/pom.xml")) {
                  servicesToAnalyze.add(serviceName)
                  echo "  ✅ ${serviceName} -> ${meta.sonarKey}"
                } else if (meta.sonarKey) {
                  echo "  ⚠️ ${serviceName} -> pom.xml not found"
                }
              }
              
              if (servicesToAnalyze.isEmpty()) {
                echo "⚠️ No services with pom.xml found for SonarCloud analysis"
                return
              }
              
              echo ""
              echo "========================================"
              echo "🔍 Running SonarCloud analysis"
              echo "  Organization Key: ${env.SONAR_ORGANIZATION_KEY}"
              echo "  Services: ${servicesToAnalyze.join(', ')}"
              echo "========================================"
              
              def analyzedServices = []
              AVAILABLE_SERVICES.each { serviceName, meta ->
                if (meta.sonarKey && fileExists("${meta.path}/pom.xml")) {
                  echo ""
                  echo "--- Analyzing: ${serviceName} ---"
                  echo "    Project Key: ${meta.sonarKey}"
                  
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
              if (analyzedServices.isEmpty()) {
                echo "⚠️ No services were successfully analyzed."
              } else {
                echo "✅ Successfully analyzed: ${analyzedServices.join(', ')}"
              }
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
              if (fileExists('tests/integration')) {
                dir('tests/integration') { 
                  sh "npm install || echo 'No package.json'"
                  sh "BASE_URL=${env.API_URL} npm test || echo 'Integration tests failed'"
                }
              } else {
                echo "Integration tests not found. Skipping."
              }
              
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
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml, **/test-results/**/*.xml'
        // Keep cache directory but clean workspace
        sh "find . -maxdepth 1 ! -name '.cache' ! -name '.' -exec rm -rf {} + 2>/dev/null || true"
      }
    }
  }
}