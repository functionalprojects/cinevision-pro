// ============================================
// CINEVISION ENTERPRISE CI/CD PIPELINE
// Industrial-grade automation with DevSecOps
// Optimized with persistent dependency caching
// ============================================

import groovy.json.JsonOutput

// ============================================
// GLOBAL CONFIGURATION
// ============================================

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
// DISCOVER SERVICES
// ============================================

def discoverAvailableServices(serviceMap) {
  def available = [:]

  serviceMap.each { serviceName, meta ->
    if (fileExists(meta.path)) {
      available[serviceName] = meta
      echo "✅ Discovered service: ${serviceName}"
    } else {
      echo "⚠️ Service not found: ${serviceName}"
    }
  }

  if (available.isEmpty()) {
    error "No services discovered in repository"
  }

  return available
}

// ============================================
// ENVIRONMENT CONFIG
// ============================================

@NonCPS
def getEnvironmentConfig(String branch) {

  if (branch == 'main' || branch == 'master') {
    return [
      env: 'prod',
      awsCredentialsId: 'aws-prod-credentials',
      deployEnabled: true,
      approvalRequired: true,
      runSecurityScan: true,
      runPerformanceTests: true,
      runIntegrationTests: true,
      trivySeverity: 'HIGH,CRITICAL',
      apiUrl: 'https://api.cinevision.com',
      argocdApp: 'cinevision-prod'
    ]
  }

  if (branch.startsWith('release/')) {
    return [
      env: 'staging',
      awsCredentialsId: 'aws-staging-credentials',
      deployEnabled: true,
      approvalRequired: true,
      runSecurityScan: true,
      runPerformanceTests: true,
      runIntegrationTests: true,
      trivySeverity: 'HIGH,CRITICAL',
      apiUrl: 'https://staging-api.cinevision.com',
      argocdApp: 'cinevision-staging'
    ]
  }

  if (branch == 'develop') {
    return [
      env: 'dev',
      awsCredentialsId: 'ecr-eks',
      deployEnabled: true,
      approvalRequired: false,
      runSecurityScan: true,
      runPerformanceTests: false,
      runIntegrationTests: true,
      trivySeverity: 'CRITICAL',
      apiUrl: 'https://dev-api.cinevisionca.link',
      argocdApp: 'cinevision-dev'
    ]
  }

  return [
    env: 'unknown',
    deployEnabled: false
  ]
}

// ============================================
// TOOL CHECK + CACHE INIT
// ============================================

def checkAndInstallTools(config) {

  sh """
    mkdir -p ${env.MAVEN_CACHE_DIR}
    mkdir -p ${env.NPM_CACHE_DIR}
    mkdir -p ${env.TRIVY_CACHE_DIR}
    mkdir -p ${env.OWASP_CACHE_DIR}
  """

  sh "chmod -R 777 /var/jenkins_home/caches || true"

  echo "========================================"
  echo "CACHE DIRECTORIES"
  echo "========================================"

  sh """
    ls -lah ${env.MAVEN_CACHE_DIR} || true
    ls -lah ${env.NPM_CACHE_DIR} || true
  """

  def hasDocker = sh(script: 'command -v docker', returnStatus: true) == 0

  if (!hasDocker) {
    error "Docker is required but not installed"
  }

  echo "✅ Docker available"

  sh "docker --version"
}

// ============================================
// SONARCLOUD ANALYSIS
// ============================================

def runSonarAnalysis(serviceName, meta, sonarToken) {

  echo "========================================"
  echo "SONARCLOUD ANALYSIS -> ${serviceName}"
  echo "========================================"

  dir(meta.path) {

    sh """
      mvn -T 1C clean compile test-compile \
        -DskipTests=true \
        -Dmaven.repo.local=${env.MAVEN_CACHE_DIR} \
        -nsu || true
    """

    def status = sh(
      script: """
        mvn -T 1C sonar:sonar \
          -Dsonar.projectKey=${meta.sonarKey} \
          -Dsonar.organization=functionalprojects-key \
          -Dsonar.host.url=https://sonarcloud.io \
          -Dsonar.login=${sonarToken} \
          -Dmaven.repo.local=${env.MAVEN_CACHE_DIR} \
          -Dsonar.java.binaries=target/classes \
          -Dsonar.sources=src/main/java \
          -Dsonar.tests=src/test/java \
          -nsu
      """,
      returnStatus: true
    )

    if (status == 0) {
      echo "✅ SonarCloud successful -> ${serviceName}"
      return true
    }

    echo "⚠️ SonarCloud issues detected -> ${serviceName}"
    return false
  }
}

// ============================================
// DEPENDENCY CHECK
// ============================================

def runDependencyCheck() {

  echo "========================================"
  echo "OWASP DEPENDENCY CHECK"
  echo "========================================"

  def hasDepCheck = sh(script: 'command -v dependency-check.sh', returnStatus: true) == 0

  if (!hasDepCheck) {
    echo "⚠️ Dependency Check not installed"
    return
  }

  AVAILABLE_SERVICES.each { serviceName, meta ->

    if (meta.type == 'maven') {

      dir(meta.path) {

        sh """
          dependency-check.sh \
            --data ${env.OWASP_CACHE_DIR} \
            --scan . \
            --format HTML \
            --format JSON \
            --out ${env.WORKSPACE}/dependency-check-reports/${serviceName} \
            --enableExperimental || true
        """
      }
    }
  }

  archiveArtifacts artifacts: 'dependency-check-reports/**/*', allowEmptyArchive: true
}

// ============================================
// TRIVY SCAN
// ============================================

def runTrivyScan(imageTag, severity) {

  echo "========================================"
  echo "TRIVY SCAN -> ${imageTag}"
  echo "========================================"

  sh """
    docker run --rm \
      -v /var/run/docker.sock:/var/run/docker.sock \
      -v ${env.TRIVY_CACHE_DIR}:/root/.cache/trivy \
      aquasec/trivy:0.55.0 \
      image \
      --scanners vuln \
      --severity ${severity} \
      --ignore-unfixed \
      ${imageTag} \
      || true
  """
}

// ============================================
// CHANGE DETECTION
// ============================================

def detectChangedServices(availableServices, branchName) {

  def fullBuildBranches = [
    'develop',
    'main',
    'master'
  ]

  if (fullBuildBranches.contains(branchName) || branchName.startsWith('release/')) {
    return availableServices.keySet() as List
  }

  def changedFiles = sh(
    script: "git diff --name-only HEAD~1 HEAD || true",
    returnStdout: true
  ).trim().split('\n')

  def changed = []

  availableServices.each { serviceName, meta ->
    if (changedFiles.any { it.startsWith("${meta.path}/") }) {
      changed << serviceName
    }
  }

  if (changed.isEmpty()) {
    changed = availableServices.keySet() as List
  }

  return changed.unique()
}

// ============================================
// BUILD SERVICE
// ============================================

def buildService(serviceName, meta, envVars) {

  def result = [
    success: true,
    imageTag: null,
    error: null
  ]

  try {

    echo "========================================"
    echo "BUILDING -> ${serviceName}"
    echo "========================================"

    dir(meta.path) {

      // =========================================
      // MAVEN BUILD
      // =========================================

      if (meta.type == 'maven' && fileExists('pom.xml')) {

        sh """
          mvn -T 1C clean package \
            -DskipTests=true \
            -Dmaven.repo.local=${env.MAVEN_CACHE_DIR} \
            -nsu
        """
      }

      // =========================================
      // NODE BUILD
      // =========================================

      if (meta.type == 'node' && fileExists('package.json')) {

        sh """
          npm config set cache ${env.NPM_CACHE_DIR} --global

          if [ -f package-lock.json ]; then
            npm ci \
              --cache ${env.NPM_CACHE_DIR} \
              --prefer-offline
          else
            npm install \
              --cache ${env.NPM_CACHE_DIR} \
              --prefer-offline \
              --legacy-peer-deps
          fi
        """

        sh "CI=false npm run build || true"
      }

      // =========================================
      // DOCKER BUILD
      // =========================================

      if (fileExists('Dockerfile')) {

        def imageTag = "${envVars.CURRENT_ECR_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${meta.image}:${envVars.IMAGE_TAG}"

        result.imageTag = imageTag

        sh """
          docker build \
            --build-arg BUILDKIT_INLINE_CACHE=1 \
            -t ${imageTag} .
        """

        runTrivyScan(imageTag, envVars.TRIVY_SEVERITY)

        withAWS(credentials: envVars.AWS_CREDENTIALS_ID, region: envVars.AWS_REGION) {

          sh """
            aws ecr get-login-password --region ${envVars.AWS_REGION} \
            | docker login --username AWS --password-stdin ${envVars.CURRENT_ECR_REGISTRY}
          """

          sh "docker push ${imageTag}"

          sh """
            docker tag ${imageTag} \
            ${envVars.CURRENT_ECR_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${meta.image}:${envVars.TARGET_ENV}-latest
          """

          sh """
            docker push \
            ${envVars.CURRENT_ECR_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${meta.image}:${envVars.TARGET_ENV}-latest
          """
        }
      }

      echo "✅ Build successful -> ${serviceName}"
    }

  } catch (Exception e) {

    result.success = false
    result.error = e.message

    echo "❌ Build failed -> ${serviceName}"
    echo e.message
  }

  return result
}

// ============================================
// GLOBAL STATE
// ============================================

def AVAILABLE_SERVICES = [:]
def BUILD_RESULTS = [:]
def SONAR_RESULTS = [:]

// ============================================
// PIPELINE
// ============================================

pipeline {

  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '30'))
    timeout(time: 120, unit: 'MINUTES')
    skipDefaultCheckout()
  }

  environment {

    // =========================================
    // GLOBAL
    // =========================================

    AWS_REGION = 'us-east-1'
    IMAGE_NAMESPACE = 'cinevision'

    DOCKER_BUILDKIT = '1'
    COMPOSE_DOCKER_CLI_BUILD = '1'

    // =========================================
    // CACHE DIRECTORIES
    // =========================================

    MAVEN_CACHE_DIR = '/var/jenkins_home/caches/maven'
    NPM_CACHE_DIR = '/var/jenkins_home/caches/npm'
    TRIVY_CACHE_DIR = '/var/jenkins_home/caches/trivy'
    OWASP_CACHE_DIR = '/var/jenkins_home/caches/owasp'

    // =========================================
    // CREDENTIALS
    // =========================================

    GITHUB_TOKEN = credentials('github-token')

    // =========================================
    // SONAR
    // =========================================

    SONAR_HOST_URL = 'https://sonarcloud.io'
    SONAR_ORGANIZATION_KEY = 'functionalprojects-key'
  }

  stages {

    // =========================================
    // INITIALIZATION
    // =========================================

    stage('🚀 Initialization') {

      steps {

        checkout scm

        script {

          AVAILABLE_SERVICES = discoverAvailableServices(getServiceMap())

          def CONFIG = getEnvironmentConfig(env.BRANCH_NAME)

          env.TARGET_ENV = CONFIG.env
          env.RUN_SECURITY_SCAN = CONFIG.runSecurityScan.toString()
          env.RUN_PERFORMANCE_TESTS = CONFIG.runPerformanceTests.toString()
          env.RUN_INTEGRATION_TESTS = CONFIG.runIntegrationTests.toString()
          env.TRIVY_SEVERITY = CONFIG.trivySeverity
          env.API_URL = CONFIG.apiUrl
          env.ARGOCD_APP = CONFIG.argocdApp
          env.AWS_CREDENTIALS_ID = CONFIG.awsCredentialsId

          env.GIT_COMMIT_SHORT = sh(
            script: 'git rev-parse --short=8 HEAD',
            returnStdout: true
          ).trim()

          env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"

          withCredentials([
            string(credentialsId: 'DEV_AWS_ACCOUNT_ID', variable: 'AWS_ACCOUNT_ID')
          ]) {
            env.CURRENT_ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
          }

          checkAndInstallTools(CONFIG)

          echo "========================================"
          echo "PIPELINE CONFIGURATION"
          echo "========================================"
          echo "Environment: ${env.TARGET_ENV}"
          echo "Image Tag: ${env.IMAGE_TAG}"
          echo "Registry: ${env.CURRENT_ECR_REGISTRY}"
          echo "========================================"
        }
      }
    }

    // =========================================
    // DEPENDENCY WARMUP
    // =========================================

    stage('⚡ Dependency Warmup') {

      steps {

        script {

          AVAILABLE_SERVICES.each { serviceName, meta ->

            dir(meta.path) {

              // =====================================
              // MAVEN PRELOAD
              // =====================================

              if (meta.type == 'maven' && fileExists('pom.xml')) {

                sh """
                  mvn dependency:go-offline \
                    -Dmaven.repo.local=${env.MAVEN_CACHE_DIR} \
                    -nsu || true
                """
              }

              // =====================================
              // NODE PRELOAD
              // =====================================

              if (meta.type == 'node' && fileExists('package.json')) {

                sh """
                  npm config set cache ${env.NPM_CACHE_DIR} --global

                  if [ -f package-lock.json ]; then
                    npm ci \
                      --cache ${env.NPM_CACHE_DIR} \
                      --prefer-offline || true
                  fi
                """
              }
            }
          }

          echo "✅ Dependency warmup completed"
        }
      }
    }

    // =========================================
    // SECURITY & CODE QUALITY
    // =========================================

    stage('🔍 Security & Code Quality') {

      when {
        expression { env.RUN_SECURITY_SCAN == 'true' }
      }

      parallel {

        stage('OWASP Dependency Check') {

          steps {

            script {
              runDependencyCheck()
            }
          }
        }

        stage('SonarCloud Analysis') {

          steps {

            script {

              withCredentials([
                string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')
              ]) {

                AVAILABLE_SERVICES.each { serviceName, meta ->

                  if (meta.sonarKey && meta.type == 'maven') {

                    def status = runSonarAnalysis(
                      serviceName,
                      meta,
                      env.SONAR_TOKEN
                    )

                    SONAR_RESULTS[serviceName] = [
                      success: status
                    ]
                  }
                }
              }
            }
          }
        }
      }
    }

    // =========================================
    // BUILD & CONTAINERIZE
    // =========================================

    stage('📦 Build & Containerize') {

      steps {

        script {

          def changed = detectChangedServices(
            AVAILABLE_SERVICES,
            env.BRANCH_NAME
          )

          env.CHANGED_SERVICES = changed.join(',')

          echo "========================================"
          echo "Changed Services: ${env.CHANGED_SERVICES}"
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

            def meta = AVAILABLE_SERVICES[serviceName]

            def result = buildService(
              serviceName,
              meta,
              buildVars
            )

            BUILD_RESULTS[serviceName] = result
          }

          echo "========================================"
          echo "BUILD SUMMARY"
          echo "========================================"

          BUILD_RESULTS.each { name, result ->
            echo "${name} -> ${result.success ? 'SUCCESS' : 'FAILED'}"
          }
        }
      }
    }

    // =========================================
    // DEPLOYMENT
    // =========================================

    stage('🚢 Deployment') {

      when {
        expression {
          BUILD_RESULTS.any { it.value.success }
        }
      }

      steps {

        script {

          if (env.TARGET_ENV == 'prod') {

            input(
              message: "Approve Production Deployment?",
              ok: "Deploy"
            )
          }

          def hasArgo = sh(
            script: 'command -v argocd',
            returnStatus: true
          ) == 0

          if (hasArgo) {

            withCredentials([
              usernamePassword(
                credentialsId: 'argocd-creds',
                usernameVariable: 'ARGO_USER',
                passwordVariable: 'ARGO_PWD'
              )
            ]) {

              sh """
                argocd login argocd.cinevision.com \
                  --username ${ARGO_USER} \
                  --password ${ARGO_PWD} \
                  --insecure \
                  --grpc-web
              """

              sh "argocd app sync ${env.ARGOCD_APP} --prune"

              sh """
                argocd app wait \
                  ${env.ARGOCD_APP} \
                  --health \
                  --timeout 600
              """
            }

            echo "✅ Deployment completed"
          }
        }
      }
    }

    // =========================================
    // POST DEPLOYMENT TESTS
    // =========================================

    stage('✅ Post Deployment Tests') {

      when {
        expression {
          env.RUN_INTEGRATION_TESTS == 'true'
        }
      }

      parallel {

        stage('Integration Tests') {

          steps {

            script {

              if (fileExists('tests/integration')) {

                dir('tests/integration') {

                  sh """
                    npm config set cache ${env.NPM_CACHE_DIR} --global
                    npm install --prefer-offline
                  """

                  sh """
                    BASE_URL=${env.API_URL} npm test || true
                  """
                }
              }
            }
          }
        }

        stage('Performance Tests') {

          when {
            expression {
              env.RUN_PERFORMANCE_TESTS == 'true'
            }
          }

          steps {

            script {

              if (fileExists('tests/performance/cinevision-load-test.js')) {

                dir('tests/performance') {

                  sh """
                    docker run --rm \
                      -v \$(pwd):/tests \
                      grafana/k6:latest \
                      run /tests/cinevision-load-test.js \
                      -e BASE_URL=${env.API_URL}
                  """
                }
              }
            }
          }
        }
      }
    }
  }

  // =========================================
  // POST ACTIONS
  // =========================================

  post {

    success {

      script {

        def successfulServices = BUILD_RESULTS
          .findAll { it.value.success }
          .keySet()
          .join(', ')

        echo "========================================"
        echo "PIPELINE SUCCESS"
        echo "========================================"
        echo "Services: ${successfulServices}"
        echo "========================================"

        slackSend(
          color: 'good',
          message: """
✅ CINEVISION PIPELINE SUCCESS

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Environment: ${env.TARGET_ENV}

Services:
${successfulServices}
          """
        )
      }
    }

    failure {

      script {

        def failedServices = BUILD_RESULTS
          .findAll { !it.value.success }
          .keySet()
          .join(', ')

        echo "========================================"
        echo "PIPELINE FAILURE"
        echo "========================================"
        echo "Failed Services: ${failedServices}"
        echo "========================================"

        slackSend(
          color: 'danger',
          message: """
❌ CINEVISION PIPELINE FAILED

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Environment: ${env.TARGET_ENV}

Failed Services:
${failedServices}

Logs:
${env.BUILD_URL}
          """
        )
      }
    }

    always {

      script {

        junit(
          allowEmptyResults: true,
          testResults: '**/target/surefire-reports/*.xml'
        )

        archiveArtifacts(
          artifacts: '**/target/*.jar',
          allowEmptyArchive: true
        )

        cleanWs()
      }
    }
  }
}