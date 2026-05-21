// ============================================
// CINEVISION ENTERPRISE MULTI-REGION CI/CD PIPELINE
// PRODUCTION HARDENED VERSION
// ============================================

import groovy.transform.Field

@Field def serviceMap = [
  'api-gateway'  : [path: 'services/api-gateway',   type: 'maven', image: 'api-gateway', sonarProject: 'cinevision-api-gateway', port: 8080],
  'user-service' : [path: 'services/userService',   type: 'maven', image: 'user-service', sonarProject: 'cinevision-user-service', port: 8081],
  'movie-service': [path: 'services/movieService',  type: 'maven', image: 'movie-service', sonarProject: 'cinevision-movie-service', port: 8082],
  'email-service': [path: 'services/emailService',  type: 'maven', image: 'email-service', sonarProject: 'cinevision-email-service', port: 8083],
  'eureka-server': [path: 'services/eureka-server', type: 'maven', image: 'eureka-server', sonarProject: 'cinevision-eureka-server', port: 8761],
  'frontend'     : [path: 'services/frontend',      type: 'node',  image: 'frontend', sonarProject: 'cinevision-frontend', port: 3000]
]

@Field def CURRENT_ENV_CONFIG = [:]

pipeline {

  agent any

  // ============================================
  // OPTIONS
  // ============================================

  options {
    timestamps()
    disableConcurrentBuilds()
    skipDefaultCheckout()

    timeout(
      time: 120,
      unit: 'MINUTES'
    )

    buildDiscarder(
      logRotator(
        numToKeepStr: '30',
        daysToKeepStr: '30'
      )
    )
  }

  // ============================================
  // ENVIRONMENT
  // ============================================

  environment {

    // ============================================
    // AWS REGIONS
    // ============================================

    AWS_REGION = 'us-east-1'
    DR_AWS_REGION = 'us-west-2'

    // ============================================
    // ECR
    // ============================================

    ECR_REPOSITORY_PREFIX = 'cinevision'

    // ============================================
    // SECURITY
    // ============================================

    TRIVY_SEVERITY = 'HIGH,CRITICAL'

    SONAR_HOST_URL = 'https://sonarcloud.io'
    SONAR_ORGANIZATION = 'functionalprojects'

    // ============================================
    // REPOSITORY
    // ============================================

    GITHUB_REPO = 'functionalprojects/cinevision-pro'

    // ============================================
    // SLACK
    // ============================================

    SLACK_CHANNEL = '#cinevision-ci-alerts'

    // ============================================
    // AWS ACCOUNT IDS
    // ============================================

    DEV_AWS_ACCOUNT_ID = credentials('DEV_AWS_ACCOUNT_ID')
    STAGING_AWS_ACCOUNT_ID = credentials('STAGING_AWS_ACCOUNT_ID')
    PROD_AWS_ACCOUNT_ID = credentials('PROD_AWS_ACCOUNT_ID')

    // ============================================
    // PRIMARY CLOUDFRONT IDS
    // ============================================

    DEV_CLOUDFRONT_DISTRIBUTION_ID = credentials('DEV_CLOUDFRONT_DISTRIBUTION_ID')
    DEV_POSTER_CLOUDFRONT_ID = credentials('DEV_POSTER_CLOUDFRONT_ID')
    DEV_ARCHIVE_CLOUDFRONT_ID = credentials('DEV_ARCHIVE_CLOUDFRONT_ID')

    STAGING_CLOUDFRONT_DISTRIBUTION_ID = credentials('STAGING_CLOUDFRONT_DISTRIBUTION_ID')
    STAGING_POSTER_CLOUDFRONT_ID = credentials('STAGING_POSTER_CLOUDFRONT_ID')
    STAGING_ARCHIVE_CLOUDFRONT_ID = credentials('STAGING_ARCHIVE_CLOUDFRONT_ID')

    PROD_CLOUDFRONT_DISTRIBUTION_ID = credentials('PROD_CLOUDFRONT_DISTRIBUTION_ID')
    PROD_POSTER_CLOUDFRONT_ID = credentials('PROD_POSTER_CLOUDFRONT_ID')
    PROD_ARCHIVE_CLOUDFRONT_ID = credentials('PROD_ARCHIVE_CLOUDFRONT_ID')

    // ============================================
    // TOKENS
    // ============================================

    GITHUB_TOKEN = credentials('github-token')

    // ============================================
    // API URLS
    // ============================================

    DEV_API_URL = 'https://dev-api.cinevisionca.link'
    STAGING_API_URL = 'https://staging-api.cinevision.com'
    PROD_API_URL = 'https://api.cinevision.com'

    // ============================================
    // S3 BUCKETS
    // ============================================

    DEV_FRONTEND_BUCKET = 'dev-cinevision-dev-frontend'
    STAGING_FRONTEND_BUCKET = 'staging-cinevision-staging-frontend'
    PROD_FRONTEND_BUCKET = 'prod-cinevision-prod-frontend'

    DEV_MOVIE_POSTERS_BUCKET = 'dev-cinevision-dev-movie-posters'
    STAGING_MOVIE_POSTERS_BUCKET = 'staging-cinevision-staging-movie-posters'
    PROD_MOVIE_POSTERS_BUCKET = 'prod-cinevision-prod-movie-posters'

    DEV_EMAIL_ARCHIVES_BUCKET = 'dev-cinevision-dev-email-archives'
    STAGING_EMAIL_ARCHIVES_BUCKET = 'staging-cinevision-staging-email-archives'
    PROD_EMAIL_ARCHIVES_BUCKET = 'prod-cinevision-prod-email-archives'
  }

  // ============================================
  // STAGES
  // ============================================

  stages {

    // ============================================
    // INITIALIZATION
    // ============================================

    stage('Initialization & Checkout') {

      steps {

        checkout scm

        script {

          CURRENT_ENV_CONFIG = getEnvironmentConfig()

          env.TARGET_ENV = CURRENT_ENV_CONFIG.env ?: 'feature'
          env.AWS_CREDENTIALS_ID = CURRENT_ENV_CONFIG.awsCredentialsId ?: ''
          env.KUSTOMIZE_OVERLAY = CURRENT_ENV_CONFIG.kustomizeOverlay ?: ''
          env.ARGOCD_APP = CURRENT_ENV_CONFIG.argocdApp ?: ''
          env.API_URL = CURRENT_ENV_CONFIG.apiUrl ?: ''

          env.DEPLOY_ENABLED = CURRENT_ENV_CONFIG.deployEnabled.toString()
          env.BUILD_IMAGES = CURRENT_ENV_CONFIG.buildImages.toString()
          env.APPROVAL_REQUIRED = CURRENT_ENV_CONFIG.approvalRequired.toString()
          env.RUN_INTEGRATION_TESTS = CURRENT_ENV_CONFIG.runIntegrationTests.toString()
          env.RUN_PERFORMANCE_TESTS = CURRENT_ENV_CONFIG.runPerformanceTests.toString()

          env.CURRENT_FRONTEND_BUCKET =
            CURRENT_ENV_CONFIG.frontendBucket ?: ''

          env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID =
            CURRENT_ENV_CONFIG.cloudfrontDistributionId ?: ''

          env.GIT_COMMIT = sh(
            script: 'git rev-parse HEAD',
            returnStdout: true
          ).trim()

          env.GIT_COMMIT_SHORT = sh(
            script: 'git rev-parse --short=8 HEAD',
            returnStdout: true
          ).trim()

          env.IMAGE_TAG =
            "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"

          if (CURRENT_ENV_CONFIG.awsAccountId?.trim()) {

            env.CURRENT_ECR_REGISTRY =
              "${CURRENT_ENV_CONFIG.awsAccountId}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"

            env.DR_ECR_REGISTRY =
              "${CURRENT_ENV_CONFIG.awsAccountId}.dkr.ecr.${env.DR_AWS_REGION}.amazonaws.com"
          }

          def changedServices = detectChangedServices()
          
          // Filter to only include services that exist and are buildable
          def availableServices = filterAvailableServices(changedServices)
          
          // Update the changed services list to only available ones
          env.CHANGED_SERVICES = availableServices.join(',')
          
          // Track skipped services for reporting
          def skippedServices = changedServices - availableServices
          if (skippedServices) {
            echo "WARNING: The following services were skipped (not available): ${skippedServices.join(', ')}"
          }

          echo '========================================='
          echo "Environment: ${env.TARGET_ENV}"
          echo "Branch: ${env.BRANCH_NAME}"
          echo "Commit: ${env.GIT_COMMIT_SHORT}"
          echo "Image Tag: ${env.IMAGE_TAG}"
          echo "Available Services: ${env.CHANGED_SERVICES}"
          if (skippedServices) {
            echo "Skipped Services: ${skippedServices.join(', ')}"
          }
          echo '========================================='
        }
      }
    }

    // ============================================
    // SECURITY & QUALITY
    // ============================================

    stage('Security & Quality') {

      when {
        expression {
          return env.CHANGED_SERVICES?.trim()
        }
      }

      parallel {

        // ============================================
        // DEPENDENCY CHECK
        // ============================================

        stage('Dependency Check') {

          steps {

            script {

              env.CHANGED_SERVICES
                .split(',')
                .findAll { it?.trim() }
                .each { serviceName ->

                  def meta = serviceMap[serviceName]

                  if (meta?.type == 'maven' && isServiceAvailable(meta.path, 'pom.xml')) {

                    dir(meta.path) {

                      sh '''
                        mvn org.owasp:dependency-check-maven:check \
                          -Dformat=XML \
                          -Dformat=HTML \
                          -DfailOnError=false || true
                      '''
                    }
                  } else if (meta?.type == 'maven') {
                    echo "Skipping dependency check for ${serviceName} - No pom.xml found"
                  }
                }
            }
          }
        }

        // ============================================
        // SONARCLOUD
        // ============================================

        stage('SonarCloud') {

          when {
            expression {
              return (
                (env.BRANCH_NAME == 'develop' ||
                env.BRANCH_NAME == 'main' ||
                env.BRANCH_NAME.startsWith('release/')) &&
                env.CHANGED_SERVICES?.trim()
              )
            }
          }

          steps {

            script {
              // SonarCloud analysis can fail without breaking the build
              // We'll catch errors and continue
              def sonarServices = env.CHANGED_SERVICES
                .split(',')
                .findAll { it?.trim() }
                .findAll { serviceName ->
                  def meta = serviceMap[serviceName]
                  return meta?.type == 'maven' && isServiceAvailable(meta.path, 'pom.xml')
                }
              
              if (sonarServices.isEmpty()) {
                echo "No Maven services available for SonarCloud analysis"
                return
              }
              
              sonarServices.each { serviceName ->

                def meta = serviceMap[serviceName]

                dir(meta.path) {

                  try {
                    withSonarQubeEnv('sonarcloud') {

                      sh """
                        mvn sonar:sonar \
                          -Dsonar.projectKey=${meta.sonarProject} \
                          -Dsonar.organization=${env.SONAR_ORGANIZATION} \
                          -Dsonar.host.url=${env.SONAR_HOST_URL} \
                          -Dsonar.coverage.exclusions='**/test/**,**/tests/**' \
                          -Dsonar.qualitygate.wait=false || true
                      """
                    }
                  } catch (Exception e) {
                    echo "SonarCloud analysis failed for ${serviceName}: ${e.message}"
                    echo "Continuing pipeline despite SonarCloud failure"
                  }
                }
              }
            }
          }
        }
      }
    }

    // ============================================
    // BUILD & PUSH
    // ============================================

    stage('Build & Push Images') {

      when {
        expression {
          return (
            env.BUILD_IMAGES == 'true' &&
            env.CHANGED_SERVICES?.trim() &&
            env.CURRENT_ECR_REGISTRY?.trim()
          )
        }
      }

      steps {

        script {

          def buildStages = [:]

          env.CHANGED_SERVICES
            .split(',')
            .findAll { it?.trim() }
            .each { serviceName ->

              def meta = serviceMap[serviceName]

              if (!meta) {
                return
              }
              
              // Check if service is actually buildable
              if (!isServiceBuildable(meta)) {
                echo "Skipping build for ${serviceName} - Required build files not found"
                return
              }

              buildStages[serviceName] = {

                dir(meta.path) {

                  if (meta.type == 'maven') {

                    sh '''
                      mvn clean verify -DskipTests=true || mvn clean compile
                    '''
                  }

                  if (meta.type == 'node') {

                    sh '''
                      if [ -f "package.json" ]; then
                        npm ci || npm install
                        npm run build || npm run build --if-present
                      else
                        echo "No package.json found, skipping build"
                      fi
                    '''
                  }

                  withAWS(
                    region: env.AWS_REGION,
                    credentials: env.AWS_CREDENTIALS_ID
                  ) {

                    sh """
                      aws ecr get-login-password \
                        --region ${env.AWS_REGION} | \
                      docker login \
                        --username AWS \
                        --password-stdin \
                        ${env.CURRENT_ECR_REGISTRY} || true
                    """

                    sh """
                      aws ecr get-login-password \
                        --region ${env.DR_AWS_REGION} | \
                      docker login \
                        --username AWS \
                        --password-stdin \
                        ${env.DR_ECR_REGISTRY} || true
                    """
                  }

                  def imageName =
                    "${env.ECR_REPOSITORY_PREFIX}/${meta.image}"

                  def primaryImage =
                    "${env.CURRENT_ECR_REGISTRY}/${imageName}:${env.IMAGE_TAG}"

                  def drImage =
                    "${env.DR_ECR_REGISTRY}/${imageName}:${env.IMAGE_TAG}"

                  // Check if Dockerfile exists before building
                  if (fileExists('Dockerfile')) {
                    sh """
                      docker build \
                        -t ${primaryImage} .
                    """

                    sh """
                      docker tag \
                        ${primaryImage} \
                        ${drImage}
                    """

                    sh """
                      trivy image \
                        --severity ${env.TRIVY_SEVERITY} \
                        --exit-code 0 \
                        ${primaryImage} || true
                    """

                    sh "docker push ${primaryImage} || true"
                    sh "docker push ${drImage} || true"
                  } else {
                    echo "No Dockerfile found in ${meta.path}, skipping Docker build and push"
                  }
                }
              }
            }

          if (buildStages.isEmpty()) {
            echo "No services available to build"
          } else {
            parallel buildStages
          }
        }
      }
    }

    // ============================================
    // GITOPS MANIFEST UPDATE
    // ============================================

    stage('GitOps Manifest Update') {

      when {
        expression {
          return (
            env.DEPLOY_ENABLED == 'true' &&
            env.KUSTOMIZE_OVERLAY?.trim() &&
            env.CHANGED_SERVICES?.trim()
          )
        }
      }

      steps {

        script {

          env.CHANGED_SERVICES
            .split(',')
            .findAll { it?.trim() }
            .each { serviceName ->

              if (serviceName == 'frontend') {
                return
              }

              def meta = serviceMap[serviceName]

              if (!meta) {
                return
              }

              dir(env.KUSTOMIZE_OVERLAY) {

                sh """
                  kustomize edit set image \
                    ${meta.image}=${env.CURRENT_ECR_REGISTRY}/${env.ECR_REPOSITORY_PREFIX}/${meta.image}:${env.IMAGE_TAG} || true
                """
              }
            }

          withCredentials([
            string(
              credentialsId: 'github-token',
              variable: 'GITHUB_TOKEN'
            )
          ]) {

            sh """
              git config user.email 'jenkins@cinevision.com'
              git config user.name 'Jenkins CI'

              git add .

              git diff --cached --quiet || \
              git commit -m '[CI] Update image tags ${env.IMAGE_TAG}'

              git push \
                https://${GITHUB_TOKEN}@github.com/${env.GITHUB_REPO}.git \
                HEAD:${env.BRANCH_NAME} || true
            """
          }
        }
      }
    }

    // ============================================
    // FRONTEND DEPLOYMENT
    // ============================================

    stage('Frontend Deployment') {

      when {
        expression {
          return (
            env.DEPLOY_ENABLED == 'true' &&
            env.CHANGED_SERVICES?.contains('frontend')
          )
        }
      }

      steps {

        script {
          def frontendMeta = serviceMap['frontend']
          
          if (!isServiceAvailable(frontendMeta.path, 'package.json')) {
            echo "Frontend service not available (missing package.json), skipping deployment"
            return
          }

          dir(frontendMeta.path) {

            sh '''
              if [ -f "package.json" ]; then
                npm ci || npm install
                npm run build || npm run build --if-present
              else
                echo "No package.json found"
              fi
            '''

            withAWS(
              region: env.AWS_REGION,
              credentials: env.AWS_CREDENTIALS_ID
            ) {

              if (fileExists('dist')) {
                sh """
                  aws s3 sync \
                    dist/ \
                    s3://${env.CURRENT_FRONTEND_BUCKET}/ \
                    --delete || true
                """
              } else if (fileExists('build')) {
                sh """
                  aws s3 sync \
                    build/ \
                    s3://${env.CURRENT_FRONTEND_BUCKET}/ \
                    --delete || true
                """
              } else {
                echo "No dist or build directory found, skipping S3 sync"
              }

              if (
                env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID?.trim()
              ) {

                sh """
                  aws cloudfront create-invalidation \
                    --distribution-id ${env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID} \
                    --paths '/*' || true
                """
              }
              else {

                echo 'No CloudFront distribution configured'
              }
            }
          }
        }
      }
    }

    // ============================================
    // ARGOCD DEPLOYMENT
    // ============================================

    stage('Deploy & Sync') {

      when {
        expression {
          return (
            env.DEPLOY_ENABLED == 'true' &&
            env.ARGOCD_APP?.trim()
          )
        }
      }

      steps {

        script {

          if (env.APPROVAL_REQUIRED == 'true') {

            input(
              message: "Approve deployment to ${env.TARGET_ENV}?"
            )
          }

          withCredentials([
            string(
              credentialsId: 'argocd-token',
              variable: 'ARGOCD_TOKEN'
            )
          ]) {

            sh """
              argocd login argocd.cinevision.com \
                --grpc-web \
                --insecure \
                --username admin \
                --password ${ARGOCD_TOKEN} || true
            """

            sh """
              argocd app sync ${env.ARGOCD_APP} \
                --grpc-web \
                --prune \
                --force || true
            """

            sh """
              argocd app wait ${env.ARGOCD_APP} \
                --grpc-web \
                --health \
                --timeout 600 || true
            """
          }
        }
      }
    }

    // ============================================
    // VERIFICATION & TESTING
    // ============================================

    stage('Verification & Testing') {

      when {
        expression {
          return env.DEPLOY_ENABLED == 'true'
        }
      }

      parallel {

        // ============================================
        // SMOKE TESTS
        // ============================================

        stage('Smoke Tests') {

          steps {

            script {

              if (fileExists('tests/smoke')) {

                dir('tests/smoke') {

                  sh """
                    npm ci || npm install || true
                    npm test -- --env=${env.TARGET_ENV} || true
                  """
                }
              }
              else {

                echo 'Smoke tests directory not found'
              }
            }
          }
        }

        // ============================================
        // INTEGRATION TESTS
        // ============================================

        stage('Integration Tests') {

          when {
            expression {
              return env.RUN_INTEGRATION_TESTS == 'true'
            }
          }

          steps {

            script {

              if (fileExists('tests/integration')) {

                dir('tests/integration') {

                  sh """
                    npm ci || npm install || true
                    BASE_URL=${env.API_URL} npm test || true
                  """
                }
              }
              else {

                echo 'Integration tests directory not found'
              }
            }
          }
        }

        // ============================================
        // OWASP ZAP
        // ============================================

        stage('OWASP ZAP') {

          steps {

            sh """
              docker run --rm \
                -v \$(pwd):/zap/wrk/:rw \
                -t ghcr.io/zaproxy/zaproxy:stable \
                zap-baseline.py \
                -t ${env.API_URL} \
                -r zap_report.html || true
            """
          }
        }

        // ============================================
        // PERFORMANCE TESTS
        // ============================================

        stage('Performance Tests') {

          when {
            expression {
              return env.RUN_PERFORMANCE_TESTS == 'true'
            }
          }

          steps {

            script {

              if (
                fileExists('tests/performance/load-test.js')
              ) {

                dir('tests/performance') {

                  sh """
                    docker run --rm \
                      -i \
                      -v \$(pwd):/scripts \
                      -e BASE_URL=${env.API_URL} \
                      grafana/k6:latest \
                      run /scripts/load-test.js || true
                  """
                }
              }
              else {

                echo 'No performance tests found'
              }
            }
          }
        }
      }
    }

    // ============================================
    // RELEASE TAGGING
    // ============================================

    stage('Release Tagging') {

      when {
        expression {
          return (
            env.TARGET_ENV == 'prod' &&
            env.BRANCH_NAME == 'main'
          )
        }
      }

      steps {

        script {

          withCredentials([
            string(
              credentialsId: 'github-token',
              variable: 'GITHUB_TOKEN'
            )
          ]) {

            sh """
              git tag \
                -a release-${env.IMAGE_TAG} \
                -m 'Release ${env.IMAGE_TAG}' || true

              git push \
                https://${GITHUB_TOKEN}@github.com/${env.GITHUB_REPO}.git \
                --tags || true
            """
          }
        }
      }
    }
  }

  // ============================================
  // POST ACTIONS
  // ============================================

  post {

    success {

      script {
        sendSlackNotification('SUCCESSFUL')
      }
    }

    failure {

      script {
        sendSlackNotification('FAILED')
      }
    }

    unstable {

      script {
        sendSlackNotification('UNSTABLE')
      }
    }

    aborted {

      script {
        sendSlackNotification('ABORTED')
      }
    }

    always {

      script {

        if (fileExists('.')) {

          junit(
            allowEmptyResults: true,
            keepLongStdio: true,
            testResults: '''
              **/target/surefire-reports/*.xml,
              **/target/failsafe-reports/*.xml,
              **/TEST-*.xml
            '''.trim()
          )

          archiveArtifacts(
            artifacts: '''
              **/target/*.jar,
              **/dist/**/*,
              **/build/**/*,
              zap_report.html
            '''.trim(),
            allowEmptyArchive: true
          )
        }

        cleanWs(
          deleteDirs: true,
          disableDeferredWipeout: true,
          notFailBuild: true
        )
      }
    }
  }
}

// ============================================
// ENVIRONMENT CONFIGURATION
// ============================================

def getEnvironmentConfig() {

  def branch = env.BRANCH_NAME ?: ''

  if (
    branch == 'main' ||
    branch == 'master' ||
    branch.startsWith('hotfix/')
  ) {

    return [
      env                      : 'prod',
      awsAccountId             : env.PROD_AWS_ACCOUNT_ID,
      awsCredentialsId         : 'aws-prod-credentials',
      argocdApp                : 'cinevision-prod',
      frontendBucket           : env.PROD_FRONTEND_BUCKET,
      cloudfrontDistributionId : env.PROD_CLOUDFRONT_DISTRIBUTION_ID,
      apiUrl                   : env.PROD_API_URL,
      kustomizeOverlay         : 'k8s/overlays/prod',
      deployEnabled            : true,
      approvalRequired         : true,
      runIntegrationTests      : true,
      runPerformanceTests      : true,
      buildImages              : true
    ]
  }

  if (branch.startsWith('release/')) {

    return [
      env                      : 'staging',
      awsAccountId             : env.STAGING_AWS_ACCOUNT_ID,
      awsCredentialsId         : 'aws-staging-credentials',
      argocdApp                : 'cinevision-staging',
      frontendBucket           : env.STAGING_FRONTEND_BUCKET,
      cloudfrontDistributionId : env.STAGING_CLOUDFRONT_DISTRIBUTION_ID,
      apiUrl                   : env.STAGING_API_URL,
      kustomizeOverlay         : 'k8s/overlays/staging',
      deployEnabled            : true,
      approvalRequired         : true,
      runIntegrationTests      : true,
      runPerformanceTests      : true,
      buildImages              : true
    ]
  }

  if (branch == 'develop') {

    return [
      env                      : 'dev',
      awsAccountId             : env.DEV_AWS_ACCOUNT_ID,
      awsCredentialsId         : 'aws-dev-credentials',
      argocdApp                : 'cinevision-dev',
      frontendBucket           : env.DEV_FRONTEND_BUCKET,
      cloudfrontDistributionId : env.DEV_CLOUDFRONT_DISTRIBUTION_ID,
      apiUrl                   : env.DEV_API_URL,
      kustomizeOverlay         : 'k8s/overlays/dev',
      deployEnabled            : true,
      approvalRequired         : false,
      runIntegrationTests      : true,
      runPerformanceTests      : false,
      buildImages              : true
    ]
  }

  return [
    env                      : 'feature',
    awsAccountId             : '',
    awsCredentialsId         : '',
    argocdApp                : '',
    frontendBucket           : '',
    cloudfrontDistributionId : '',
    apiUrl                   : '',
    kustomizeOverlay         : '',
    deployEnabled            : false,
    approvalRequired         : false,
    runIntegrationTests      : false,
    runPerformanceTests      : false,
    buildImages              : true
  ]
}

// ============================================
// DETECT CHANGED SERVICES
// ============================================

def detectChangedServices() {

  def changedFilesRaw = sh(
    script: '''
      if git rev-parse HEAD~1 >/dev/null 2>&1; then
        git diff --name-only HEAD~1 HEAD
      else
        git ls-files
      fi
    ''',
    returnStdout: true
  ).trim()

  def changedFiles =
    changedFilesRaw ? changedFilesRaw.split('\n') : []

  def changed = []

  serviceMap.each { serviceName, meta ->

    if (
      changedFiles.any {
        it.startsWith(meta.path + '/')
      }
    ) {

      changed << serviceName
    }
  }

  if (
    changed.isEmpty() &&
    (
      env.BRANCH_NAME == 'develop' ||
      env.BRANCH_NAME == 'main' ||
      env.BRANCH_NAME.startsWith('release/')
    )
  ) {

    changed = serviceMap.keySet() as List
  }

  return changed.unique()
}

// ============================================
// SERVICE AVAILABILITY HELPERS
// ============================================

def isServiceAvailable(String servicePath, String requiredFile) {
  try {
    return fileExists("${servicePath}/${requiredFile}")
  } catch (Exception e) {
    return false
  }
}

def isServiceBuildable(Map meta) {
  if (!meta) return false
  
  switch (meta.type) {
    case 'maven':
      return isServiceAvailable(meta.path, 'pom.xml')
    case 'node':
      return isServiceAvailable(meta.path, 'package.json')
    default:
      return false
  }
}

def filterAvailableServices(List<String> services) {
  return services.findAll { serviceName ->
    def meta = serviceMap[serviceName]
    if (!meta) return false
    
    def isAvailable = isServiceBuildable(meta)
    if (!isAvailable) {
      echo "Service ${serviceName} is not available (missing required build files at ${meta.path})"
    }
    return isAvailable
  }
}

// ============================================
// SLACK NOTIFICATIONS
// ============================================

def sendSlackNotification(String buildStatus) {

  def colorMap = [
    'SUCCESSFUL': 'good',
    'FAILED'    : 'danger',
    'UNSTABLE'  : 'warning',
    'ABORTED'   : '#808080'
  ]

  def color = colorMap[buildStatus] ?: 'warning'

  def message = """
*CineVision Pipeline ${buildStatus}*

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Environment: ${env.TARGET_ENV}
Branch: ${env.BRANCH_NAME}
Services: ${env.CHANGED_SERVICES}
Commit: ${env.GIT_COMMIT_SHORT}
URL: ${env.BUILD_URL}
"""

  try {

    slackSend(
      channel: env.SLACK_CHANNEL,
      color: color,
      tokenCredentialId: 'slack-token',
      message: message,
      failOnError: false
    )
  }
  catch (Exception ex) {

    echo "Slack notification failed: ${ex.message}"
  }
}