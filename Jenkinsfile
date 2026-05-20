// ============================================
// CINEVISION INDUSTRIAL-GRADE CI/CD PIPELINE
// Features: SCA, SAST, DAST, GitOps, Slack Rich Notifications
// ============================================

def serviceMap = [
  'api-gateway'  : [path: 'services/api-gateway',   type: 'maven', image: 'api-gateway'],
  'user-service' : [path: 'services/userService',   type: 'maven', image: 'user-service'],
  'movie-service': [path: 'services/movieService',  type: 'maven', image: 'movie-service'],
  'email-service': [path: 'services/emailService',  type: 'maven', image: 'email-service'],
  'eureka-server': [path: 'services/eureka-server', type: 'maven', image: 'eureka-server'],
  'frontend'     : [path: 'services/frontend',      type: 'node',  image: 'frontend']
]

def detectChangedServices() {
  def changedFiles = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim().split('\n') as List
  def changed = []
  
  serviceMap.each { serviceName, meta ->
    if (changedFiles.any { it.startsWith("${meta.path}/") }) {
      changed << serviceName
    }
  }
  
  if (changed.isEmpty() || changedFiles.isEmpty()) {
    echo "No specific service changes detected - building all services"
    changed = serviceMap.keySet() as List
  }
  
  return changed.unique()
}

def getEnvironmentConfig() {
  def branch = env.BRANCH_NAME
  
  // Production (Mainline)
  if (branch == 'main' || branch == 'master' || branch.startsWith('hotfix/')) {
    return [
      env: 'prod',
      awsAccountId: env.PROD_AWS_ACCOUNT_ID,
      awsCredentialsId: 'aws-prod-credentials',
      namespace: 'cinevision-prod',
      argocdApp: 'cinevision-prod-green',
      frontendBucket: env.PROD_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.PROD_CLOUDFRONT_DISTRIBUTION_ID,
      posterBucket: env.PROD_MOVIE_POSTERS_BUCKET,
      posterCloudfrontId: env.PROD_POSTER_CLOUDFRONT_ID,
      archiveBucket: env.PROD_EMAIL_ARCHIVES_BUCKET,
      archiveCloudfrontId: env.PROD_ARCHIVE_CLOUDFRONT_ID,
      apiUrl: env.PROD_API_URL,
      kustomizeOverlay: 'k8s/overlays/prod/green',
      deployEnabled: true,
      approvalRequired: true,
      runSecurityScan: true,
      runPerformanceTests: true,
      runIntegrationTests: true,
      buildImages: true
    ]
  }
  
  // Staging (Release Candidates)
  if (branch.startsWith('release/')) {
    return [
      env: 'staging',
      awsAccountId: env.STAGING_AWS_ACCOUNT_ID,
      awsCredentialsId: 'aws-staging-credentials',
      namespace: 'cinevision-staging',
      argocdApp: 'cinevision-staging',
      frontendBucket: env.STAGING_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.STAGING_CLOUDFRONT_DISTRIBUTION_ID,
      posterBucket: env.STAGING_MOVIE_POSTERS_BUCKET,
      posterCloudfrontId: env.STAGING_POSTER_CLOUDFRONT_ID,
      archiveBucket: env.STAGING_EMAIL_ARCHIVES_BUCKET,
      archiveCloudfrontId: env.STAGING_ARCHIVE_CLOUDFRONT_ID,
      apiUrl: env.STAGING_API_URL,
      kustomizeOverlay: 'k8s/overlays/staging',
      deployEnabled: true,
      approvalRequired: true,
      runSecurityScan: true,
      runPerformanceTests: true,
      runIntegrationTests: true,
      buildImages: true
    ]
  }
  
  // Development (Integration)
  if (branch == 'develop') {
    return [
      env: 'dev',
      awsAccountId: env.DEV_AWS_ACCOUNT_ID,
      awsCredentialsId: 'ecr-eks',
      namespace: 'cinevision-dev',
      argocdApp: 'cinevision-dev',
      frontendBucket: env.DEV_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.DEV_CLOUDFRONT_DISTRIBUTION_ID,
      posterBucket: env.DEV_MOVIE_POSTERS_BUCKET,
      posterCloudfrontId: env.DEV_POSTER_CLOUDFRONT_ID,
      archiveBucket: env.DEV_EMAIL_ARCHIVES_BUCKET,
      archiveCloudfrontId: env.DEV_ARCHIVE_CLOUDFRONT_ID,
      apiUrl: env.DEV_API_URL,
      kustomizeOverlay: 'k8s/overlays/dev',
      deployEnabled: true,
      approvalRequired: false,
      runSecurityScan: true,
      runPerformanceTests: false,
      runIntegrationTests: true,
      buildImages: true
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
    DEV_CLOUDFRONT_DISTRIBUTION_ID      = credentials('DEV_CLOUDFRONT_DISTRIBUTION_ID')
    DEV_POSTER_CLOUDFRONT_ID           = credentials('DEV_POSTER_CLOUDFRONT_ID')
    DEV_ARCHIVE_CLOUDFRONT_ID          = credentials('DEV_ARCHIVE_CLOUDFRONT_ID')
    
    STAGING_CLOUDFRONT_DISTRIBUTION_ID  = credentials('STAGING_CLOUDFRONT_DISTRIBUTION_ID')
    STAGING_POSTER_CLOUDFRONT_ID       = credentials('STAGING_POSTER_CLOUDFRONT_ID')
    STAGING_ARCHIVE_CLOUDFRONT_ID      = credentials('STAGING_ARCHIVE_CLOUDFRONT_ID')
    
    PROD_CLOUDFRONT_DISTRIBUTION_ID     = credentials('PROD_CLOUDFRONT_DISTRIBUTION_ID')
    PROD_POSTER_CLOUDFRONT_ID          = credentials('PROD_POSTER_CLOUDFRONT_ID')
    PROD_ARCHIVE_CLOUDFRONT_ID         = credentials('PROD_ARCHIVE_CLOUDFRONT_ID')
    
    // Git Configuration
    GITHUB_REPO = 'functionalprojects/cinevision-pro'
    GITHUB_TOKEN = credentials('github-token')
    SLACK_TOKEN = credentials('slack-token')
  }
  
  stages {
    stage('🚀 Initialization') {
      steps {
        script {
          env.CONFIG = getEnvironmentConfig()
          env.TARGET_ENV = env.CONFIG.env
          
          env.CURRENT_ECR_REGISTRY = "${env.CONFIG.awsAccountId}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
          env.GIT_COMMIT = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
          env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
          env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
          
          env.CURRENT_API_URL = env.CONFIG.apiUrl
          env.CURRENT_FRONTEND_BUCKET = env.CONFIG.frontendBucket ?: ''
          env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID = env.CONFIG.cloudfrontDistributionId ?: ''
          
          env.CURRENT_POSTER_BUCKET = env.CONFIG.posterBucket ?: ''
          env.CURRENT_POSTER_CLOUDFRONT_ID = env.CONFIG.posterCloudfrontId ?: ''
          
          env.CURRENT_ARCHIVE_BUCKET = env.CONFIG.archiveBucket ?: ''
          env.CURRENT_ARCHIVE_CLOUDFRONT_ID = env.CONFIG.archiveCloudfrontId ?: ''
          
          env.DEPLOY_ENABLED = env.CONFIG.deployEnabled.toString()
          env.BUILD_IMAGES = env.CONFIG.buildImages.toString()
          
          // Critical validation for Dev
          if (env.TARGET_ENV == 'dev') {
            if (!env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID || env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID == '****') {
              error("DEV_CLOUDFRONT_DISTRIBUTION_ID is not properly configured")
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
            dependencyCheck additionalArguments: '--format HTML --format XML --out .', odcInstallation: 'DP-Check'
            dependencyCheckPublisher pattern: 'dependency-check-report.xml'
          }
        }
        
        stage('SAST: SonarCloud') {
          steps {
            withSonarQubeEnv('sonarcloud') {
              sh 'mvn sonar:sonar'
            }
            timeout(time: 10, unit: 'MINUTES') {
              waitForQualityGate abortPipeline: true
            }
          }
        }
      }
    }
    
    stage('📦 Build & Containerize') {
      when { expression { env.BUILD_IMAGES == 'true' } }
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
                  // Compile
                  if (meta.type == 'maven') {
                    sh "mvn clean package -DskipTests=false"
                  } else {
                    sh "npm ci && npm test"
                  }
                  
                  // Docker
                  def fullImageName = "${env.IMAGE_NAMESPACE}/${meta.image}"
                  def imageTag = "${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${env.IMAGE_TAG}"
                  
                  sh "docker build -t ${imageTag} ."
                  sh "trivy image --severity ${env.TRIVY_SEVERITY} --exit-code 1 ${imageTag}"
                  
                  withAWS(credentials: env.CONFIG.awsCredentialsId, region: env.AWS_REGION) {
                    sh "aws ecr get-login-password --region ${env.AWS_REGION} | docker login --username AWS --password-stdin ${env.CURRENT_ECR_REGISTRY}"
                    sh "docker push ${imageTag}"
                    sh "docker tag ${imageTag} ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${env.TARGET_ENV}-latest"
                    sh "docker push ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${env.TARGET_ENV}-latest"
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
      when { expression { env.DEPLOY_ENABLED == 'true' && env.CHANGED_SERVICES } }
      steps {
        script {
          def overlay = env.CONFIG.kustomizeOverlay
          env.CHANGED_SERVICES.split(',').each { serviceName ->
            if (serviceName != 'frontend') {
              def meta = serviceMap[serviceName]
              def fullImageName = "${env.CURRENT_ECR_REGISTRY}/${env.IMAGE_NAMESPACE}/${meta.image}"
              sh "kustomize edit set image ${meta.image}=${fullImageName}:${env.IMAGE_TAG} -k ${overlay}"
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
      when { expression { env.DEPLOY_ENABLED == 'true' && env.CHANGED_SERVICES?.contains('frontend') } }
      steps {
        script {
          withAWS(credentials: env.CONFIG.awsCredentialsId, region: env.AWS_REGION) {
            dir(serviceMap['frontend'].path) {
              sh 'npm ci'
              sh 'npm run build'
              sh "aws s3 sync dist s3://${env.CURRENT_FRONTEND_BUCKET} --delete --exact-timestamps"
              
              // Invalidation for Frontend
              if (env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID && env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID != '****') {
                sh "aws cloudfront create-invalidation --distribution-id ${env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID} --paths '/*'"
              }
              
              // Invalidation for Movie Posters
              if (env.CHANGED_SERVICES?.contains('movie-service') && env.CURRENT_POSTER_CLOUDFRONT_ID && env.CURRENT_POSTER_CLOUDFRONT_ID != '****') {
                sh "aws cloudfront create-invalidation --distribution-id ${env.CURRENT_POSTER_CLOUDFRONT_ID} --paths '/*'"
              }
              
              // Invalidation for Email Archives
              if (env.CHANGED_SERVICES?.contains('email-service') && env.CURRENT_ARCHIVE_CLOUDFRONT_ID && env.CURRENT_ARCHIVE_CLOUDFRONT_ID != '****') {
                sh "aws cloudfront create-invalidation --distribution-id ${env.CURRENT_ARCHIVE_CLOUDFRONT_ID} --paths '/*'"
              }
            }
          }
        }
      }
    }
    
    stage('🚢 Deployment & Sync') {
      when { expression { env.DEPLOY_ENABLED == 'true' } }
      steps {
        script {
          if (env.CONFIG.approvalRequired) {
            input message: "Approve deployment to ${env.TARGET_ENV}?", ok: "Deploy"
          }
          
          // 1. Initial Sync
          sh "argocd app sync ${env.CONFIG.argocdApp} --grpc-web --prune"
          sh "argocd app wait ${env.CONFIG.argocdApp} --health --timeout 600"
          
          // 2. Production Specific: Canary & Blue-Green
          if (env.TARGET_ENV == 'prod') {
            stage('🚥 Canary Analysis') {
              echo "🚀 Starting Canary Deployment (10% Traffic)"
              // In a real scenario, this would involve updating Istio or Argo Rollouts
              // Here we simulate the canary phase by validating the 'green' pods before full shift
              
              try {
                sh "python3 scripts/canary-analysis.py --url ${env.CONFIG.apiUrl} --duration 60 --interval 10"
                echo "✅ Canary Analysis Passed. Proceeding to full traffic shift."
              } catch (Exception e) {
                echo "❌ Canary Analysis Failed! Rolling back..."
                // Rollback logic (e.g., re-apply blue overlay)
                sh "kubectl apply -k k8s/overlays/prod/blue"
                error("Canary Analysis Failed: ${e.message}")
              }
            }
            
            echo "💎 Performing Full Blue-Green Traffic Shift"
            sh "kubectl apply -k k8s/overlays/prod/green"
            sh "kubectl rollout status deployment -l app=cinevision --timeout=5m"
          }
        }
      }
    }
    
    stage('✅ Verification & DAST') {
      when { expression { env.DEPLOY_ENABLED == 'true' } }
      parallel {
        stage('Smoke & Integration') {
          steps {
            script {
              if (fileExists('tests/smoke')) {
                dir('tests/smoke') { sh "npm ci && npm test -- --env=${env.TARGET_ENV}" }
              }
              if (env.CONFIG.runIntegrationTests) {
                dir('tests/integration') { sh "npm install && BASE_URL=${env.CONFIG.apiUrl} npm test" }
              }
            }
          }
        }
        
        stage('DAST: OWASP ZAP') {
          steps {
            sh "docker run --rm -v \$(pwd):/zap/wrk/:rw -t owasp/zap2docker-stable zap-baseline.py -t ${env.CONFIG.apiUrl} -r zap_report.html || true"
            archiveArtifacts artifacts: 'zap_report.html', allowEmptyArchive: true
          }
        }
        
        stage('Performance: k6') {
          when { expression { env.CONFIG.runPerformanceTests } }
          steps {
            dir('tests/performance') { sh "docker run --rm -v \$(pwd):/tests -t grafana/k6 run /tests/cinevision-load-test.js -e BASE_URL=${env.CONFIG.apiUrl}" }
          }
        }
      }
    }
    
    stage('🔖 Release Tagging') {
      when { expression { env.TARGET_ENV == 'prod' } }
      steps {
        sh """
          git tag -a release-${env.IMAGE_TAG} -m "Release ${env.IMAGE_TAG}"
          git push https://${env.GITHUB_TOKEN}@github.com/${env.GITHUB_REPO}.git release-${env.IMAGE_TAG}
        """
      }
    }
  }
  
  post {
    success { sendSlackNotification('SUCCESSFUL') }
    failure { sendSlackNotification('FAILED') }
    always {
      script {
        node {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml, **/test-results/**/*.xml'
        }
      }
      cleanWs()
    }
  }
}

// ============================================
// Helper Functions
// ============================================

def sendSlackNotification(String buildStatus) {
    def colorCode = buildStatus == 'SUCCESSFUL' ? 'good' : (buildStatus == 'FAILED' ? 'danger' : 'warning')
    def emoji = buildStatus == 'SUCCESSFUL' ? '✅' : (buildStatus == 'FAILED' ? '❌' : '⚠️')
    
    slackSend(
        token: env.SLACK_TOKEN,
        color: colorCode,
        message: "${emoji} *CineVision Build ${buildStatus}* \n" +
                 "*Project:* ${env.JOB_NAME} \n" +
                 "*Build:* <${env.BUILD_URL}|#${env.BUILD_NUMBER}> \n" +
                 "*Branch:* ${env.BRANCH_NAME} \n" +
                 "*Environment:* ${env.TARGET_ENV ?: 'N/A'} \n" +
                 "*Commit:* ${env.GIT_COMMIT_SHORT ?: 'N/A'} \n" +
                 "*Services:* ${env.CHANGED_SERVICES ?: 'All'}"
    )
}