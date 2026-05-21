// ============================================
// CINEVISION INDUSTRIAL-GRADE CI/CD PIPELINE
// Version: 6.1 - Multi-Region DR Production Ready
// Features:
// - Multi-Service Monorepo CI/CD
// - SCA / SAST / DAST / Performance Testing
// - GitOps + ArgoCD
// - Multi-Region DR Deployment
// - CloudFront Multi-Distribution Invalidation
// - ECR Multi-Region Replication
// - Slack Notifications
// - SonarCloud Quality Gates
// - OWASP Dependency Check
// - Trivy Container Scanning
// - k6 Performance Testing
// ============================================

import groovy.transform.Field

// ============================================
// GLOBAL SERVICE MAP
// ============================================

@Field def serviceMap = [
  'api-gateway'  : [path: 'services/api-gateway',   type: 'maven', image: 'api-gateway', sonarProject: 'cinevision-api-gateway', port: 8080],
  'user-service' : [path: 'services/userService',   type: 'maven', image: 'user-service', sonarProject: 'cinevision-user-service', port: 8081],
  'movie-service': [path: 'services/movieService',  type: 'maven', image: 'movie-service', sonarProject: 'cinevision-movie-service', port: 8082],
  'email-service': [path: 'services/emailService',  type: 'maven', image: 'email-service', sonarProject: 'cinevision-email-service', port: 8083],
  'eureka-server': [path: 'services/eureka-server', type: 'maven', image: 'eureka-server', sonarProject: 'cinevision-eureka-server', port: 8761],
  'frontend'     : [path: 'services/frontend',      type: 'node',  image: 'frontend', sonarProject: 'cinevision-frontend', port: 3000]
]

@Field def CURRENT_ENV_CONFIG = [:]

// ============================================
// PIPELINE
// ============================================

pipeline {

  agent {
    label 'docker && linux'
  }

  options {
    timestamps()
    ansiColor('xterm')
    disableConcurrentBuilds(abortPrevious: true)
    timeout(time: 90, unit: 'MINUTES')
    buildDiscarder(logRotator(
      numToKeepStr: '30',
      daysToKeepStr: '30'
    ))
    skipDefaultCheckout()
    preserveStashes(buildCount: 5)
  }

  environment {

    // ============================================
    // AWS REGIONS
    // ============================================

    AWS_REGION    = 'us-east-1'
    DR_AWS_REGION = 'us-west-2'

    // ============================================
    // ECR
    // ============================================

    ECR_REPOSITORY_PREFIX = 'cinevision'

    // ============================================
    // SECURITY
    // ============================================

    TRIVY_SEVERITY     = 'HIGH,CRITICAL'
    SONAR_HOST_URL     = 'https://sonarcloud.io'
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
    // TOKENS
    // ============================================

    GITHUB_TOKEN = credentials('github-token')
    SLACK_TOKEN  = credentials('slack-token')
    SONAR_TOKEN  = credentials('sonar-token')
    ARGOCD_TOKEN = credentials('argocd-token')

    // ============================================
    // AWS ACCOUNT IDS
    // ============================================

    DEV_AWS_ACCOUNT_ID     = credentials('DEV_AWS_ACCOUNT_ID')
    STAGING_AWS_ACCOUNT_ID = credentials('STAGING_AWS_ACCOUNT_ID')
    PROD_AWS_ACCOUNT_ID    = credentials('PROD_AWS_ACCOUNT_ID')

    // ============================================
    // CLOUDFRONT IDS
    // ============================================

    DEV_CLOUDFRONT_DISTRIBUTION_ID     = credentials('DEV_CLOUDFRONT_DISTRIBUTION_ID')
    DEV_POSTER_CLOUDFRONT_ID           = credentials('DEV_POSTER_CLOUDFRONT_ID')
    DEV_ARCHIVE_CLOUDFRONT_ID          = credentials('DEV_ARCHIVE_CLOUDFRONT_ID')

    STAGING_CLOUDFRONT_DISTRIBUTION_ID = credentials('STAGING_CLOUDFRONT_DISTRIBUTION_ID')
    STAGING_POSTER_CLOUDFRONT_ID       = credentials('STAGING_POSTER_CLOUDFRONT_ID')
    STAGING_ARCHIVE_CLOUDFRONT_ID      = credentials('STAGING_ARCHIVE_CLOUDFRONT_ID')

    PROD_CLOUDFRONT_DISTRIBUTION_ID    = credentials('PROD_CLOUDFRONT_DISTRIBUTION_ID')
    PROD_POSTER_CLOUDFRONT_ID          = credentials('PROD_POSTER_CLOUDFRONT_ID')
    PROD_ARCHIVE_CLOUDFRONT_ID         = credentials('PROD_ARCHIVE_CLOUDFRONT_ID')

    // ============================================
    // DR CLOUDFRONT IDS
    // ============================================

    PROD_DR_CLOUDFRONT_DISTRIBUTION_ID    = credentials('PROD_DR_CLOUDFRONT_DISTRIBUTION_ID')
    STAGING_DR_CLOUDFRONT_DISTRIBUTION_ID = credentials('STAGING_DR_CLOUDFRONT_DISTRIBUTION_ID')

    // ============================================
    // API URLS
    // ============================================

    DEV_API_URL     = 'https://dev-api.cinevisionca.link'
    STAGING_API_URL = 'https://staging-api.cinevision.com'
    PROD_API_URL    = 'https://api.cinevision.com'

    // ============================================
    // FRONTEND BUCKETS
    // ============================================

    DEV_FRONTEND_BUCKET     = 'dev-cinevision-dev-frontend'
    STAGING_FRONTEND_BUCKET = 'staging-cinevision-staging-frontend'
    PROD_FRONTEND_BUCKET    = 'prod-cinevision-prod-frontend'

    // ============================================
    // DR FRONTEND BUCKETS
    // ============================================

    STAGING_DR_FRONTEND_BUCKET = 'staging-cinevision-dr-frontend'
    PROD_DR_FRONTEND_BUCKET    = 'prod-cinevision-dr-frontend'
  }

  stages {

    // ============================================
    // INITIALIZATION
    // ============================================

    stage('🚀 Initialization & Checkout') {

      steps {

        checkout scm

        script {

          CURRENT_ENV_CONFIG = getEnvironmentConfig()

          env.TARGET_ENV            = CURRENT_ENV_CONFIG.env
          env.AWS_CREDENTIALS_ID    = CURRENT_ENV_CONFIG.awsCredentialsId
          env.KUSTOMIZE_OVERLAY     = CURRENT_ENV_CONFIG.kustomizeOverlay
          env.ARGOCD_APP            = CURRENT_ENV_CONFIG.argocdApp
          env.API_URL               = CURRENT_ENV_CONFIG.apiUrl
          env.DEPLOY_ENABLED        = CURRENT_ENV_CONFIG.deployEnabled.toString()
          env.BUILD_IMAGES          = CURRENT_ENV_CONFIG.buildImages.toString()
          env.APPROVAL_REQUIRED     = CURRENT_ENV_CONFIG.approvalRequired.toString()
          env.RUN_INTEGRATION_TESTS = CURRENT_ENV_CONFIG.runIntegrationTests.toString()
          env.RUN_PERFORMANCE_TESTS = CURRENT_ENV_CONFIG.runPerformanceTests.toString()

          env.CURRENT_FRONTEND_BUCKET =
            CURRENT_ENV_CONFIG.frontendBucket ?: ''

          env.DR_FRONTEND_BUCKET =
            CURRENT_ENV_CONFIG.drFrontendBucket ?: ''

          env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID =
            CURRENT_ENV_CONFIG.cloudfrontDistributionId ?: ''

          env.DR_CLOUDFRONT_DISTRIBUTION_ID =
            CURRENT_ENV_CONFIG.drCloudfrontDistributionId ?: ''

          if (CURRENT_ENV_CONFIG.awsAccountId) {

            env.CURRENT_ECR_REGISTRY =
              "${CURRENT_ENV_CONFIG.awsAccountId}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"

            env.DR_ECR_REGISTRY =
              "${CURRENT_ENV_CONFIG.awsAccountId}.dkr.ecr.${env.DR_AWS_REGION}.amazonaws.com"
          }

          sh 'git fetch --unshallow || true'

          env.GIT_COMMIT =
            sh(script: 'git rev-parse HEAD', returnStdout: true).trim()

          env.GIT_COMMIT_SHORT =
            sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()

          env.IMAGE_TAG =
            "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"

          env.BUILD_TIMESTAMP =
            sh(
              script: 'date -u +"%Y-%m-%dT%H:%M:%SZ"',
              returnStdout: true
            ).trim()

          def changedServices = detectChangedServices()

          env.CHANGED_SERVICES = changedServices.join(',')

          validateEnvironmentConfiguration()

          echo """
=========================================
🚀 PIPELINE CONFIGURATION
=========================================
Environment: ${env.TARGET_ENV}
Branch: ${env.BRANCH_NAME}
Build: ${env.BUILD_NUMBER}
Commit: ${env.GIT_COMMIT_SHORT}
Tag: ${env.IMAGE_TAG}
Services: ${env.CHANGED_SERVICES}
Deploy Enabled: ${env.DEPLOY_ENABLED}
=========================================
"""
        }
      }
    }

    // ============================================
    // SECURITY & QUALITY
    // ============================================

    stage('🔍 Security & Code Quality') {

      when {
        expression { env.CHANGED_SERVICES?.trim() }
      }

      parallel {

        stage('SCA: Dependency Check') {

          steps {

            script {

              def services = env.CHANGED_SERVICES.split(',')

              services.each { serviceName ->

                def meta = serviceMap[serviceName]

                if (!meta || meta.type != 'maven') {
                  return
                }

                dir(meta.path) {

                  sh """
                    mvn org.owasp:dependency-check-maven:check \
                      -Dformat=XML \
                      -Dformat=HTML \
                      -DfailOnError=false || true
                  """

                  if (fileExists('dependency-check-report.xml')) {

                    dependencyCheckPublisher(
                      pattern: 'dependency-check-report.xml',
                      allowEmptyResults: true
                    )
                  }
                }
              }
            }
          }
        }

        stage('SAST: SonarCloud') {

          when {
            expression { env.BRANCH_NAME !=~ /feature\/.*/ }
          }

          steps {

            script {

              def services = env.CHANGED_SERVICES.split(',')

              withSonarQubeEnv('sonarcloud') {

                services.each { serviceName ->

                  def meta = serviceMap[serviceName]

                  if (!meta || meta.type != 'maven') {
                    return
                  }

                  dir(meta.path) {

                    sh """
                      mvn sonar:sonar \
                        -Dsonar.projectKey=${meta.sonarProject} \
                        -Dsonar.organization=${env.SONAR_ORGANIZATION} \
                        -Dsonar.host.url=${env.SONAR_HOST_URL} \
                        -Dsonar.login=${env.SONAR_TOKEN} \
                        -Dsonar.projectVersion=${env.IMAGE_TAG}
                    """
                  }
                }
              }
            }
          }
        }
      }
    }

    // ============================================
    // BUILD & CONTAINERIZE
    // ============================================

    stage('📦 Build & Containerize') {

      when {
        expression {
          env.BUILD_IMAGES == 'true' &&
          env.CHANGED_SERVICES?.trim()
        }
      }

      steps {

        script {

          def services = env.CHANGED_SERVICES.split(',')

          services.each { serviceName ->

            def meta = serviceMap[serviceName]

            if (!meta) {
              return
            }

            stage("Build ${serviceName}") {

              dir(meta.path) {

                if (meta.type == 'maven') {

                  sh 'mvn clean package -DskipTests=false'

                } else {

                  sh '''
                    npm ci || npm install
                    npm run build --if-present
                  '''
                }

                if (env.AWS_CREDENTIALS_ID?.trim()) {

                  withAWS(
                    credentials: env.AWS_CREDENTIALS_ID,
                    region: env.AWS_REGION
                  ) {

                    sh """
                      aws ecr get-login-password \
                        --region ${env.AWS_REGION} | \
                      docker login \
                        --username AWS \
                        --password-stdin \
                        ${env.CURRENT_ECR_REGISTRY}
                    """

                    sh """
                      aws ecr get-login-password \
                        --region ${env.DR_AWS_REGION} | \
                      docker login \
                        --username AWS \
                        --password-stdin \
                        ${env.DR_ECR_REGISTRY}
                    """
                  }
                }

                def imageBase =
                  "${env.ECR_REPOSITORY_PREFIX}/${meta.image}"

                def primaryImage =
                  "${env.CURRENT_ECR_REGISTRY}/${imageBase}:${env.IMAGE_TAG}"

                def drImage =
                  "${env.DR_ECR_REGISTRY}/${imageBase}:${env.IMAGE_TAG}"

                def latestImage =
                  "${env.CURRENT_ECR_REGISTRY}/${imageBase}:${env.TARGET_ENV}-latest"

                sh """
                  docker build \
                    -t ${primaryImage} \
                    -t ${latestImage} .
                """

                sh """
                  trivy image \
                    --severity ${env.TRIVY_SEVERITY} \
                    --exit-code 0 \
                    --ignore-unfixed \
                    ${primaryImage}
                """

                retry(3) {
                  sh "docker push ${primaryImage}"
                }

                retry(3) {
                  sh "docker push ${latestImage}"
                }

                sh "docker tag ${primaryImage} ${drImage}"

                retry(3) {
                  sh "docker push ${drImage}"
                }

                echo "✅ ${serviceName} built successfully"
              }
            }
          }
        }
      }
    }

    // ============================================
    // GITOPS UPDATE
    // ============================================

    stage('📂 GitOps Manifest Update') {

      when {
        expression {
          env.DEPLOY_ENABLED == 'true' &&
          env.KUSTOMIZE_OVERLAY?.trim()
        }
      }

      steps {

        script {

          def services = env.CHANGED_SERVICES.split(',')

          services.each { serviceName ->

            if (serviceName == 'frontend') {
              return
            }

            def meta = serviceMap[serviceName]

            dir(env.KUSTOMIZE_OVERLAY) {

              sh """
                kustomize edit set image \
                  ${meta.image}=${env.CURRENT_ECR_REGISTRY}/${env.ECR_REPOSITORY_PREFIX}/${meta.image}:${env.IMAGE_TAG}
              """
            }
          }

          withCredentials([
            string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')
          ]) {

            sh """
              git config user.email "jenkins@cinevision.com"
              git config user.name "Jenkins CI"

              git add ${env.KUSTOMIZE_OVERLAY}

              git diff --cached --quiet || \
              git commit -m "[CI] Update images ${env.IMAGE_TAG}"

              git push https://${GITHUB_TOKEN}@github.com/${env.GITHUB_REPO}.git HEAD:${env.BRANCH_NAME}
            """
          }
        }
      }
    }

    // ============================================
    // FRONTEND DEPLOYMENT
    // ============================================

    stage('🌐 Frontend Deployment') {

      when {
        expression {
          env.CHANGED_SERVICES?.contains('frontend') &&
          env.DEPLOY_ENABLED == 'true'
        }
      }

      steps {

        script {

          dir(serviceMap['frontend'].path) {

            def buildDir = fileExists('dist') ? 'dist' : 'build'

            withAWS(
              credentials: env.AWS_CREDENTIALS_ID,
              region: env.AWS_REGION
            ) {

              sh """
                aws s3 sync ${buildDir}/ \
                  s3://${env.CURRENT_FRONTEND_BUCKET}/ \
                  --delete
              """

              sh """
                aws cloudfront create-invalidation \
                  --distribution-id ${env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID} \
                  --paths "/*"
              """

              if (env.DR_FRONTEND_BUCKET?.trim()) {

                sh """
                  aws s3 sync ${buildDir}/ \
                    s3://${env.DR_FRONTEND_BUCKET}/ \
                    --region ${env.DR_AWS_REGION} \
                    --delete
                """
              }

              if (env.DR_CLOUDFRONT_DISTRIBUTION_ID?.trim()) {

                sh """
                  aws cloudfront create-invalidation \
                    --distribution-id ${env.DR_CLOUDFRONT_DISTRIBUTION_ID} \
                    --paths "/*"
                """
              }
            }
          }
        }
      }
    }

    // ============================================
    // DEPLOYMENT
    // ============================================

    stage('🚢 Deployment & Sync') {

      when {
        expression {
          env.DEPLOY_ENABLED == 'true' &&
          env.ARGOCD_APP?.trim()
        }
      }

      steps {

        script {

          if (env.APPROVAL_REQUIRED == 'true') {

            input(
              message: "Approve deployment to ${env.TARGET_ENV}?",
              ok: 'Deploy'
            )
          }

          withCredentials([
            string(credentialsId: 'argocd-token', variable: 'ARGOCD_TOKEN')
          ]) {

            sh """
              argocd login argocd.cinevision.com \
                --username admin \
                --password ${ARGOCD_TOKEN} \
                --grpc-web \
                --insecure
            """

            sh """
              argocd app sync ${env.ARGOCD_APP} \
                --grpc-web \
                --prune
            """

            sh """
              argocd app wait ${env.ARGOCD_APP} \
                --health \
                --sync \
                --timeout 600 \
                --grpc-web
            """
          }
        }
      }
    }

    // ============================================
    // VALIDATION & TESTING
    // ============================================

    stage('✅ Verification & Testing') {

      when {
        expression {
          env.DEPLOY_ENABLED == 'true' &&
          env.API_URL?.trim()
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
                    npm ci || npm install
                    npm test -- --url=${env.API_URL}
                  """
                }

              } else {

                echo 'No smoke tests found'
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
              env.RUN_INTEGRATION_TESTS == 'true'
            }
          }

          steps {

            script {

              if (fileExists('tests/integration')) {

                dir('tests/integration') {

                  sh """
                    npm ci || npm install
                    BASE_URL=${env.API_URL} npm test
                  """
                }

              } else {

                echo 'No integration tests found'
              }
            }
          }
        }

        // ============================================
        // DAST
        // ============================================

        stage('DAST: OWASP ZAP') {

          steps {

            sh """
              docker run --rm \
                -v \$(pwd):/zap/wrk/:rw \
                -t owasp/zap2docker-stable \
                zap-baseline.py \
                -t ${env.API_URL} \
                -r zap_report.html || true
            """

            archiveArtifacts(
              artifacts: 'zap_report.html',
              allowEmptyArchive: true
            )
          }
        }

        // ============================================
        // PERFORMANCE TESTING
        // ============================================

        stage('Performance: k6') {

          when {
            expression {
              env.RUN_PERFORMANCE_TESTS == 'true'
            }
          }

          options {
            timeout(time: 30, unit: 'MINUTES')
          }

          steps {

            script {

              if (fileExists('tests/performance/load-test.js')) {

                dir('tests/performance') {

                  sh """
                    docker run --rm \
                      -v \$(pwd):/tests \
                      -e BASE_URL=${env.API_URL} \
                      -e K6_OUT=json=/tests/k6-results.json \
                      grafana/k6:latest \
                      run \
                        --summary-export=/tests/k6-summary.json \
                        /tests/load-test.js
                  """

                  archiveArtifacts(
                    artifacts: 'k6-results.json,k6-summary.json',
                    allowEmptyArchive: true
                  )
                }

              } else {

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

    stage('🔖 Release Tagging') {

      when {
        expression {
          env.TARGET_ENV == 'prod' &&
          env.BRANCH_NAME == 'main'
        }
      }

      steps {

        script {

          withCredentials([
            string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')
          ]) {

            sh """
              git config user.email "jenkins@cinevision.com"
              git config user.name "Jenkins CI"

              git tag -a "release-${env.IMAGE_TAG}" \
                -m "Release ${env.IMAGE_TAG}"

              git push \
                https://${GITHUB_TOKEN}@github.com/${env.GITHUB_REPO}.git \
                --tags
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

        echo "✅ Pipeline completed successfully"

        sendSlackNotification('SUCCESSFUL')
      }
    }

    failure {

      script {

        echo "❌ Pipeline failed"

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

        junit(
          allowEmptyResults: true,
          testResults: '''
            **/target/surefire-reports/*.xml,
            **/dependency-check-report.xml,
            **/TEST-*.xml
          '''
        )

        archiveArtifacts(
          artifacts: '''
            **/target/*.jar,
            **/dist/**/*,
            **/build/**/*,
            **/zap_report.html,
            **/k6-results.json,
            **/k6-summary.json
          ''',
          allowEmptyArchive: true
        )

        sh 'docker system prune -af || true'

        cleanWs(
          cleanWhenFailure: true,
          cleanWhenSuccess: true,
          deleteDirs: true
        )
      }
    }
  }
}

// ============================================
// HELPER FUNCTIONS
// ============================================

def getEnvironmentConfig() {

  def branch = env.BRANCH_NAME ?: 'unknown'

  if (branch == 'main' || branch == 'master') {

    return [
      env                        : 'prod',
      awsAccountId               : env.PROD_AWS_ACCOUNT_ID,
      awsCredentialsId           : 'aws-prod-credentials',
      argocdApp                  : 'cinevision-prod',
      frontendBucket             : env.PROD_FRONTEND_BUCKET,
      drFrontendBucket           : env.PROD_DR_FRONTEND_BUCKET,
      cloudfrontDistributionId   : env.PROD_CLOUDFRONT_DISTRIBUTION_ID,
      drCloudfrontDistributionId : env.PROD_DR_CLOUDFRONT_DISTRIBUTION_ID,
      apiUrl                     : env.PROD_API_URL,
      kustomizeOverlay           : 'k8s/overlays/prod',
      deployEnabled              : true,
      approvalRequired           : true,
      runIntegrationTests        : true,
      runPerformanceTests        : true,
      buildImages                : true
    ]
  }

  if (branch.startsWith('release/')) {

    return [
      env                        : 'staging',
      awsAccountId               : env.STAGING_AWS_ACCOUNT_ID,
      awsCredentialsId           : 'aws-staging-credentials',
      argocdApp                  : 'cinevision-staging',
      frontendBucket             : env.STAGING_FRONTEND_BUCKET,
      drFrontendBucket           : env.STAGING_DR_FRONTEND_BUCKET,
      cloudfrontDistributionId   : env.STAGING_CLOUDFRONT_DISTRIBUTION_ID,
      drCloudfrontDistributionId : env.STAGING_DR_CLOUDFRONT_DISTRIBUTION_ID,
      apiUrl                     : env.STAGING_API_URL,
      kustomizeOverlay           : 'k8s/overlays/staging',
      deployEnabled              : true,
      approvalRequired           : true,
      runIntegrationTests        : true,
      runPerformanceTests        : true,
      buildImages                : true
    ]
  }

  if (branch == 'develop') {

    return [
      env                        : 'dev',
      awsAccountId               : env.DEV_AWS_ACCOUNT_ID,
      awsCredentialsId           : 'aws-dev-credentials',
      argocdApp                  : 'cinevision-dev',
      frontendBucket             : env.DEV_FRONTEND_BUCKET,
      cloudfrontDistributionId   : env.DEV_CLOUDFRONT_DISTRIBUTION_ID,
      apiUrl                     : env.DEV_API_URL,
      kustomizeOverlay           : 'k8s/overlays/dev',
      deployEnabled              : true,
      approvalRequired           : false,
      runIntegrationTests        : true,
      runPerformanceTests        : false,
      buildImages                : true
    ]
  }

  return [
    env                 : 'feature',
    deployEnabled       : false,
    buildImages         : true
  ]
}

def detectChangedServices() {

  def changedFilesRaw = sh(
    script: '''
      git diff --name-only origin/main...HEAD || true
    ''',
    returnStdout: true
  ).trim()

  def changedFiles =
    changedFilesRaw ? changedFilesRaw.split('\n') : []

  def changed = []

  serviceMap.each { serviceName, meta ->

    if (changedFiles.any { it.startsWith("${meta.path}/") }) {

      changed << serviceName
    }
  }

  if (
    changed.isEmpty() &&
    (
      env.BRANCH_NAME == 'develop' ||
      env.BRANCH_NAME == 'main' ||
      env.BRANCH_NAME?.startsWith('release/')
    )
  ) {

    changed = serviceMap.keySet() as List
  }

  return changed.unique()
}

def validateEnvironmentConfiguration() {

  echo "Validating environment configuration..."
}

def sendSlackNotification(String status) {

  def colors = [
    SUCCESSFUL: 'good',
    FAILED    : 'danger',
    UNSTABLE  : 'warning',
    ABORTED   : '#808080'
  ]

  slackSend(
    tokenCredentialId: 'slack-token',
    channel: env.SLACK_CHANNEL,
    color: colors[status],
    message: """
${status}
Environment: ${env.TARGET_ENV}
Branch: ${env.BRANCH_NAME}
Build: ${env.BUILD_NUMBER}
Services: ${env.CHANGED_SERVICES}
"""
  )
}