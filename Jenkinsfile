// ============================================
// CINEVISION INDUSTRIAL-GRADE CI/CD PIPELINE
// Features: SCA, SAST, DAST, GitOps, Slack Rich Notifications
// ============================================

def serviceMap = [
  'api-gateway'  : [path: 'services/api-gateway',   type: 'maven', image: 'api-gateway', sonarProject: 'cinevision-api-gateway'],
  'user-service' : [path: 'services/userService',   type: 'maven', image: 'user-service', sonarProject: 'cinevision-user-service'],
  'movie-service': [path: 'services/movieService',  type: 'maven', image: 'movie-service', sonarProject: 'cinevision-movie-service'],
  'email-service': [path: 'services/emailService',  type: 'maven', image: 'email-service', sonarProject: 'cinevision-email-service'],
  'eureka-server': [path: 'services/eureka-server', type: 'maven', image: 'eureka-server', sonarProject: 'cinevision-eureka-server'],
  'frontend'     : [path: 'services/frontend',      type: 'node',  image: 'frontend', sonarProject: 'cinevision-frontend']
]

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

  def changedFiles = changedFilesRaw ?
    changedFilesRaw.split('\n') as List : []

  def changed = []

  serviceMap.each { serviceName, meta ->
    if (changedFiles.any { it.startsWith("${meta.path}/") }) {
      changed << serviceName
    }
  }

  if (changed.isEmpty()) {
    echo "No specific service changes detected - building all services"
    changed = serviceMap.keySet() as List
  }

  return changed.unique()
}

def getEnvironmentConfig() {

  def branch = env.BRANCH_NAME

  // Production
  if (branch == 'main' || branch == 'master' || (branch != null && branch.startsWith('hotfix/'))) {
    return [
      env                     : 'prod',
      awsAccountId            : env.PROD_AWS_ACCOUNT_ID,
      awsCredentialsId        : 'aws-prod-credentials',
      namespace               : 'cinevision-prod',
      argocdApp               : 'cinevision-prod-green',
      frontendBucket          : env.PROD_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.PROD_CLOUDFRONT_DISTRIBUTION_ID,
      posterBucket            : env.PROD_MOVIE_POSTERS_BUCKET,
      posterCloudfrontId      : env.PROD_POSTER_CLOUDFRONT_ID,
      archiveBucket           : env.PROD_EMAIL_ARCHIVES_BUCKET,
      archiveCloudfrontId     : env.PROD_ARCHIVE_CLOUDFRONT_ID,
      apiUrl                  : env.PROD_API_URL,
      kustomizeOverlay        : 'k8s/overlays/prod/green',
      deployEnabled           : true,
      approvalRequired        : true,
      runSecurityScan         : true,
      runPerformanceTests     : true,
      runIntegrationTests     : true,
      buildImages             : true
    ]
  }

  // Staging
  if (branch != null && branch.startsWith('release/')) {
    return [
      env                     : 'staging',
      awsAccountId            : env.STAGING_AWS_ACCOUNT_ID,
      awsCredentialsId        : 'aws-staging-credentials',
      namespace               : 'cinevision-staging',
      argocdApp               : 'cinevision-staging',
      frontendBucket          : env.STAGING_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.STAGING_CLOUDFRONT_DISTRIBUTION_ID,
      posterBucket            : env.STAGING_MOVIE_POSTERS_BUCKET,
      posterCloudfrontId      : env.STAGING_POSTER_CLOUDFRONT_ID,
      archiveBucket           : env.STAGING_EMAIL_ARCHIVES_BUCKET,
      archiveCloudfrontId     : env.STAGING_ARCHIVE_CLOUDFRONT_ID,
      apiUrl                  : env.STAGING_API_URL,
      kustomizeOverlay        : 'k8s/overlays/staging',
      deployEnabled           : true,
      approvalRequired        : true,
      runSecurityScan         : true,
      runPerformanceTests     : true,
      runIntegrationTests     : true,
      buildImages             : true
    ]
  }

  // Development
  if (branch == 'develop') {
    return [
      env                     : 'dev',
      awsAccountId            : env.DEV_AWS_ACCOUNT_ID,
      awsCredentialsId        : 'ecr-eks',
      namespace               : 'cinevision-dev',
      argocdApp               : 'cinevision-dev',
      frontendBucket          : env.DEV_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.DEV_CLOUDFRONT_DISTRIBUTION_ID,
      posterBucket            : env.DEV_MOVIE_POSTERS_BUCKET,
      posterCloudfrontId      : env.DEV_POSTER_CLOUDFRONT_ID,
      archiveBucket           : env.DEV_EMAIL_ARCHIVES_BUCKET,
      archiveCloudfrontId     : env.DEV_ARCHIVE_CLOUDFRONT_ID,
      apiUrl                  : env.DEV_API_URL,
      kustomizeOverlay        : 'k8s/overlays/dev',
      deployEnabled           : true,
      approvalRequired        : false,
      runSecurityScan         : true,
      runPerformanceTests     : false,
      runIntegrationTests     : true,
      buildImages             : true
    ]
  }

  return [env: 'unknown', deployEnabled: false]
}

pipeline {

  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '30'))
    timeout(time: 90, unit: 'MINUTES')
  }

  environment {

    AWS_REGION = 'us-east-1'
    DR_AWS_REGION = 'us-west-2'

    // Account IDs
    DEV_AWS_ACCOUNT_ID     = credentials('DEV_AWS_ACCOUNT_ID')
    STAGING_AWS_ACCOUNT_ID = credentials('STAGING_AWS_ACCOUNT_ID')
    PROD_AWS_ACCOUNT_ID    = credentials('PROD_AWS_ACCOUNT_ID')

    // Image namespace
    IMAGE_NAMESPACE = 'cinevision'
    TRIVY_SEVERITY  = 'HIGH,CRITICAL'

    // API URLs
    DEV_API_URL      = 'https://dev-api.cinevisionca.link'
    STAGING_API_URL  = 'https://staging-api.cinevision.com'
    PROD_API_URL     = 'https://api.cinevision.com'

    // Bucket Names
    DEV_FRONTEND_BUCKET      = 'dev-cinevision-dev-frontend'
    STAGING_FRONTEND_BUCKET  = 'staging-cinevision-staging-frontend'
    PROD_FRONTEND_BUCKET     = 'prod-cinevision-prod-frontend'

    DEV_MOVIE_POSTERS_BUCKET      = 'dev-cinevision-dev-movie-posters'
    STAGING_MOVIE_POSTERS_BUCKET  = 'staging-cinevision-staging-movie-posters'
    PROD_MOVIE_POSTERS_BUCKET     = 'prod-cinevision-prod-movie-posters'

    DEV_EMAIL_ARCHIVES_BUCKET     = 'dev-cinevision-dev-email-archives'
    STAGING_EMAIL_ARCHIVES_BUCKET = 'staging-cinevision-staging-email-archives'
    PROD_EMAIL_ARCHIVES_BUCKET    = 'prod-cinevision-prod-email-archives'

    // CloudFront distribution IDs
    DEV_CLOUDFRONT_DISTRIBUTION_ID = credentials('DEV_CLOUDFRONT_DISTRIBUTION_ID')
    DEV_POSTER_CLOUDFRONT_ID       = credentials('DEV_POSTER_CLOUDFRONT_ID')
    DEV_ARCHIVE_CLOUDFRONT_ID      = credentials('DEV_ARCHIVE_CLOUDFRONT_ID')

    STAGING_CLOUDFRONT_DISTRIBUTION_ID = credentials('STAGING_CLOUDFRONT_DISTRIBUTION_ID')
    STAGING_POSTER_CLOUDFRONT_ID       = credentials('STAGING_POSTER_CLOUDFRONT_ID')
    STAGING_ARCHIVE_CLOUDFRONT_ID      = credentials('STAGING_ARCHIVE_CLOUDFRONT_ID')

    PROD_CLOUDFRONT_DISTRIBUTION_ID = credentials('PROD_CLOUDFRONT_DISTRIBUTION_ID')
    PROD_POSTER_CLOUDFRONT_ID       = credentials('PROD_POSTER_CLOUDFRONT_ID')
    PROD_ARCHIVE_CLOUDFRONT_ID      = credentials('PROD_ARCHIVE_CLOUDFRONT_ID')

    // Git
    GITHUB_REPO = 'functionalprojects/cinevision-pro'
    GITHUB_TOKEN = credentials('github-token')

    // Slack
    SLACK_TOKEN = credentials('slack-token')
    SLACK_CHANNEL = '#cinevision-ci-alerts'
    
    // SonarCloud
    SONAR_HOST_URL = 'https://sonarcloud.io'
    SONAR_ORGANIZATION = 'functionalprojects'
  }

  stages {

    stage('🚀 Initialization') {

      steps {

        script {

          def config = getEnvironmentConfig()

          env.TARGET_ENV = config.env ?: 'unknown'

          env.AWS_CREDENTIALS_ID = config.awsCredentialsId ?: ''
          env.KUSTOMIZE_OVERLAY = config.kustomizeOverlay ?: ''
          env.ARGOCD_APP = config.argocdApp ?: ''
          env.API_URL = config.apiUrl ?: ''

          env.DEPLOY_ENABLED = (config.deployEnabled ?: false).toString()
          env.BUILD_IMAGES = (config.buildImages ?: false).toString()
          env.APPROVAL_REQUIRED = (config.approvalRequired ?: false).toString()
          env.RUN_INTEGRATION_TESTS = (config.runIntegrationTests ?: false).toString()
          env.RUN_PERFORMANCE_TESTS = (config.runPerformanceTests ?: false).toString()

          env.CURRENT_ECR_REGISTRY =
            "${config.awsAccountId}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"

          env.GIT_COMMIT =
            sh(script: 'git rev-parse HEAD', returnStdout: true).trim()

          env.GIT_COMMIT_SHORT =
            sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()

          env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"

          env.CURRENT_API_URL = config.apiUrl ?: ''
          env.CURRENT_FRONTEND_BUCKET = config.frontendBucket ?: ''

          env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID =
            config.cloudfrontDistributionId ?: ''

          env.CURRENT_POSTER_BUCKET = config.posterBucket ?: ''

          env.CURRENT_POSTER_CLOUDFRONT_ID =
            config.posterCloudfrontId ?: ''

          env.CURRENT_ARCHIVE_BUCKET = config.archiveBucket ?: ''

          env.CURRENT_ARCHIVE_CLOUDFRONT_ID =
            config.archiveCloudfrontId ?: ''

          env.CHANGED_SERVICES = ''

          if (['dev', 'staging', 'prod'].contains(env.TARGET_ENV)) {

            def distId = env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID

            if (!distId || distId == '****') {
              error("${env.TARGET_ENV.toUpperCase()}_CLOUDFRONT_DISTRIBUTION_ID is not properly configured")
            }
          }

          echo "🚀 Pipeline initialized for ${env.TARGET_ENV}"
        }
      }
    }

    stage('🔍 Security & Code Quality') {

      parallel {

        stage('SCA: Dependency Check') {

          steps {
            script {
              // Run dependency check only on changed services or all if no changes detected
              def servicesToScan = detectChangedServices()
              
              servicesToScan.each { serviceName ->
                def meta = serviceMap[serviceName]
                dir(meta.path) {
                  dependencyCheck(
                    additionalArguments: '--format HTML --format XML --out .',
                    odcInstallation: 'DP-Check'
                  )
                  
                  dependencyCheckPublisher(
                    pattern: 'dependency-check-report.xml'
                  )
                }
              }
            }
          }
        }

        stage('SAST: SonarCloud') {

          steps {
            script {
              def servicesToScan = detectChangedServices()
              def sonarBranches = [:]
              
              servicesToScan.each { serviceName ->
                def meta = serviceMap[serviceName]
                
                sonarBranches[serviceName] = {
                  dir(meta.path) {
                    withSonarQubeEnv('sonarcloud') {
                      def sonarCommand = """
                        mvn sonar:sonar \
                          -Dsonar.projectKey=${meta.sonarProject} \
                          -Dsonar.organization=${env.SONAR_ORGANIZATION} \
                          -Dsonar.host.url=${env.SONAR_HOST_URL} \
                          -Dsonar.branch.name=${env.BRANCH_NAME} \
                          -Dsonar.projectVersion=${env.IMAGE_TAG}
                      """
                      sh sonarCommand
                    }
                  }
                }
              }
              
              parallel sonarBranches
              
              // Wait for quality gates for changed services
              servicesToScan.each { serviceName ->
                def meta = serviceMap[serviceName]
                timeout(time: 10, unit: 'MINUTES') {
                  waitForQualityGate abortPipeline: true
                }
              }
            }
          }
        }
      }
    }

    stage('📦 Build & Containerize') {

      when {
        expression { env.BUILD_IMAGES == 'true' }
      }

      steps {

        script {

          def changed = detectChangedServices()

          env.CHANGED_SERVICES = changed.join(',')

          def branches = [:]

          changed.each { serviceName ->

            def meta = serviceMap[serviceName]

            branches[serviceName] = {

              stage("Process ${serviceName}") {

                dir(meta.path) {

                  // Build
                  if (meta.type == 'maven') {
                    sh 'mvn clean package -DskipTests=false'
                  } else {
                    sh 'npm ci && npm test'
                  }

                  // Docker
                  def fullImageName =
                    "${env.IMAGE_NAMESPACE}/${meta.image}"

                  def imageTag =
                    "${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${env.IMAGE_TAG}"

                  sh "docker build -t ${imageTag} ."

                  sh """
                    trivy image \
                    --severity ${env.TRIVY_SEVERITY} \
                    --exit-code 1 \
                    ${imageTag}
                  """

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

                    sh "docker push ${imageTag}"

                    sh """
                      docker tag ${imageTag} \
                      ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${env.TARGET_ENV}-latest
                    """

                    sh """
                      docker push \
                      ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${env.TARGET_ENV}-latest
                    """
                  }
                }
              }
            }
          }

          parallel branches
        }
      }
    }

    stage('📂 GitOps Manifest Update') {

      when {
        expression {
          env.DEPLOY_ENABLED == 'true' &&
          env.CHANGED_SERVICES != null &&
          env.CHANGED_SERVICES != ''
        }
      }

      steps {

        script {

          def overlay = env.KUSTOMIZE_OVERLAY

          def changedServicesList =
            env.CHANGED_SERVICES ?
            env.CHANGED_SERVICES.split(',') : []

          changedServicesList.each { serviceName ->

            if (serviceName != 'frontend' &&
                serviceMap.containsKey(serviceName)) {

              def meta = serviceMap[serviceName]

              def fullImageName =
                "${env.CURRENT_ECR_REGISTRY}/${env.IMAGE_NAMESPACE}/${meta.image}"

              sh """
                kustomize edit set image \
                ${meta.image}=${fullImageName}:${env.IMAGE_TAG} \
                -k ${overlay}
              """
            }
          }

          sh """
            git config user.email "jenkins@cinevision.com"
            git config user.name "Jenkins CI"

            git add ${overlay}

            git commit -m "[CI] Deploy ${env.IMAGE_TAG} to ${env.TARGET_ENV}" || echo "No changes"

            git push origin HEAD:${env.BRANCH_NAME}
          """
        }
      }
    }

    stage('🌐 Frontend Deployment') {

      when {
        expression {
          env.DEPLOY_ENABLED == 'true' &&
          env.CHANGED_SERVICES != null &&
          env.CHANGED_SERVICES.contains('frontend')
        }
      }

      steps {

        script {

          withAWS(
            credentials: env.AWS_CREDENTIALS_ID,
            region: env.AWS_REGION
          ) {

            dir(serviceMap['frontend'].path) {

              sh 'npm ci'
              sh 'npm run build'

              sh """
                aws s3 sync dist \
                s3://${env.CURRENT_FRONTEND_BUCKET} \
                --delete \
                --exact-timestamps
              """

              // Frontend
              if (env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID &&
                  env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID != '****') {

                sh """
                  aws cloudfront create-invalidation \
                  --distribution-id ${env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID} \
                  --paths '/*'
                """
              }

              // Posters
              if (env.CHANGED_SERVICES.contains('movie-service') &&
                  env.CURRENT_POSTER_CLOUDFRONT_ID &&
                  env.CURRENT_POSTER_CLOUDFRONT_ID != '****') {

                sh """
                  aws cloudfront create-invalidation \
                  --distribution-id ${env.CURRENT_POSTER_CLOUDFRONT_ID} \
                  --paths '/*'
                """
              }

              // Archives
              if (env.CHANGED_SERVICES.contains('email-service') &&
                  env.CURRENT_ARCHIVE_CLOUDFRONT_ID &&
                  env.CURRENT_ARCHIVE_CLOUDFRONT_ID != '****') {

                sh """
                  aws cloudfront create-invalidation \
                  --distribution-id ${env.CURRENT_ARCHIVE_CLOUDFRONT_ID} \
                  --paths '/*'
                """
              }
            }
          }
        }
      }
    }

    stage('🚢 Deployment & Sync') {

      when {
        expression { env.DEPLOY_ENABLED == 'true' }
      }

      steps {

        script {

          if (env.APPROVAL_REQUIRED == 'true') {

            input(
              message: "Approve deployment to ${env.TARGET_ENV}?",
              ok: "Deploy"
            )
          }

          sh "argocd app sync ${env.ARGOCD_APP} --grpc-web --prune"

          sh """
            argocd app wait \
            ${env.ARGOCD_APP} \
            --health \
            --timeout 600
          """

          if (env.TARGET_ENV == 'prod') {

            stage('🚥 Canary Analysis') {

              echo "🚀 Starting Canary Deployment (10% Traffic)"

              try {

                sh """
                  python3 scripts/canary-analysis.py \
                  --url ${env.API_URL} \
                  --duration 60 \
                  --interval 10
                """

                echo "✅ Canary Analysis Passed"

              } catch (Exception e) {

                echo "❌ Canary Analysis Failed! Rolling back..."

                sh "kubectl apply -k k8s/overlays/prod/blue"

                error("Canary Analysis Failed: ${e.message}")
              }
            }

            echo "💎 Performing Full Blue-Green Traffic Shift"

            sh "kubectl apply -k k8s/overlays/prod/green"

            sh """
              kubectl rollout status deployment \
              -l app=cinevision \
              --timeout=5m
            """
          }
        }
      }
    }

    stage('✅ Verification & DAST') {

      when {
        expression { env.DEPLOY_ENABLED == 'true' }
      }

      parallel {

        stage('Smoke & Integration') {

          steps {

            script {

              if (fileExists('tests/smoke')) {

                dir('tests/smoke') {
                  sh "npm ci && npm test -- --env=${env.TARGET_ENV}"
                }
              }

              if (env.RUN_INTEGRATION_TESTS == 'true') {

                dir('tests/integration') {

                  sh """
                    npm install &&
                    BASE_URL=${env.API_URL} npm test
                  """
                }
              }
            }
          }
        }

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

        stage('Performance: k6') {

          when {
            expression { env.RUN_PERFORMANCE_TESTS == 'true' }
          }

          steps {

            dir('tests/performance') {

              sh """
                docker run --rm \
                -v \$(pwd):/tests \
                -t grafana/k6 \
                run /tests/cinevision-load-test.js \
                -e BASE_URL=${env.API_URL}
              """
            }
          }
        }
      }
    }

    stage('🔖 Release Tagging') {

      when {
        expression { env.TARGET_ENV == 'prod' }
      }

      steps {

        script {

          sh """
            git tag -a release-${env.IMAGE_TAG} \
            -m "Release ${env.IMAGE_TAG}"

            git push \
            https://${env.GITHUB_TOKEN}@github.com/${env.GITHUB_REPO}.git \
            release-${env.IMAGE_TAG}
          """
        }
      }
    }
  }

  post {

    success {

      script {
        sendSlackNotification(this, 'SUCCESSFUL')
      }
    }

    failure {

      script {
        sendSlackNotification(this, 'FAILED')
      }
    }

    always {

      script {

        junit(
          allowEmptyResults: true,
          testResults: '**/target/surefire-reports/*.xml, **/test-results/**/*.xml, **/dependency-check-report.xml'
        )

        cleanWs()
      }
    }
  }
}

// ============================================
// Helper Functions
// ============================================

def sendSlackNotification(scriptContext, String buildStatus) {

  def colorCode =
    buildStatus == 'SUCCESSFUL' ? 'good' :
    (buildStatus == 'FAILED' ? 'danger' : 'warning')

  def emoji =
    buildStatus == 'SUCCESSFUL' ? '✅' :
    (buildStatus == 'FAILED' ? '❌' : '⚠️')

  def jobName =
    scriptContext.env.JOB_NAME ?: 'Unknown Job'

  def buildNumber =
    scriptContext.env.BUILD_NUMBER ?: 'N/A'

  def buildUrl =
    scriptContext.env.BUILD_URL ?: '#'

  def branchName =
    scriptContext.env.BRANCH_NAME ?: 'Unknown Branch'

  def targetEnv =
    scriptContext.env.TARGET_ENV ?: 'N/A'

  def gitCommitShort =
    scriptContext.env.GIT_COMMIT_SHORT ?: 'N/A'

  def changedServices =
    scriptContext.env.CHANGED_SERVICES ?: 'All'

  // Use try-catch to handle Slack notification failures gracefully
  try {
    slackSend(
      tokenCredentialId: 'slack-token',
      channel: scriptContext.env.SLACK_CHANNEL,
      color: colorCode,
      failOnError: false,
      message:
        "${emoji} *CineVision Build ${buildStatus}*\n" +
        "*Project:* ${jobName}\n" +
        "*Build:* <${buildUrl}|#${buildNumber}>\n" +
        "*Branch:* ${branchName}\n" +
        "*Environment:* ${targetEnv}\n" +
        "*Commit:* ${gitCommitShort}\n" +
        "*Services:* ${changedServices}"
    )
  } catch (Exception e) {
    echo "Warning: Slack notification failed - ${e.message}"
  }
}