// ============================================
// CINEVISION INDUSTRIAL-GRADE CI/CD PIPELINE
// Features: SCA, SAST, DAST, GitOps, Slack Rich Notifications
// ============================================

// Global configuration - accessible throughout pipeline
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

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '30', daysToKeepStr: '30'))
    timeout(time: 90, unit: 'MINUTES')
    ansiColor('xterm')
    skipDefaultCheckout()
  }

  environment {
    // AWS Configuration
    AWS_REGION = 'us-east-1'
    DR_AWS_REGION = 'us-west-2'
    
    // ECR Configuration
    ECR_REPOSITORY_PREFIX = 'cinevision'
    
    // Security Configuration
    TRIVY_SEVERITY = 'HIGH,CRITICAL'
    SONAR_HOST_URL = 'https://sonarcloud.io'
    SONAR_ORGANIZATION = 'functionalprojects'
    
    // GitHub Configuration
    GITHUB_REPO = 'functionalprojects/cinevision-pro'
    
    // Slack Configuration
    SLACK_CHANNEL = '#cinevision-ci-alerts'
    
    // Account IDs - Will be set via credentials
    DEV_AWS_ACCOUNT_ID = credentials('DEV_AWS_ACCOUNT_ID')
    STAGING_AWS_ACCOUNT_ID = credentials('STAGING_AWS_ACCOUNT_ID')
    PROD_AWS_ACCOUNT_ID = credentials('PROD_AWS_ACCOUNT_ID')
    
    // CloudFront Distribution IDs
    DEV_CLOUDFRONT_DISTRIBUTION_ID = credentials('DEV_CLOUDFRONT_DISTRIBUTION_ID')
    DEV_POSTER_CLOUDFRONT_ID = credentials('DEV_POSTER_CLOUDFRONT_ID')
    DEV_ARCHIVE_CLOUDFRONT_ID = credentials('DEV_ARCHIVE_CLOUDFRONT_ID')
    
    STAGING_CLOUDFRONT_DISTRIBUTION_ID = credentials('STAGING_CLOUDFRONT_DISTRIBUTION_ID')
    STAGING_POSTER_CLOUDFRONT_ID = credentials('STAGING_POSTER_CLOUDFRONT_ID')
    STAGING_ARCHIVE_CLOUDFRONT_ID = credentials('STAGING_ARCHIVE_CLOUDFRONT_ID')
    
    PROD_CLOUDFRONT_DISTRIBUTION_ID = credentials('PROD_CLOUDFRONT_DISTRIBUTION_ID')
    PROD_POSTER_CLOUDFRONT_ID = credentials('PROD_POSTER_CLOUDFRONT_ID')
    PROD_ARCHIVE_CLOUDFRONT_ID = credentials('PROD_ARCHIVE_CLOUDFRONT_ID')
    
    // Git and Slack Tokens
    GITHUB_TOKEN = credentials('github-token')
    SLACK_TOKEN = credentials('slack-token')
    
    // API URLs
    DEV_API_URL = 'https://dev-api.cinevisionca.link'
    STAGING_API_URL = 'https://staging-api.cinevision.com'
    PROD_API_URL = 'https://api.cinevision.com'
    
    // S3 Buckets
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

  stages {
    stage('🚀 Initialization & Checkout') {
      steps {
        checkout scm
        
        script {
          // Load environment configuration
          CURRENT_ENV_CONFIG = getEnvironmentConfig()
          
          // Set environment variables from config
          env.TARGET_ENV = CURRENT_ENV_CONFIG.env
          env.AWS_CREDENTIALS_ID = CURRENT_ENV_CONFIG.awsCredentialsId
          env.KUSTOMIZE_OVERLAY = CURRENT_ENV_CONFIG.kustomizeOverlay
          env.ARGOCD_APP = CURRENT_ENV_CONFIG.argocdApp
          env.API_URL = CURRENT_ENV_CONFIG.apiUrl
          env.DEPLOY_ENABLED = CURRENT_ENV_CONFIG.deployEnabled.toString()
          env.BUILD_IMAGES = CURRENT_ENV_CONFIG.buildImages.toString()
          env.APPROVAL_REQUIRED = CURRENT_ENV_CONFIG.approvalRequired.toString()
          env.RUN_INTEGRATION_TESTS = CURRENT_ENV_CONFIG.runIntegrationTests.toString()
          env.RUN_PERFORMANCE_TESTS = CURRENT_ENV_CONFIG.runPerformanceTests.toString()
          
          // Set resource-specific environment variables
          env.CURRENT_FRONTEND_BUCKET = CURRENT_ENV_CONFIG.frontendBucket ?: ''
          env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID = CURRENT_ENV_CONFIG.cloudfrontDistributionId ?: ''
          env.CURRENT_POSTER_BUCKET = CURRENT_ENV_CONFIG.posterBucket ?: ''
          env.CURRENT_POSTER_CLOUDFRONT_ID = CURRENT_ENV_CONFIG.posterCloudfrontId ?: ''
          env.CURRENT_ARCHIVE_BUCKET = CURRENT_ENV_CONFIG.archiveBucket ?: ''
          env.CURRENT_ARCHIVE_CLOUDFRONT_ID = CURRENT_ENV_CONFIG.archiveCloudfrontId ?: ''
          
          // Set ECR registry
          if (CURRENT_ENV_CONFIG.awsAccountId) {
            env.CURRENT_ECR_REGISTRY = "${CURRENT_ENV_CONFIG.awsAccountId}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
          }
          
          // Git metadata
          env.GIT_COMMIT = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
          env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
          env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
          env.BUILD_TIMESTAMP = sh(script: 'date -u +"%Y-%m-%dT%H:%M:%SZ"', returnStdout: true).trim()
          
          // Detect changed services
          def changedServices = detectChangedServices()
          env.CHANGED_SERVICES = changedServices.join(',')
          
          // Validation
          if (['dev', 'staging', 'prod'].contains(env.TARGET_ENV)) {
            validateEnvironmentConfiguration()
          }
          
          echo "========================================="
          echo "🚀 Pipeline Configuration"
          echo "========================================="
          echo "Environment: ${env.TARGET_ENV}"
          echo "Branch: ${env.BRANCH_NAME}"
          echo "Build: ${env.BUILD_NUMBER}"
          echo "Commit: ${env.GIT_COMMIT_SHORT}"
          echo "Tag: ${env.IMAGE_TAG}"
          echo "Services: ${env.CHANGED_SERVICES}"
          echo "Deploy Enabled: ${env.DEPLOY_ENABLED}"
          echo "========================================="
        }
      }
    }

    stage('🔍 Security & Code Quality') {
      parallel {
        stage('SCA: Dependency Check') {
          when {
            expression { env.CHANGED_SERVICES != '' }
          }
          steps {
            script {
              def servicesToScan = env.CHANGED_SERVICES.split(',')
              def mavenServices = servicesToScan.findAll { serviceMap[it]?.type == 'maven' }
              
              if (mavenServices.isEmpty()) {
                echo 'No Maven services to scan for dependencies'
                return
              }
              
              def scaStages = [:]
              mavenServices.each { serviceName ->
                def meta = serviceMap[serviceName]
                scaStages[serviceName] = {
                  stage("Dependency Check: ${serviceName}") {
                    dir(meta.path) {
                      // Run dependency check
                      sh '''
                        mvn org.owasp:dependency-check-maven:check \
                          -Dformat=XML \
                          -Dformat=HTML \
                          -DoutputDirectory=. \
                          -DautoUpdate=true \
                          -DfailOnError=false || true
                      '''
                      
                      // Publish results if they exist
                      script {
                        if (fileExists('dependency-check-report.xml')) {
                          dependencyCheckPublisher(
                            pattern: 'dependency-check-report.xml',
                            allowEmptyResults: true
                          )
                        } else {
                          echo "No dependency-check-report.xml found for ${serviceName}"
                        }
                      }
                    }
                  }
                }
              }
              
              parallel scaStages
            }
          }
        }

        stage('SAST: SonarCloud') {
          when {
            expression { env.CHANGED_SERVICES != '' && env.BRANCH_NAME != 'main' }
          }
          steps {
            script {
              def servicesToScan = env.CHANGED_SERVICES.split(',')
              def mavenServices = servicesToScan.findAll { serviceMap[it]?.type == 'maven' }
              
              if (mavenServices.isEmpty()) {
                echo 'No Maven services to analyze with SonarCloud'
                return
              }
              
              def sonarStages = [:]
              mavenServices.each { serviceName ->
                def meta = serviceMap[serviceName]
                sonarStages[serviceName] = {
                  stage("Sonar Analysis: ${serviceName}") {
                    dir(meta.path) {
                      withSonarQubeEnv('sonarcloud') {
                        sh """
                          mvn sonar:sonar \
                            -Dsonar.projectKey=${meta.sonarProject} \
                            -Dsonar.organization=${env.SONAR_ORGANIZATION} \
                            -Dsonar.host.url=${env.SONAR_HOST_URL} \
                            -Dsonar.branch.name=${env.BRANCH_NAME} \
                            -Dsonar.projectVersion=${env.IMAGE_TAG} \
                            -Dsonar.sourceEncoding=UTF-8
                        """
                      }
                    }
                  }
                }
              }
              
              parallel sonarStages
              
              // Wait for quality gates (only for main/develop branches)
              if (env.BRANCH_NAME == 'develop' || env.BRANCH_NAME == 'main') {
                timeout(time: 10, unit: 'MINUTES') {
                  waitForQualityGate abortPipeline: false
                }
              }
            }
          }
        }
      }
    }

    stage('📦 Build & Containerize') {
      when {
        expression { env.BUILD_IMAGES == 'true' && env.CHANGED_SERVICES != '' }
      }
      steps {
        script {
          def servicesToBuild = env.CHANGED_SERVICES.split(',')
          def buildStages = [:]
          
          servicesToBuild.each { serviceName ->
            def meta = serviceMap[serviceName]
            if (!meta) {
              echo "Warning: Unknown service ${serviceName}, skipping"
              return
            }
            
            buildStages[serviceName] = {
              stage("Build: ${serviceName}") {
                dir(meta.path) {
                  // Build application
                  if (meta.type == 'maven') {
                    sh 'mvn clean compile test package -DskipTests=false'
                  } else if (meta.type == 'node') {
                    sh '''
                      npm ci || npm install
                      npm run build --if-present
                      npm test --if-present
                    '''
                  }
                  
                  // Login to ECR
                  withAWS(credentials: env.AWS_CREDENTIALS_ID, region: env.AWS_REGION) {
                    sh """
                      aws ecr get-login-password --region ${env.AWS_REGION} | \
                      docker login --username AWS --password-stdin ${env.CURRENT_ECR_REGISTRY}
                    """
                  }
                  
                  // Build Docker image
                  def fullImageName = "${env.ECR_REPOSITORY_PREFIX}/${meta.image}"
                  def imageTag = "${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${env.IMAGE_TAG}"
                  def latestTag = "${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${env.TARGET_ENV}-latest"
                  
                  sh "docker build -t ${imageTag} -t ${latestTag} ."
                  
                  // Security scan with Trivy
                  sh """
                    trivy image \
                      --severity ${env.TRIVY_SEVERITY} \
                      --exit-code 0 \
                      --format table \
                      ${imageTag}
                  """
                  
                  // Push to ECR
                  sh "docker push ${imageTag}"
                  sh "docker push ${latestTag}"
                  
                  echo "✅ Successfully built and pushed: ${imageTag}"
                }
              }
            }
          }
          
          if (buildStages.isEmpty()) {
            echo 'No services to build'
          } else {
            parallel buildStages
          }
        }
      }
    }

    stage('📂 GitOps Manifest Update') {
      when {
        expression {
          env.DEPLOY_ENABLED == 'true' &&
          env.CHANGED_SERVICES != '' &&
          env.KUSTOMIZE_OVERLAY != ''
        }
      }
      steps {
        script {
          def servicesToUpdate = env.CHANGED_SERVICES.split(',')
          def updated = false
          
          servicesToUpdate.each { serviceName ->
            if (serviceName != 'frontend' && serviceMap.containsKey(serviceName)) {
              def meta = serviceMap[serviceName]
              def fullImageName = "${env.CURRENT_ECR_REGISTRY}/${env.ECR_REPOSITORY_PREFIX}/${meta.image}"
              def overlayDir = env.KUSTOMIZE_OVERLAY
              
              // Check if kustomization.yaml exists
              if (fileExists("${overlayDir}/kustomization.yaml")) {
                dir(overlayDir) {
                  sh """
                    kustomize edit set image \
                      ${meta.image}=${fullImageName}:${env.IMAGE_TAG}
                  """
                  updated = true
                }
              } else {
                echo "Warning: kustomization.yaml not found in ${overlayDir}"
              }
            }
          }
          
          if (updated) {
            withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
              sh """
                git config user.email "jenkins@cinevision.com"
                git config user.name "Jenkins CI"
                git add ${env.KUSTOMIZE_OVERLAY}
                git diff --cached --quiet || git commit -m "[CI] Update images to ${env.IMAGE_TAG} for ${env.TARGET_ENV}"
                git push https://${GITHUB_TOKEN}@github.com/${env.GITHUB_REPO}.git HEAD:${env.BRANCH_NAME}
              """
            }
          } else {
            echo 'No manifest updates needed'
          }
        }
      }
    }

    stage('🌐 Frontend Deployment') {
      when {
        expression {
          env.DEPLOY_ENABLED == 'true' &&
          env.CHANGED_SERVICES?.contains('frontend') &&
          env.CURRENT_FRONTEND_BUCKET != ''
        }
      }
      steps {
        script {
          def meta = serviceMap['frontend']
          
          withAWS(credentials: env.AWS_CREDENTIALS_ID, region: env.AWS_REGION) {
            dir(meta.path) {
              // Build frontend
              sh '''
                npm ci || npm install
                npm run build
              '''
              
              // Sync to S3
              sh """
                aws s3 sync dist/ \
                  s3://${env.CURRENT_FRONTEND_BUCKET}/ \
                  --delete \
                  --cache-control "max-age=31536000, immutable"
              """
              
              // Invalidate CloudFront
              if (env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID && 
                  env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID != '****') {
                sh """
                  aws cloudfront create-invalidation \
                    --distribution-id ${env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID} \
                    --paths "/*"
                """
              }
            }
          }
          
          echo "✅ Frontend deployed to ${env.CURRENT_FRONTEND_BUCKET}"
        }
      }
    }

    stage('🚢 Deployment & Sync') {
      when {
        expression { env.DEPLOY_ENABLED == 'true' && env.ARGOCD_APP != '' }
      }
      steps {
        script {
          // Approval gate for production
          if (env.APPROVAL_REQUIRED == 'true') {
            input(
              message: "⚠️ Approve deployment to ${env.TARGET_ENV}?",
              submitter: 'admin,release-manager',
              parameters: [
                string(
                  defaultValue: env.IMAGE_TAG,
                  description: 'Image tag to deploy',
                  name: 'DEPLOY_TAG'
                )
              ]
            )
          }
          
          // Sync with ArgoCD
          withCredentials([string(credentialsId: 'argocd-token', variable: 'ARGOCD_TOKEN')]) {
            sh """
              argocd app sync ${env.ARGOCD_APP} \
                --grpc-web \
                --prune \
                --force
              
              argocd app wait ${env.ARGOCD_APP} \
                --health \
                --sync \
                --timeout 600 \
                --grpc-web
            """
          }
          
          echo "✅ Deployment sync completed for ${env.ARGOCD_APP}"
        }
      }
    }

    stage('✅ Verification & Testing') {
      when {
        expression { env.DEPLOY_ENABLED == 'true' }
      }
      parallel {
        stage('Smoke Tests') {
          steps {
            script {
              if (fileExists('tests/smoke')) {
                dir('tests/smoke') {
                  sh """
                    npm ci || npm install
                    npm test -- --env=${env.TARGET_ENV} --url=${env.API_URL}
                  """
                }
              } else {
                echo 'No smoke tests found'
              }
            }
          }
        }
        
        stage('Integration Tests') {
          when {
            expression { env.RUN_INTEGRATION_TESTS == 'true' }
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
        
        stage('DAST: OWASP ZAP') {
          steps {
            script {
              sh """
                docker run --rm \
                  -v \$(pwd):/zap/wrk/:rw \
                  -t owasp/zap2docker-stable \
                  zap-baseline.py \
                  -t ${env.API_URL} \
                  -r zap_report.html \
                  -z "-config replacer.full_list\\(0\\).description=auth \
                    -config replacer.full_list\\(0\\).enabled=true \
                    -config replacer.full_list\\(0\\).matchtype=REQ_HEADER \
                    -config replacer.full_list\\(0\\).matchstr=Authorization \
                    -config replacer.full_list\\(0\\).regex=false" || true
              """
              
              archiveArtifacts(
                artifacts: 'zap_report.html',
                allowEmptyArchive: true
              )
            }
          }
        }
        
        stage('Performance: k6') {
          when {
            expression { env.RUN_PERFORMANCE_TESTS == 'true' }
          }
          steps {
            script {
              if (fileExists('tests/performance')) {
                dir('tests/performance') {
                  sh """
                    docker run --rm \
                      -v \$(pwd):/tests \
                      -e BASE_URL=${env.API_URL} \
                      grafana/k6:latest \
                      run /tests/load-test.js
                  """
                }
              } else {
                echo 'No performance tests found'
              }
            }
          }
        }
      }
    }

    stage('🔖 Release Tagging') {
      when {
        expression { env.TARGET_ENV == 'prod' && env.BRANCH_NAME == 'main' }
      }
      steps {
        script {
          withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
            sh """
              git tag -a "release-${env.IMAGE_TAG}" \
                -m "Release ${env.IMAGE_TAG}\\nEnvironment: ${env.TARGET_ENV}\\nBuild: ${env.BUILD_NUMBER}\\nCommit: ${env.GIT_COMMIT}"
              git push https://${GITHUB_TOKEN}@github.com/${env.GITHUB_REPO}.git --tags
            """
          }
          
          echo "✅ Created release tag: release-${env.IMAGE_TAG}"
        }
      }
    }
  }

  post {
    success {
      script {
        def duration = currentBuild.durationString
        echo "✅ Pipeline completed successfully in ${duration}"
        sendSlackNotification('SUCCESSFUL')
      }
    }
    
    failure {
      script {
        def duration = currentBuild.durationString
        echo "❌ Pipeline failed after ${duration}"
        sendSlackNotification('FAILED')
      }
    }
    
    unstable {
      script {
        echo "⚠️ Pipeline completed with unstable results"
        sendSlackNotification('UNSTABLE')
      }
    }
    
    aborted {
      script {
        echo "🛑 Pipeline was aborted"
        sendSlackNotification('ABORTED')
      }
    }
    
    always {
      script {
        // Archive test reports
        junit(
          allowEmptyResults: true,
          testResults: '**/target/surefire-reports/*.xml, **/test-results/**/*.xml, **/dependency-check-report.xml, **/TEST-*.xml'
        )
        
        // Archive build artifacts (keep last 10)
        archiveArtifacts(
          artifacts: '**/target/*.jar, **/dist/**/*, **/build/**/*',
          allowEmptyArchive: true,
          fingerprint: false
        )
        
        // Clean workspace
        cleanWs(
          cleanWhenAborted: true,
          cleanWhenFailure: true,
          cleanWhenNotBuilt: true,
          cleanWhenSuccess: true,
          cleanWhenUnstable: true
        )
      }
    }
  }
}

// ============================================
// Helper Functions
// ============================================

def getEnvironmentConfig() {
  def branch = env.BRANCH_NAME ?: 'unknown'
  
  // Production (main/master or hotfix)
  if (branch == 'main' || branch == 'master' || branch.startsWith('hotfix/')) {
    return [
      env                     : 'prod',
      awsAccountId            : env.PROD_AWS_ACCOUNT_ID,
      awsCredentialsId        : 'aws-prod-credentials',
      namespace               : 'cinevision-prod',
      argocdApp               : 'cinevision-prod',
      frontendBucket          : env.PROD_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.PROD_CLOUDFRONT_DISTRIBUTION_ID,
      posterBucket            : env.PROD_MOVIE_POSTERS_BUCKET,
      posterCloudfrontId      : env.PROD_POSTER_CLOUDFRONT_ID,
      archiveBucket           : env.PROD_EMAIL_ARCHIVES_BUCKET,
      archiveCloudfrontId     : env.PROD_ARCHIVE_CLOUDFRONT_ID,
      apiUrl                  : env.PROD_API_URL,
      kustomizeOverlay        : 'k8s/overlays/prod',
      deployEnabled           : true,
      approvalRequired        : true,
      runSecurityScan         : true,
      runPerformanceTests     : true,
      runIntegrationTests     : true,
      buildImages             : true
    ]
  }
  
  // Staging (release branches)
  if (branch.startsWith('release/')) {
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
  
  // Development (develop branch)
  if (branch == 'develop') {
    return [
      env                     : 'dev',
      awsAccountId            : env.DEV_AWS_ACCOUNT_ID,
      awsCredentialsId        : 'aws-dev-credentials',
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
  
  // Feature branches
  return [
    env                     : 'feature',
    awsAccountId            : null,
    awsCredentialsId        : null,
    namespace               : null,
    argocdApp               : null,
    frontendBucket          : null,
    cloudfrontDistributionId: null,
    posterBucket            : null,
    posterCloudfrontId      : null,
    archiveBucket           : null,
    archiveCloudfrontId     : null,
    apiUrl                  : null,
    kustomizeOverlay        : null,
    deployEnabled           : false,
    approvalRequired        : false,
    runSecurityScan         : true,
    runPerformanceTests     : false,
    runIntegrationTests     : false,
    buildImages             : true
  ]
}

def detectChangedServices() {
  // Get changes between current commit and previous one
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
  
  def changedFiles = changedFilesRaw ? changedFilesRaw.split('\n') as List : []
  
  // Find which services have changes
  def changed = []
  serviceMap.each { serviceName, meta ->
    if (changedFiles.any { it.startsWith("${meta.path}/") }) {
      changed << serviceName
      echo "Changes detected in: ${serviceName} (${meta.path})"
    }
  }
  
  // If no specific changes detected, build all for full pipelines
  if (changed.isEmpty()) {
    echo "No specific service changes detected in last commit"
    
    // For feature branches, build only changed or none
    if (env.BRANCH_NAME == 'develop' || env.BRANCH_NAME == 'main' || env.BRANCH_NAME?.startsWith('release/')) {
      echo "Building all services for full pipeline"
      changed = serviceMap.keySet() as List
    } else {
      echo "Feature branch with no changes - skipping build"
    }
  }
  
  return changed.unique()
}

def validateEnvironmentConfiguration() {
  def requiredVars = [
    'CURRENT_CLOUDFRONT_DISTRIBUTION_ID',
    'CURRENT_FRONTEND_BUCKET',
    'API_URL'
  ]
  
  def missingVars = []
  requiredVars.each { varName ->
    if (!env[varName] || env[varName] == '****') {
      missingVars << varName
    }
  }
  
  if (missingVars) {
    error("Missing required configuration for ${env.TARGET_ENV}: ${missingVars.join(', ')}")
  }
  
  echo "✅ Environment configuration validated for ${env.TARGET_ENV}"
}

def sendSlackNotification(String buildStatus) {
  def colorMap = [
    'SUCCESSFUL': 'good',
    'FAILED': 'danger',
    'UNSTABLE': 'warning',
    'ABORTED': '#808080'
  ]
  
  def emojiMap = [
    'SUCCESSFUL': '✅',
    'FAILED': '❌',
    'UNSTABLE': '⚠️',
    'ABORTED': '🛑'
  ]
  
  def color = colorMap[buildStatus] ?: 'warning'
  def emoji = emojiMap[buildStatus] ?: 'ℹ️'
  
  def buildUrl = env.BUILD_URL ?: '#'
  def jobName = env.JOB_NAME ?: 'Unknown'
  def buildNumber = env.BUILD_NUMBER ?: 'N/A'
  def branchName = env.BRANCH_NAME ?: 'Unknown'
  def targetEnv = env.TARGET_ENV ?: 'N/A'
  def gitCommit = env.GIT_COMMIT_SHORT ?: 'N/A'
  def changedServices = env.CHANGED_SERVICES ?: 'None'
  def duration = currentBuild.durationString
  
  def message = """
${emoji} *CineVision Build ${buildStatus}*
• *Project:* ${jobName}
• *Build:* <${buildUrl}|#${buildNumber}>
• *Branch:* ${branchName}
• *Environment:* ${targetEnv}
• *Commit:* ${gitCommit}
• *Services:* ${changedServices}
• *Duration:* ${duration}
• *Triggered by:* ${env.CHANGE_AUTHOR_DISPLAY_NAME ?: 'Automated'}
""".stripIndent()
  
  try {
    slackSend(
      tokenCredentialId: 'slack-token',
      channel: env.SLACK_CHANNEL,
      color: color,
      message: message,
      failOnError: false
    )
    echo "✅ Slack notification sent (${buildStatus})"
  } catch (Exception e) {
    echo "⚠️ Slack notification failed: ${e.message}"
  }
}